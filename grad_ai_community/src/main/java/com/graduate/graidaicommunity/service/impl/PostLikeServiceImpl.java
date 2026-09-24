package com.graduate.graidaicommunity.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.graduate.graidaicommunity.common.Result;
import com.graduate.graidaicommunity.common.UserContext;
import com.graduate.graidaicommunity.mapper.PostLikeMapper;
import com.graduate.graidaicommunity.pojo.Post;
import com.graduate.graidaicommunity.pojo.PostLike;
import com.graduate.graidaicommunity.service.PostLikeService;
import com.graduate.graidaicommunity.service.PostService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

@Service
public class PostLikeServiceImpl extends ServiceImpl<PostLikeMapper, PostLike> implements PostLikeService {

    @Resource
    private PostService postService;
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<?> toggleLike(Long postId) {
        // 1. 校验帖子是否存在
        Post post = postService.getById(postId);
        if (post == null) {
            return Result.fail(400, "帖子不存在");
        }

        Long userId = UserContext.getUserId();
        LambdaQueryWrapper<PostLike> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PostLike::getPostId, postId);
        wrapper.eq(PostLike::getUserId, userId);
        PostLike likeRecord = this.getOne(wrapper);

        if (likeRecord != null) {
            // 2. 已点赞：取消点赞
            this.removeById(likeRecord.getId());
            post.setLikeCount(post.getLikeCount() - 1);
            postService.updateById(post);
            return Result.success("取消点赞");
        } else {
            // 3. 未点赞：执行点赞
            try {
                PostLike newLike = new PostLike();
                newLike.setPostId(postId);
                newLike.setUserId(userId);
                this.save(newLike);

                post.setLikeCount(post.getLikeCount() + 1);
                postService.updateById(post);
                return Result.success("点赞成功");
            } catch (DuplicateKeyException e) {
                // 并发场景兜底：联合唯一索引触发重复，直接返回已点赞
                return Result.success("点赞成功");
            }
        }
    }
}