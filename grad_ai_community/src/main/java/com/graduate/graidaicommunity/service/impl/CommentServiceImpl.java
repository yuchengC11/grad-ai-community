package com.graduate.graidaicommunity.service.impl;
import com.graduate.graidaicommunity.util.RedisUtil;
import javax.annotation.Resource;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.graduate.graidaicommunity.common.Result;
import com.graduate.graidaicommunity.common.UserContext;
import com.graduate.graidaicommunity.mapper.CommentMapper;
import com.graduate.graidaicommunity.pojo.Comment;
import com.graduate.graidaicommunity.pojo.Post;
import com.graduate.graidaicommunity.pojo.SysUser;
import com.graduate.graidaicommunity.service.CommentService;
import com.graduate.graidaicommunity.service.PostService;
import com.graduate.graidaicommunity.service.UserService;
import com.graduate.graidaicommunity.util.RedisUtil;
import com.graduate.graidaicommunity.vo.CommentAddDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CommentServiceImpl extends ServiceImpl<CommentMapper, Comment> implements CommentService {
    @Resource
    private RedisUtil redisUtil;
    @Resource
    private UserService userService;

    @Resource
    private PostService postService;


    private static final String COMMENT_COUNT_KEY_PREFIX = "post:comment:count:";
    private static final String COMMENT_COUNT_SYNC_KEY = "post:comment:sync";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<?> addComment(CommentAddDTO dto) {
        // 1. 校验帖子是否存在
        Post post = postService.getById(dto.getPostId());
        if (post == null) {
            return Result.fail(400, "帖子不存在，无法评论");
        }

        // 2. 保存评论记录
        Comment comment = new Comment();
        comment.setPostId(dto.getPostId());
        comment.setUserId(UserContext.getUserId());
        comment.setContent(dto.getContent());
        this.save(comment);

        // 3. 更新评论数缓存（优先Redis）
        try {
            redisUtil.increment(COMMENT_COUNT_KEY_PREFIX + dto.getPostId(), 1);
            redisUtil.sAdd(COMMENT_COUNT_SYNC_KEY, dto.getPostId().toString());
            log.info("【Redis】评论数+1，帖子 {}", dto.getPostId());
        } catch (Exception e) {
            log.warn("Redis评论数更新失败，降级MySQL: {}", e.getMessage());
            // 防止 commentCount 为 null 导致空指针
            int currentCount = post.getCommentCount() == null ? 0 : post.getCommentCount();
            post.setCommentCount(currentCount + 1);
            postService.updateById(post);
        }

        return Result.success("评论成功");
    }

    @Override
    public Result<?> getCommentList(Long postId) {
        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Comment::getPostId, postId);
        wrapper.orderByDesc(Comment::getCreateTime);
        List<Comment> list = this.list(wrapper);
        // 填充昵称
        if (!list.isEmpty()) {
            List<Long> userIds = list.stream().map(Comment::getUserId).collect(Collectors.toList());
            List<SysUser> users = userService.listByIds(userIds);
            Map<Long, SysUser> userMap = users.stream().collect(Collectors.toMap(SysUser::getId, u -> u));
            for (Comment comment : list) {
                SysUser user = userMap.get(comment.getUserId());
                if (user != null) {
                    comment.setNickname(user.getNickname());
                }
            }
        }
        return Result.success(list);
    }
}