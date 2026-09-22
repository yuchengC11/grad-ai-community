package com.graduate.graidaicommunity.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.graduate.graidaicommunity.common.Result;
import com.graduate.graidaicommunity.pojo.Friend;
import org.springframework.transaction.annotation.Transactional;

public interface FriendService extends IService<Friend> {
    @Transactional
    void deleteFriend(Long friendId);
    // 同意好友请求
    Result<?> acceptFriend(Long friendId);
    // 拒绝好友请求
    Result<?> rejectFriend(Long friendId);
    // 获取好友列表（已同意的）
    Result<?> getFriendList();
    // 获取待处理的好友请求
    Result<?> getPendingRequests();
    Result<?> addFriend(Long toUserId);
}