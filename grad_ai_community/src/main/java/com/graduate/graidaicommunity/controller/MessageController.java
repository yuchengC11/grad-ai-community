package com.graduate.graidaicommunity.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.graduate.graidaicommunity.common.Result;
import com.graduate.graidaicommunity.common.UserContext;
import com.graduate.graidaicommunity.mapper.MessageMapper;
import com.graduate.graidaicommunity.pojo.SysMessage;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/sys-message")
public class MessageController {

    @Resource
    private MessageMapper messageMapper;

    @GetMapping("/list")
    public Result<List<SysMessage>> getMyMessage(
            @RequestParam(required = false, defaultValue = "all") String type) {
        Long userId = UserContext.getUserId();
        LambdaQueryWrapper<SysMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysMessage::getUserId, userId);
        if ("unread".equals(type)) {
            wrapper.eq(SysMessage::getIsRead, 0);
        }
        else if ("friend".equals(type)) {
            return Result.success(Collections.emptyList());
        }
        wrapper.orderByDesc(SysMessage::getCreateTime);
        List<SysMessage> list = messageMapper.selectList(wrapper);
        return Result.success(list);
    }
}