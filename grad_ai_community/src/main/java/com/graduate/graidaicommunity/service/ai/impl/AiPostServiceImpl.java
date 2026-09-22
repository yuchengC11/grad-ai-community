package com.graduate.graidaicommunity.service.ai.impl;

import com.graduate.graidaicommunity.common.AiException;
import com.graduate.graidaicommunity.common.Result;
import com.graduate.graidaicommunity.pojo.Post;
import com.graduate.graidaicommunity.service.PostService;
import com.graduate.graidaicommunity.service.ai.AiPostService;
import com.graduate.graidaicommunity.service.ai.DashScopeClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;


@Slf4j
@Service
public class AiPostServiceImpl implements AiPostService {

    @Resource
    private PostService postService;
    @Resource
    private DashScopeClient dashScopeClient;
    @Lazy
    @Resource
    private AiPostService selfProxy;

    private static final String SYSTEM_BASE =
            "你是一位专业、严谨的毕业生互助社区AI顾问。回答时必须遵循以下格式：\n\n" +
                    "【核心建议】\n用2-3句话给出最直接、可落地的核心观点。\n\n" +
                    "【详细分析】\n分点说明，用\"1.\"、\"2.\"、\"3.\"编号。每一点包含：具体做法 + 实际例子。\n\n" +
                    "【行动清单】\n列出3条今天就能执行的具体行动步骤。\n\n" +
                    "【注意事项】\n提醒1-2个常见误区。\n\n" +
                    "规则：\n" +
                    "- 禁止使用\"|\"、未闭合的\"**\"、乱码符号拼接内容\n" +
                    "- 禁止直接堆砌关键词，必须组织成通顺的中文段落\n" +
                    "- 每段话必须语义完整、标点正确\n" +
                    "- 总字数控制在300-500字\n";

    private static final Map<String, String> PERSONA_PROMPT;

    static {
        Map<String, String> map = new HashMap<>();
        map.put("employment", SYSTEM_BASE + "\n当前场景：就业建议。重点分析目标岗位、简历优化、面试准备、薪资谈判。");
        map.put("postgraduate", SYSTEM_BASE + "\n当前场景：考研经验。重点分析院校选择、复习规划、专业课重点、心态调整。");
        map.put("civil", SYSTEM_BASE + "\n当前场景：考公备考。重点分析岗位筛选、行测技巧、申论写作、面试策略。");
        map.put("job", SYSTEM_BASE + "\n当前场景：求职感悟。重点分析职场适应、人际关系、技能提升、职业规划。");
        map.put("default", SYSTEM_BASE + "\n当前场景：通用问答。针对毕业生的学习、生活、职业发展给出建议。");
        PERSONA_PROMPT = java.util.Collections.unmodifiableMap(map);
    }

    @Override
    public Result<String> generatePostSummary(Long postId) {
        Post post = postService.getById(postId);
        if (post == null) {
            return Result.fail(400, "帖子不存在");
        }
        String prompt = "请对求职社区帖子生成100字以内精简摘要，只输出摘要正文，不要多余解释、符号：帖子标题："
                + post.getTitle() + "；帖子正文：" + post.getContent();
        try {
            String res = dashScopeClient.chatCompletion(prompt, null, null, null);
            return Result.success(res);
        } catch (AiException e) {
            log.warn("AI总结降级, postId={}: {}", postId, e.getMessage());
            return Result.fail(503, e.getMessage());
        } catch (Exception e) {
            log.error("AI总结系统异常, postId={}", postId, e);
            return Result.fail(500, "系统繁忙");
        }
    }

    @Override
    public Result<String> generateAiComment(Long postId, String prompt) {
        Post post = postService.getById(postId);
        if (post == null) {
            return Result.fail(400, "帖子不存在");
        }
        String fullPrompt = "根据帖子内容生成80字以内友好社区评论，严格遵守用户要求：帖子【"
                + post.getTitle() + "】" + post.getContent() + " 用户需求：" + prompt;
        try {
            String res = dashScopeClient.chatCompletion(fullPrompt, null, null, null);
            return Result.success(res);
        } catch (AiException e) {
            log.warn("AI评论降级, postId={}: {}", postId, e.getMessage());
            return Result.fail(503, e.getMessage());
        } catch (Exception e) {
            log.error("AI评论系统异常, postId={}", postId, e);
            return Result.fail(500, "系统繁忙");
        }
    }

