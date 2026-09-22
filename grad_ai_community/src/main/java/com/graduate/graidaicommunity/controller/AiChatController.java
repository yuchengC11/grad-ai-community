package com.graduate.graidaicommunity.controller;

import com.graduate.graidaicommunity.common.Result;
import com.graduate.graidaicommunity.dto.ChatRequest;
import com.graduate.graidaicommunity.service.ai.AiPostService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
public class AiChatController {

    @Resource
    private AiPostService aiPostService;

    @PostMapping("/ai/chat")
    public Result<String> chat(@RequestBody @Valid ChatRequest request) {
        return aiPostService.chat(request.getQuestion(), request.getHistory(), request.getPersona());
    }
}