package com.graduate.graidaicommunity.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.graduate.graidaicommunity.common.Result;
import com.graduate.graidaicommunity.common.UserContext;
import com.graduate.graidaicommunity.mapper.FavoriteMapper;
import com.graduate.graidaicommunity.pojo.Favorite;
import com.graduate.graidaicommunity.pojo.Post;
import com.graduate.graidaicommunity.pojo.SysUser;
import com.graduate.graidaicommunity.service.FavoriteService;
import com.graduate.graidaicommunity.service.PostService;
import com.graduate.graidaicommunity.service.UserService;
import com.graduate.graidaicommunity.util.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class FavoriteServiceImpl extends ServiceImpl<FavoriteMapper, Favorite> implements FavoriteService {

    @Resource
    private PostService postService;
    @Resource
    private UserService userService;
    @Resource
    private RedisUtil redisUtil;

    private static final String FAVORITE_KEY_PREFIX = "post:favorite:";

    @Override
    public Result<?> toggleFavorite(Long postId) {
        Post post = postService.getById(postId);
        if (post == null) {
            return Result.fail(400, "帖子不存在");
        }

        Long userId = UserContext.getUserId();
        String favoriteKey = FAVORITE_KEY_PREFIX + postId;

        Long result = redisUtil.toggleSetAtomic(favoriteKey, userId.toString());//返回1，加入收藏；返回0，取消；null=Redis 故障

        if (result == null) {
            log.warn("Redis收藏失败，降级MySQL, userId={}, postId={}", userId, postId);
            return toggleFavoriteByMySQL(postId, userId);
        }

        try {
            if (result == 1) {
                LambdaQueryWrapper<Favorite> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(Favorite::getUserId, userId).eq(Favorite::getPostId, postId);
                if (this.count(wrapper) == 0) {
                    Favorite newFav = new Favorite();
                    newFav.setUserId(userId);
                    newFav.setPostId(postId);
                    this.save(newFav);
                }
            } else {
                LambdaQueryWrapper<Favorite> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(Favorite::getUserId, userId).eq(Favorite::getPostId, postId);
                this.remove(wrapper);
            }
        } catch (Exception e) {
            log.error("收藏同步MySQL失败, userId={}, postId={}, result={}", userId, postId, result, e);
        }

        if (result == 1) {
            log.info("【收藏】成功，用户 {} 帖子 {}", userId, postId);
            return Result.success("收藏成功");
        } else {
            log.info("【取消收藏】成功，用户 {} 帖子 {}", userId, postId);
            return Result.success("取消收藏");
        }
    }

    @Override
    public Result<?> getMyFavorites(Integer pageNum, Integer pageSize) {
        Long userId = UserContext.getUserId();
        Page<Favorite> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Favorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Favorite::getUserId, userId);
        wrapper.orderByDesc(Favorite::getCreateTime);
        Page<Favorite> favPage = this.page(page, wrapper);

        List<Long> postIds = favPage.getRecords().stream()
                .map(Favorite::getPostId)
                .collect(Collectors.toList());

        Page<Post> resultPage = new Page<>(pageNum, pageSize, favPage.getTotal());
        if (!postIds.isEmpty()) {
            List<Post> posts = postService.listByIds(postIds);
            fillPostExtraInfo(posts, userId);
            resultPage.setRecords(posts);
        }
        return Result.success(resultPage);
    }

    private void fillPostExtraInfo(List<Post> posts, Long currentUserId) {
        if (posts == null || posts.isEmpty()) return;

        List<Long> userIds = posts.stream().map(Post::getUserId).distinct().collect(Collectors.toList());
        List<SysUser> users = userService.listByIds(userIds);
        Map<Long, SysUser> userMap = users.stream().collect(Collectors.toMap(SysUser::getId, u -> u));

        for (Post post : posts) {
            SysUser user = userMap.get(post.getUserId());
            if (user != null) {
                post.setNickname(user.getNickname());
                post.setAvatar(user.getAvatar());
            }

            String likeKey = "post:like:" + post.getId();
            try {
                Long likeCount = redisUtil.sCard(likeKey);
                if (likeCount != null) {
                    post.setLikeCount(likeCount.intValue());
                }
            } catch (Exception e) {
                // 保持数据库原值
            }

            String commentKey = "post:comment:count:" + post.getId();
            try {
                String countStr = redisUtil.get(commentKey);
                if (countStr != null) {
                    post.setCommentCount(Integer.valueOf(countStr));
                }
            } catch (Exception e) {
                // 保持数据库原值
            }

            if (currentUserId != null) {
                try {
                    Boolean hasLiked = redisUtil.sIsMember(likeKey, currentUserId.toString());
                    post.setHasLiked(hasLiked != null && hasLiked);
                } catch (Exception e) {
                    post.setHasLiked(false);
                }
            }

            post.setIsFavorited(true);
        }
    }

    private Result<?> toggleFavoriteByMySQL(Long postId, Long userId) {
        LambdaQueryWrapper<Favorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Favorite::getUserId, userId);
        wrapper.eq(Favorite::getPostId, postId);
        Favorite fav = this.getOne(wrapper);

        if (fav != null) {
            this.removeById(fav.getId());
            log.info("【MySQL降级】取消收藏，用户 {} 帖子 {}", userId, postId);
            return Result.success("取消收藏");
        } else {
            try {
                Favorite newFav = new Favorite();
                newFav.setUserId(userId);
                newFav.setPostId(postId);
                this.save(newFav);
                log.info("【MySQL降级】收藏成功，用户 {} 帖子 {}", userId, postId);
                return Result.success("收藏成功");
            } catch (DuplicateKeyException e) {
                return Result.success("收藏成功");
            }
        }
    }
}