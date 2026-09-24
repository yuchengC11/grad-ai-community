package com.graduate.graidaicommunity.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.graduate.graidaicommunity.common.Result;
import com.graduate.graidaicommunity.common.UserContext;
import com.graduate.graidaicommunity.mapper.PrivateMessageMapper;
import com.graduate.graidaicommunity.pojo.PrivateMessage;
import com.graduate.graidaicommunity.pojo.SysUser;
import com.graduate.graidaicommunity.service.PrivateMessageService;
import com.graduate.graidaicommunity.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
public class PrivateMessageServiceImpl extends ServiceImpl<PrivateMessageMapper, PrivateMessage> implements PrivateMessageService {

    @Resource
    private UserService userService;

    @Override
    @Transactional
    public Result<?> sendMessage(Long toUserId, String content) {
        Long fromUserId = UserContext.getUserId();
        if (toUserId == null || toUserId.equals(fromUserId)) {
            return Result.fail(400, "不能给自己发消息");
        }
        SysUser toUser = userService.getById(toUserId);
        if (toUser == null) {
            return Result.fail(404, "接收用户不存在");
        }
        if (content == null || content.trim().isEmpty()) {
            return Result.fail(400, "消息内容不能为空");
        }
        if (content.length() > 500) {
            return Result.fail(400, "消息内容不能超过500字");
        }
// 敏感词过滤
        //  无需检查好友关系，直接发送
        PrivateMessage msg = new PrivateMessage();
        msg.setFromUserId(fromUserId);
        msg.setToUserId(toUserId);
        msg.setContent(content);
        msg.setIsRead(0);
        this.save(msg);
        return Result.success("发送成功");
    }

    @Override
    public Result<?> getConversation(Long userId, Integer pageNum, Integer pageSize) {
        Long currentUserId = UserContext.getUserId();
        LambdaQueryWrapper<PrivateMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w ->
                w.eq(PrivateMessage::getFromUserId, currentUserId)
                        .eq(PrivateMessage::getToUserId, userId)
        ).or(w ->
                w.eq(PrivateMessage::getFromUserId, userId)
                        .eq(PrivateMessage::getToUserId, currentUserId)
        );
        wrapper.orderByDesc(PrivateMessage::getCreateTime);
        Page<PrivateMessage> page = new Page<>(pageNum, pageSize);
        Page<PrivateMessage> result = this.page(page, wrapper);
        return Result.success(result);
    }

    @Override
    public Result<Integer> getUnreadCount() {
        Long userId = UserContext.getUserId();
        LambdaQueryWrapper<PrivateMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PrivateMessage::getToUserId, userId)
                .eq(PrivateMessage::getIsRead, 0);
        long count = this.count(wrapper);
        return Result.success((int) count);
    }

    @Override
    @Transactional
    public Result<?> markAsRead(Long userId) {
        Long currentUserId = UserContext.getUserId();
        LambdaQueryWrapper<PrivateMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PrivateMessage::getFromUserId, userId)
                .eq(PrivateMessage::getToUserId, currentUserId)
                .eq(PrivateMessage::getIsRead, 0);
        PrivateMessage update = new PrivateMessage();
        update.setIsRead(1);
        this.update(update, wrapper);
        return Result.success("已读");
    }
    @Override
    public Result<?> getChatList() {
        Long userId = UserContext.getUserId();
        LambdaQueryWrapper<PrivateMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PrivateMessage::getFromUserId, userId)
                .or()
                .eq(PrivateMessage::getToUserId, userId);
        wrapper.orderByDesc(PrivateMessage::getCreateTime);
        List<PrivateMessage> msgs = this.list(wrapper);

        Set<Long> userIds = new HashSet<>();
        for (PrivateMessage msg : msgs) {
            if (msg.getFromUserId().equals(userId)) {
                userIds.add(msg.getToUserId());
            } else {
                userIds.add(msg.getFromUserId());
            }
        }
        if (userIds.isEmpty()) {
            return Result.success(new ArrayList<>());
        }

        List<SysUser> users = userService.listByIds(userIds);

        List<Map<String, Object>> result = new ArrayList<>();
        for (SysUser user : users) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", user.getId());
            item.put("nickname", user.getNickname());
            item.put("avatar", user.getAvatar());

            LambdaQueryWrapper<PrivateMessage> lastWrapper = new LambdaQueryWrapper<>();
            lastWrapper.and(w ->
                    w.eq(PrivateMessage::getFromUserId, userId)
                            .eq(PrivateMessage::getToUserId, user.getId())
            ).or(w ->
                    w.eq(PrivateMessage::getFromUserId, user.getId())
                            .eq(PrivateMessage::getToUserId, userId)
            );
            lastWrapper.orderByDesc(PrivateMessage::getCreateTime);
            lastWrapper.last("LIMIT 1");
            PrivateMessage last = this.getOne(lastWrapper);
            if (last != null) {
                item.put("lastContent", last.getContent());
                item.put("lastTime", last.getCreateTime());
            }

            LambdaQueryWrapper<PrivateMessage> unreadWrapper = new LambdaQueryWrapper<>();
            unreadWrapper.eq(PrivateMessage::getFromUserId, user.getId())
                    .eq(PrivateMessage::getToUserId, userId)
                    .eq(PrivateMessage::getIsRead, 0);
            int unread = (int) this.count(unreadWrapper);
            item.put("unreadCount", unread);
            result.add(item);
        }

            result.sort((a, b) -> {
            LocalDateTime t1 = (LocalDateTime) a.get("lastTime");
            LocalDateTime t2 = (LocalDateTime) b.get("lastTime");
            if (t1 == null && t2 == null) return 0;
            if (t1 == null) return 1;
            if (t2 == null) return -1;
            return t2.compareTo(t1);
        });
        return Result.success(result);
    }
}