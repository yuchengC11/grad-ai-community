package com.graduate.graidaicommunity.controller;

import com.graduate.graidaicommunity.common.Result;
import com.graduate.graidaicommunity.dto.ChatRequest;
import com.graduate.graidaicommunity.service.ai.AiPostService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import javax.annotation.Resource;
import javax.validation.Valid;

@RestController
public class AiChatController {
    @Resource
    private AiPostService aiPostService;

    @PostMapping("/ai/chat")
    public Result<String> chat(@RequestBody @Valid ChatRequest request) {
        return aiPostService.chat(request.getQuestion(), request.getHistory(), request.getPersona());
    }
    @PostMapping("/ai/stream")
    public SseEmitter streamChat(@RequestBody @Valid ChatRequest request) {
        return aiPostService.streamChat(
                request.getQuestion(),
                request.getHistory(),
                request.getPersona()
        );
    }

}