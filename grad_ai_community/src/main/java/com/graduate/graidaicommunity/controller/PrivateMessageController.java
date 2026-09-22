package com.graduate.graidaicommunity.controller;

import com.graduate.graidaicommunity.common.Result;
import com.graduate.graidaicommunity.common.UserContext;
import com.graduate.graidaicommunity.service.PrivateMessageService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Map;

@RestController
@RequestMapping("/message")
public class PrivateMessageController {
    @Resource
    private PrivateMessageService privateMessageService;

    @PostMapping("/send")
    public Result<?> send(@RequestBody Map<String, Object> body) {
        Object toUserIdObj = body.get("toUserId");
        Object contentObj = body.get("content");
        if (toUserIdObj == null || contentObj == null) {
            return Result.fail(400, "toUserId和content不能为空");
        }
        Long toUserId;
        try {
            toUserId = Long.parseLong(toUserIdObj.toString());
        } catch (NumberFormatException e) {
            return Result.fail(400, "toUserId格式错误");
        }
        String content = contentObj.toString().trim();
        if (content.isEmpty()) {
            return Result.fail(400, "消息内容不能为空");
        }
        if (content.length() > 500) {
            return Result.fail(400, "消息内容不能超过500字");
        }
        return privateMessageService.sendMessage(toUserId, content);
    }

    @GetMapping("/conversation/{userId}")
    public Result<?> getConversation(@PathVariable Long userId,
                                     @RequestParam(defaultValue = "1") Integer pageNum,
                                     @RequestParam(defaultValue = "20") Integer pageSize) {
        return privateMessageService.getConversation(userId, pageNum, pageSize);
    }

    @GetMapping("/unread")
    public Result<Integer> getUnreadCount() {
        return privateMessageService.getUnreadCount();
    }

    @PostMapping("/read")
    public Result<?> markAsRead(@RequestParam Long userId) {
        return privateMessageService.markAsRead(userId);
    }

    @GetMapping("/chat/list")
    public Result<?> getChatList() {
        return privateMessageService.getChatList();
    }
}