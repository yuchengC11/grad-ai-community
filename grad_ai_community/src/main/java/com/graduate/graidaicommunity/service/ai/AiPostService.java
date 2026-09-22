package com.graduate.graidaicommunity.service.ai;

import com.graduate.graidaicommunity.common.Result;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

public interface AiPostService {
    Result<String> generatePostSummary(Long postId);

    Result<String> generateAiComment(Long postId, String prompt);

    Result<String> chat(String question, List<Map<String, String>> history, String persona);

    SseEmitter streamChat(String question, List<Map<String, String>> history, String persona);
    void doStreamChat(SseEmitter emitter, String question,
                      List<Map<String, String>> history, String persona);
}