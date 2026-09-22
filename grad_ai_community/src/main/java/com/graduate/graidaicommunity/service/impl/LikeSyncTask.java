package com.graduate.graidaicommunity.service.impl;

import com.graduate.graidaicommunity.mapper.PostLikeMapper;
import com.graduate.graidaicommunity.mapper.PostMapper;
import com.graduate.graidaicommunity.pojo.PostLike;
import com.graduate.graidaicommunity.util.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Slf4j
@Component
public class LikeSyncTask {

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private PostLikeMapper postLikeMapper;

    @Autowired
    private PostMapper postMapper;

    private static final String POST_LIKE_KEY = "post:like:";
    private static final String POST_LIKE_SYNC_KEY = "post:like:sync";

    @Scheduled(cron = "0 */5 * * * ?")
    @Transactional(rollbackFor = Exception.class)
    public void syncLikeToMySQL() {
        Set<String> postIds = redisUtil.sMembers(POST_LIKE_SYNC_KEY);
        if (postIds == null || postIds.isEmpty()) {
            log.info("【定时任务】暂无需要同步的点赞数据");
            return;
        }

        log.info("【定时任务】开始同步点赞数据，涉及帖子数: {}", postIds.size());

        for (String pid : postIds) {
            Long postId = Long.valueOf(pid);
            try {
                syncSinglePost(postId);
                redisUtil.sRem(POST_LIKE_SYNC_KEY, pid);
            } catch (Exception e) {
                log.error("【定时任务】同步帖子[{}]点赞数据失败: {}", postId, e.getMessage());
            }
        }

        log.info("【定时任务】点赞数据同步完成");
    }

    private void syncSinglePost(Long postId) {
        String likeKey = POST_LIKE_KEY + postId;

        Set<String> userIds = redisUtil.sMembers(likeKey);

        long likeCount = (userIds != null) ? userIds.size() : 0;

        postLikeMapper.deleteByPostId(postId);

        if (userIds != null && !userIds.isEmpty()) {
            for (String uid : userIds) {
                PostLike like = new PostLike();
                like.setPostId(postId);
                like.setUserId(Long.valueOf(uid));
                postLikeMapper.insert(like);
            }
        }

        postMapper.updateLikeCount(postId, likeCount);
        log.info("【定时任务】帖子[{}]同步完成，点赞数: {}", postId, likeCount);
    }
}