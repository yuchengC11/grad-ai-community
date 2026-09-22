package com.graduate.graidaicommunity.service.impl;

import com.graduate.graidaicommunity.pojo.Post;
import com.graduate.graidaicommunity.service.PostService;
import com.graduate.graidaicommunity.util.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Set;

@Slf4j
@Component
public class CommentCountSyncTask {

    @Resource
    private RedisUtil redisUtil;

    @Resource
    private PostService postService;

    private static final String COMMENT_COUNT_KEY_PREFIX = "post:comment:count:";
    private static final String COMMENT_COUNT_SYNC_KEY = "post:comment:sync";

    @Scheduled(fixedDelay = 5000)
    @Transactional(rollbackFor = Exception.class)
    public void syncCommentCountToMySQL() {
        Set<String> postIds = redisUtil.sMembers(COMMENT_COUNT_SYNC_KEY);
        if (postIds == null || postIds.isEmpty()) {
            return;
        }

        for (String postIdStr : postIds) {
            Long postId = Long.valueOf(postIdStr);
            String key = COMMENT_COUNT_KEY_PREFIX + postId;
            String countStr = redisUtil.get(key);
            if (countStr != null) {
                Post post = postService.getById(postId);
                if (post != null) {
                    post.setCommentCount(Integer.parseInt(countStr));
                    postService.updateById(post);
                }
            }
            redisUtil.sRem(COMMENT_COUNT_SYNC_KEY, postIdStr);
        }
    }
}