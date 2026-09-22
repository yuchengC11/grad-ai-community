package com.graduate.graidaicommunity.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.graduate.graidaicommunity.common.Result;
import com.graduate.graidaicommunity.common.UserContext;
import com.graduate.graidaicommunity.mapper.FavoriteMapper;
import com.graduate.graidaicommunity.mapper.PostLikeMapper;
import com.graduate.graidaicommunity.mapper.PostMapper;
import com.graduate.graidaicommunity.pojo.Favorite;
import com.graduate.graidaicommunity.pojo.Post;
import com.graduate.graidaicommunity.pojo.PostLike;
import com.graduate.graidaicommunity.pojo.SysUser;
import com.graduate.graidaicommunity.service.PostService;
import com.graduate.graidaicommunity.service.UserService;
import com.graduate.graidaicommunity.util.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PostServiceImpl extends ServiceImpl<PostMapper, Post> implements PostService {

    @Resource
    private PostMapper postMapper;
    @Resource
    private UserService userService;
    @Resource
    private PostLikeMapper postLikeMapper;
    @Resource
    private FavoriteMapper favoriteMapper;
    @Resource
    private RedisUtil redisUtil;

    private static final String POST_LIKE_KEY = "post:like:";
    private static final String POST_LIKE_SYNC_KEY = "post:like:sync";
    private static final String COMMENT_COUNT_KEY_PREFIX = "post:comment:count:";

    // ========== 公共方法 ==========

    @Override
    public Result<?> getPostList(Integer pageNum, Integer pageSize, Long parentCategoryId, Long categoryId) {
        Page<Post> page = new Page<>(pageNum, pageSize);
        Long userId = UserContext.getUserId();

        IPage<Post> result;
        if (categoryId != null) {
            result = postMapper.selectListByCategoryWithFavorite(page, categoryId.intValue(), userId);
        } else {
            result = postMapper.selectListWithFavorite(page, userId);
        }

        fillUserInfo(result);
        fillLikeCountFromRedis(result.getRecords());
        fillCommentCountFromRedis(result.getRecords());
        fillHasLiked(result.getRecords());
        // ✅ 不再需要 fillHasFavorited，SQL 已经联表查出来了

        return Result.success(result);
    }
    @Override
    public Result<?> getPostDetail(Long id) {
        Post post = getById(id);
        if (post == null) {
            return Result.fail(404, "帖子不存在");
        }
        SysUser user = userService.getById(post.getUserId());
        if (user != null) {
            post.setNickname(user.getNickname());
            post.setAvatar(user.getAvatar());
        }
        Long likeCount = getLikeCountFromRedis(id);
        if (likeCount != null) {
            post.setLikeCount(likeCount.intValue());
        }
        Long commentCount = getCommentCountFromRedis(id);
        if (commentCount != null) {
            post.setCommentCount(commentCount.intValue());
        }
        Long currentUserId = UserContext.getUserId();

        // 点赞状态
        String key = POST_LIKE_KEY + id;
        try {
            Boolean hasLiked = redisUtil.sIsMember(key, currentUserId.toString());
            post.setHasLiked(hasLiked != null && hasLiked);
        } catch (Exception e) {
            LambdaQueryWrapper<PostLike> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(PostLike::getPostId, id);
            wrapper.eq(PostLike::getUserId, currentUserId);
            post.setHasLiked(postLikeMapper.selectOne(wrapper) != null);
        }

        // ✅ 新增：收藏状态
        try {
            post.setIsFavorited(favoriteMapper.checkFavorite(currentUserId, id) > 0);
        } catch (Exception e) {
            log.error("查询收藏状态失败: {}", e.getMessage());
            post.setIsFavorited(false);
        }

        return Result.success(post);
    }

    @Override
    public Result<?> addPost(Post post) {
        Long userId = UserContext.getUserId();
        post.setUserId(userId);
        if (post.getLikeCount() == null) post.setLikeCount(0);
        if (post.getCommentCount() == null) post.setCommentCount(0);
        save(post);
        return Result.success("发布成功");
    }

    @Override
    public Result<?> toggleLike(Long postId) {
        Post post = getById(postId);
        if (post == null) {
            return Result.fail(400, "帖子不存在");
        }

        Long userId = UserContext.getUserId();
        String postLikeKey = POST_LIKE_KEY + postId;

        try {
            Long result = redisUtil.toggleSetAtomic(postLikeKey, userId.toString());
            if (result != null) {
                redisUtil.sAdd(POST_LIKE_SYNC_KEY, postId.toString());
                // 同时更新内存计数，避免立即查Redis
                if (result == 1) {
                    log.info("【Redis】点赞成功，用户 {} 帖子 {}", userId, postId);
                } else {
                    log.info("【Redis】取消点赞，用户 {} 帖子 {}", userId, postId);
                }
                return Result.success(result == 1 ? "点赞成功" : "取消点赞");
            }
            throw new RuntimeException("Lua执行失败");
        } catch (Exception e) {
            log.warn("Redis Lua失败，直接降级MySQL: {}", e.getMessage());
            return toggleLikeByMySQL(postId, userId);
        }
    }

    @Override
    public Result<?> getLikeStatus(Long postId) {
        Long userId = UserContext.getUserId();
        String key = POST_LIKE_KEY + postId;
        try {
            Boolean hasLiked = redisUtil.sIsMember(key, userId.toString());
            return Result.success(hasLiked != null && hasLiked);
        } catch (Exception e) {
            LambdaQueryWrapper<PostLike> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(PostLike::getPostId, postId);
            wrapper.eq(PostLike::getUserId, userId);
            PostLike likeRecord = postLikeMapper.selectOne(wrapper);
            return Result.success(likeRecord != null);
        }
    }

    @Override
    public Result<?> getMyLikedPosts(Integer pageNum, Integer pageSize) {
        Long userId = UserContext.getUserId();

        Page<PostLike> likePage = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<PostLike> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PostLike::getUserId, userId);
        wrapper.orderByDesc(PostLike::getCreateTime);
        Page<PostLike> likeResult = postLikeMapper.selectPage(likePage, wrapper);

        List<Long> postIds = likeResult.getRecords().stream()
                .map(PostLike::getPostId)
                .collect(Collectors.toList());

        Page<Post> resultPage = new Page<>(pageNum, pageSize, likeResult.getTotal());
        if (!postIds.isEmpty()) {
            LambdaQueryWrapper<Post> postWrapper = new LambdaQueryWrapper<>();
            postWrapper.in(Post::getId, postIds);
            postWrapper.orderByDesc(Post::getCreateTime);
            List<Post> posts = postMapper.selectList(postWrapper);
            resultPage.setRecords(posts);
            fillUserInfo(resultPage);
            fillLikeCountFromRedis(posts);
            fillCommentCountFromRedis(posts);
            fillHasLiked(posts);
            fillHasFavorited(posts);  // ✅ 新增
        }
        return Result.success(resultPage);
    }

    @Override
    public Result<?> searchPosts(String keyword, Integer pageNum, Integer pageSize) {
        if (!StringUtils.hasText(keyword)) {
            return Result.fail(400, "搜索关键词不能为空");
        }
        Page<Post> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(Post::getTitle, keyword)
                .or()
                .like(Post::getContent, keyword);
        wrapper.orderByDesc(Post::getCreateTime);
        Page<Post> result = postMapper.selectPage(page, wrapper);
        fillUserInfo(result);
        fillLikeCountFromRedis(result.getRecords());
        fillCommentCountFromRedis(result.getRecords());
        fillHasLiked(result.getRecords());
        fillHasFavorited(result.getRecords());  // ✅ 新增
        return Result.success(result);
    }

    @Override
    public Result<?> getPostsByUser(Long userId, Integer pageNum, Integer pageSize) {
        Page<Post> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Post::getUserId, userId);
        wrapper.orderByDesc(Post::getCreateTime);
        Page<Post> result = postMapper.selectPage(page, wrapper);
        fillUserInfo(result);
        fillLikeCountFromRedis(result.getRecords());
        fillCommentCountFromRedis(result.getRecords());
        fillHasLiked(result.getRecords());
        fillHasFavorited(result.getRecords());  // ✅ 新增
        return Result.success(result);
    }

    @Override
    public Result<?> getMyPosts(Integer pageNum, Integer pageSize) {
        Long userId = UserContext.getUserId();
        Page<Post> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Post::getUserId, userId);
        wrapper.orderByDesc(Post::getCreateTime);
        Page<Post> result = postMapper.selectPage(page, wrapper);
        fillUserInfo(result);
        fillLikeCountFromRedis(result.getRecords());
        fillCommentCountFromRedis(result.getRecords());
        fillHasFavorited(result.getRecords());  // ✅ 新增
        return Result.success(result);
    }

    // ========== 私有辅助方法 ==========

    private void fillUserInfo(IPage<Post> page) {
        List<Post> records = page.getRecords();
        if (records != null && !records.isEmpty()) {
            List<Long> userIds = records.stream().map(Post::getUserId).collect(Collectors.toList());
            List<SysUser> users = userService.listByIds(userIds);
            Map<Long, SysUser> userMap = users.stream().collect(Collectors.toMap(SysUser::getId, u -> u));
            for (Post post : records) {
                SysUser user = userMap.get(post.getUserId());
                if (user != null) {
                    post.setNickname(user.getNickname());
                    post.setAvatar(user.getAvatar());
                }
            }
        }
    }
    private void fillLikeCountFromRedis(List<Post> posts) {
        if (posts == null || posts.isEmpty()) return;
        for (Post post : posts) {
            Long count = getLikeCountFromRedis(post.getId());
            if (count != null) {
                post.setLikeCount(count.intValue());
            }
        }
    }

    private void fillCommentCountFromRedis(List<Post> posts) {
        if (posts == null || posts.isEmpty()) return;
        for (Post post : posts) {
            Long count = getCommentCountFromRedis(post.getId());
            if (count != null) {
                post.setCommentCount(count.intValue());
            }
        }
    }

    private void fillHasLiked(List<Post> posts) {
        if (posts == null || posts.isEmpty()) return;
        Long currentUserId = UserContext.getUserId();
        for (Post post : posts) {
            String key = POST_LIKE_KEY + post.getId();
            try {
                Boolean hasLiked = redisUtil.sIsMember(key, currentUserId.toString());
                post.setHasLiked(hasLiked != null && hasLiked);
            } catch (Exception e) {
                LambdaQueryWrapper<PostLike> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(PostLike::getPostId, post.getId());
                wrapper.eq(PostLike::getUserId, currentUserId);
                post.setHasLiked(postLikeMapper.selectOne(wrapper) != null);
            }
        }
    }

    // ✅ 新增：填充收藏状态
    private void fillHasFavorited(List<Post> posts) {
        if (posts == null || posts.isEmpty()) return;
        Long currentUserId = UserContext.getUserId();
        if (currentUserId == null) {
            for (Post post : posts) {
                post.setIsFavorited(false);
            }
            return;
        }

        List<Long> postIds = posts.stream().map(Post::getId).collect(Collectors.toList());
        try {
            List<Long> favoritedIds = favoriteMapper.selectFavoritedPostIds(currentUserId, postIds);
            Set<Long> favoritedSet = new HashSet<>(favoritedIds);
            for (Post post : posts) {
                post.setIsFavorited(favoritedSet.contains(post.getId()));
            }
        } catch (Exception e) {
            log.error("批量查询收藏状态失败: {}", e.getMessage());
            for (Post post : posts) {
                post.setIsFavorited(false);
            }
        }
    }

    private Long getLikeCountFromRedis(Long postId) {
        String postLikeKey = POST_LIKE_KEY + postId;
        try {
            Long count = redisUtil.sCard(postLikeKey);
            if (count != null) {
                return count;
            }
            Long dbCount = postMapper.selectLikeCount(postId);
            return dbCount != null ? dbCount : 0L;
        } catch (Exception e) {
            log.error("Redis读取失败，降级MySQL: {}", e.getMessage());
            Long dbCount = postMapper.selectLikeCount(postId);
            return dbCount != null ? dbCount : 0L;
        }
    }

    private Long getCommentCountFromRedis(Long postId) {
        String key = COMMENT_COUNT_KEY_PREFIX + postId;
        try {
            String countStr = redisUtil.get(key);
            if (countStr != null) {
                return Long.valueOf(countStr);
            }
            Post post = getById(postId);
            if (post != null && post.getCommentCount() != null) {
                redisUtil.set(key, String.valueOf(post.getCommentCount()));
                return post.getCommentCount().longValue();
            }
            return 0L;
        } catch (Exception e) {
            log.error("Redis读取评论数失败，降级MySQL: {}", e.getMessage());
            Post post = getById(postId);
            return post != null ? post.getCommentCount().longValue() : 0L;
        }
    }

    private Result<?> toggleLikeByRedisSAdd(Long postId, Long userId, String postLikeKey) {
        try {
            Long added = redisUtil.sAdd(postLikeKey, userId.toString());
            if (added != null && added > 0) {
                redisUtil.sAdd(POST_LIKE_SYNC_KEY, postId.toString());
                log.info("【Redis】点赞成功，用户 {} 帖子 {}", userId, postId);
                return Result.success("点赞成功");
            } else {
                redisUtil.sRem(postLikeKey, userId.toString());
                redisUtil.sAdd(POST_LIKE_SYNC_KEY, postId.toString());
                log.info("【Redis】取消点赞，用户 {} 帖子 {}", userId, postId);
                return Result.success("取消点赞");
            }
        } catch (Exception ex) {
            log.error("Redis完全失败，降级MySQL: {}", ex.getMessage());
            return toggleLikeByMySQL(postId, userId);
        }
    }

    private Result<?> toggleLikeByMySQL(Long postId, Long userId) {
        LambdaQueryWrapper<PostLike> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PostLike::getPostId, postId).eq(PostLike::getUserId, userId);
        PostLike likeRecord = postLikeMapper.selectOne(wrapper);

        if (likeRecord != null) {
            // 取消点赞
            postLikeMapper.deleteById(likeRecord.getId());
            // 同步更新计数
            Long dbCount = postMapper.selectLikeCount(postId);
            postMapper.updateLikeCount(postId, dbCount != null ? Math.max(0, dbCount - 1) : 0);
            log.info("【MySQL】取消点赞，用户 {} 帖子 {}", userId, postId);
            return Result.success("取消点赞");
        } else {
            try {
                PostLike newLike = new PostLike();
                newLike.setPostId(postId);
                newLike.setUserId(userId);
                postLikeMapper.insert(newLike);
                // 同步更新计数
                Long dbCount = postMapper.selectLikeCount(postId);
                postMapper.updateLikeCount(postId, dbCount != null ? dbCount + 1 : 1);
                log.info("【MySQL】点赞成功，用户 {} 帖子 {}", userId, postId);
                return Result.success("点赞成功");
            } catch (DuplicateKeyException e) {
                log.warn("重复点赞（数据库唯一索引拦截），用户 {} 帖子 {}", userId, postId);
                return Result.success("点赞成功");
            }
        }
    }
    @Override
    public Result<?> deletePost(Long postId) {
        Post post = getById(postId);
        if (post == null) {
            return Result.fail(404, "帖子不存在");
        }

        Long currentUserId = UserContext.getUserId();
        // 权限校验：只能删自己的帖子
        if (!post.getUserId().equals(currentUserId)) {
            return Result.fail(403, "无权删除他人帖子");
        }

        // 1. 删帖子本身
        removeById(postId);

        // 2. 清理 Redis 缓存
        redisUtil.delete(POST_LIKE_KEY + postId);
        redisUtil.delete(COMMENT_COUNT_KEY_PREFIX + postId);

        // 3. 清理该帖子的收藏记录（可选，看业务需求）
        LambdaQueryWrapper<Favorite> favWrapper = new LambdaQueryWrapper<>();
        favWrapper.eq(Favorite::getPostId, postId);
        favoriteMapper.delete(favWrapper);

        // 4. 清理点赞记录（可选）
        LambdaQueryWrapper<PostLike> likeWrapper = new LambdaQueryWrapper<>();
        likeWrapper.eq(PostLike::getPostId, postId);
        postLikeMapper.delete(likeWrapper);

        log.info("【删除帖子】成功，用户 {} 删除了帖子 {}", currentUserId, postId);
        return Result.success("删除成功");
    }
}