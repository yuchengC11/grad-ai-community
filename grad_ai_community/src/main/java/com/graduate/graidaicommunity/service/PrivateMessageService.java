package com.graduate.graidaicommunity.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.graduate.graidaicommunity.common.Result;
import com.graduate.graidaicommunity.pojo.PrivateMessage;

public interface PrivateMessageService extends IService<PrivateMessage> {

    Result<?> sendMessage(Long toUserId, String content);

    Result<?> getConversation(Long userId, Integer pageNum, Integer pageSize);

    Result<Integer> getUnreadCount();

    Result<?> markAsRead(Long userId);
    Result<?> getChatList();
}