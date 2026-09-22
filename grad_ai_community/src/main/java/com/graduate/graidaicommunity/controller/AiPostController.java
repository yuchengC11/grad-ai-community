package com.graduate.graidaicommunity.controller;

import com.graduate.graidaicommunity.common.AiException;
import com.graduate.graidaicommunity.common.Result;
import com.graduate.graidaicommunity.service.ai.AiPostService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@Slf4j
@RestController
@RequestMapping("/ai/post")
public class AiPostController {

    @Resource
    private AiPostService aiPostService;

    @GetMapping("/summary")
    public Result<String> getPostSummary(@RequestParam("postId") Long postId) {
        if (postId == null || postId <= 0) {
            return Result.fail(400, "postId 必须是正整数");
        }
        try {
            return aiPostService.generatePostSummary(postId);
        } catch (AiException e) {
            log.error("AI总结异常, postId={}: {}", postId, e.getMessage());
            return Result.fail(503, "AI总结服务繁忙，请稍后再试");
        } catch (Exception e) {
            log.error("AI总结系统异常, postId={}", postId, e);
            return Result.fail(500, "系统繁忙");
        }
    }

    @GetMapping("/genComment")
    public Result<String> getAiComment(
            @RequestParam("postId") Long postId,
            @RequestParam("prompt") String prompt
    ) {
        if (postId == null || postId <= 0) {
            return Result.fail(400, "postId 必须是正整数");
        }
        if (prompt == null || prompt.trim().isEmpty()) {
            return Result.fail(400, "prompt 不能为空");
        }
        if (prompt.length() > 500) {
            return Result.fail(400, "prompt 长度不能超过500字");
        }
        try {
            return aiPostService.generateAiComment(postId, prompt);
        } catch (AiException e) {
            log.error("AI评论异常, postId={}: {}", postId, e.getMessage());
            return Result.fail(503, "AI评论服务繁忙，请稍后再试");
        } catch (Exception e) {
            log.error("AI评论系统异常, postId={}", postId, e);
            return Result.fail(500, "系统繁忙");
        }
    }
}