    @Override
    public Result<String> chat(String question, List<Map<String, String>> history, String persona) {
        if (question == null || question.trim().isEmpty()) {
            return Result.fail(400, "问题不能为空");
        }
        try {
            String fullPrompt = buildPrompt(question, persona);
            String answer = dashScopeClient.chatCompletion(fullPrompt, history, persona, null);
            return Result.success(cleanAiResponse(answer));
        } catch (AiException e) {
            log.warn("AI聊天降级: {}", e.getMessage());
            return Result.fail(503, e.getMessage());
        } catch (Exception e) {
            log.error("AI聊天系统异常", e);
            return Result.fail(500, "系统繁忙");
        }
    }

    @Override
    public SseEmitter streamChat(String question, List<Map<String, String>> history, String persona) {
        SseEmitter emitter = new SseEmitter(120000L);
        // ✅ 关键修复：通过 selfProxy（代理对象）调用，@Async 才会生效
        // 不能写 this.doStreamChat(...)，那样是内部调用，不走代理
        selfProxy.doStreamChat(emitter, question, history, persona);
        return emitter;
    }

    @Async("aiExecutor")
    public void doStreamChat(SseEmitter emitter, String question,
                             List<Map<String, String>> history, String persona) {
        try {
            String fullPrompt = buildPrompt(question, persona);
            dashScopeClient.chatCompletion(
                    fullPrompt, history, persona,
                    (chunk) -> {
                        try {
                            String cleanChunk = chunk.replaceAll("[`•◆▶]+", "");
                            emitter.send(SseEmitter.event().data(cleanChunk));
                        } catch (IOException e) {
                            log.warn("SSE发送失败，客户端可能已断开: {}", e.getMessage());
                            emitter.completeWithError(e);
                        }
                    }
            );
            emitter.send(SseEmitter.event().data("[DONE]"));
            emitter.complete();
        } catch (AiException e) {
            log.warn("AI流式降级: {}", e.getMessage());
            safeSendAndComplete(emitter, "AI服务繁忙，请稍后再试");
        } catch (RejectedExecutionException e) {
            log.warn("AI线程池拒绝，触发限流: {}", e.getMessage());
            safeSendAndComplete(emitter, "AI服务繁忙，请稍后再试");
        } catch (Exception e) {
            log.error("AI流式系统异常", e);
            emitter.completeWithError(e);
        }
    }

    private void safeSendAndComplete(SseEmitter emitter, String msg) {
        try {
            emitter.send(SseEmitter.event().data(msg));
            emitter.complete();
        } catch (IOException ex) {
            emitter.completeWithError(ex);
        }
    }

    private String buildPrompt(String question, String persona) {
        String system = PERSONA_PROMPT.getOrDefault(persona, PERSONA_PROMPT.get("default"));
        return system + "\n\n用户问题：" + question + "\n\n请严格按上述格式回答：";
    }

    private String cleanAiResponse(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "抱歉，AI没有返回有效内容，请重试。";
        }
        text = text.replaceAll("[`•◆▶]+", "");
        long starCount = text.chars().filter(ch -> ch == '*').count();
        if (starCount % 2 != 0) {
            text = text.replace("**", "");
        }
        if (text.split("\\|").length > 6) {
            text = text.replace("|", "，");
        }
        text = text.replaceAll("\n{3,}", "\n\n");
        text = text.replaceAll("(?m)^\\s+|\\s+$", "");
        if (text.length() < 10 || text.split("\\|").length > 8) {
            return "【核心建议】\n建议先明确你的具体目标，这样我能给出更精准的建议。\n\n" +
                    "【行动清单】\n1. 补充你的背景信息（学校、专业、年级）\n" +
                    "2. 描述你目前的困惑\n" +
                    "3. 提出一个具体的、可回答的问题";
        }
        return text.trim();
    }
}
