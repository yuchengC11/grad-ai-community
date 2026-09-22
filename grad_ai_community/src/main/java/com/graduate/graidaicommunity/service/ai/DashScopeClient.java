package com.graduate.graidaicommunity.service.ai;

import com.graduate.graidaicommunity.common.AiException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Slf4j
@Component
public class DashScopeClient {

    // 本地实例ObjectMapper，工具类不依赖spring注入
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Value("${openai.api.key:}")
    private String apiKeyRaw;


    @Value("${openai.base.url:https://dashscope.aliyuncs.com/compatible-mode/v1}")
    private String baseUrl;


    // ✅ 免费模型 qwen3.7-plus
    @Value("${openai.model:qwen3.7-plus}")
    private String model;

    @Value("${openai.temperature:0.7}")
    private Double temperature;

    private static final MediaType JSON_MEDIA = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .callTimeout(90, TimeUnit.SECONDS)
            .build();

    private String getCleanKey() {
        if (apiKeyRaw == null || apiKeyRaw.trim().isEmpty()) {
            return "";
        }
        return apiKeyRaw.replaceAll("[^a-zA-Z0-9_\\-.]", "").trim();
    }

    private String getSystemPrompt(String persona) {
        String formatConstraint = "你是一位专业、严谨的毕业生互助社区AI顾问。回答时必须遵循以下格式：\n\n" +
                "【核心建议】\n用2-3句话给出最直接、可落地的核心观点。\n\n" +
                "【详细分析】\n分点说明，用\"1.\"、\"2.\"、\"3.\"编号。每一点包含：具体做法 + 实际例子。\n\n" +
                "【行动清单】\n列出3条今天就能执行的具体行动步骤。\n\n" +
                "【注意事项】\n提醒1-2个常见误区。\n\n" +
                "规则：\n" +
                "- 禁止使用\"|\"、未闭合的\"**\"、乱码符号拼接内容\n" +
                "- 禁止直接堆砌关键词，必须组织成通顺的中文段落\n" +
                "- 每段话必须语义完整、标点正确\n" +
                "- 总字数控制在400-600字\n\n";

        String roleStyle;
        if (persona == null || persona.isEmpty()) {
            roleStyle = "当前场景：通用问答。针对毕业生的学习、生活、职业发展给出建议。\n";
        } else {
            switch (persona) {
                case "employment":
                case "就业建议":
                    roleStyle = "当前场景：就业建议。你是资深就业指导专家，重点分析目标岗位、简历优化、面试准备、薪资谈判。语气专业务实。\n";
                    break;
                case "postgraduate":
                case "考研经验":
                    roleStyle = "当前场景：考研经验。你是考研辅导专家，重点分析院校选择、复习规划、专业课重点、心态调整。语气鼓励且有条理。\n";
                    break;
                case "civil":
                case "考公备考":
                    roleStyle = "当前场景：考公备考。你是公务员考试辅导专家，重点分析岗位筛选、行测技巧、申论写作、面试策略。语气严谨细致。\n";
                    break;
                case "job":
                case "求职感悟":
                    roleStyle = "当前场景：求职感悟。你是职场前辈，分享求职感悟、职场经验、行业洞察。语气真诚接地气。\n";
                    break;
                case "学长":
                    roleStyle = "当前场景：通用问答。你是热心亲切的学长，语言风格随和，多用鼓励语气，偶尔用口语化表达（如'我觉得'、'我当年'）。\n";
                    break;
                case "学姐":
                    roleStyle = "当前场景：通用问答。你是温柔细心的学姐，语言风格细腻体贴，善于发现优点，给出具体建议，语气温暖坚定。\n";
                    break;
                case "面试官":
                    roleStyle = "当前场景：通用问答。你是资深HR/面试官，语言风格专业简洁、逻辑清晰，善于追问和引导，注重岗位匹配度。\n";
                    break;
                case "导师":
                    roleStyle = "当前场景：通用问答。你是经验丰富的职业导师，语言风格沉稳有深度，善于启发式提问，注重长期发展。\n";
                    break;
                default:
                    roleStyle = "当前场景：通用问答。针对毕业生的学习、生活、职业发展给出建议。\n";
                    break;
            }
        }
        return formatConstraint + roleStyle + "请严格按上述格式回答用户问题：";
    }

    @CircuitBreaker(name = "dashscope", fallbackMethod = "chatCompletionFallback")
    @RateLimiter(name = "dashscope", fallbackMethod = "chatCompletionFallback")
    public String chatCompletion(String userText,
                                 List<Map<String, String>> history,
                                 String persona,
                                 Consumer<String> onChunk) {
        String cleanKey = getCleanKey();
        if (cleanKey.isEmpty()) {
            throw new AiException("未配置dashscope.api.key");
        }

        boolean isStream = (onChunk != null);

        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("temperature", temperature);
        body.put("max_tokens", 1500);
        if (isStream) {
            body.put("stream", true);
        }

        List<Map<String, String>> messages = new ArrayList<>();

        String systemPrompt = getSystemPrompt(persona);
        Map<String, String> sysMsg = new HashMap<>();
        sysMsg.put("role", "system");
        sysMsg.put("content", systemPrompt);
        messages.add(sysMsg);

        if (history != null && !history.isEmpty()) {
            for (Map<String, String> msg : history) {
                String role = msg.get("role");
                String content = msg.get("content");
                if (role == null || content == null || role.isEmpty() || content.isEmpty()) {
                    continue;
                }
                if ("ai".equals(role)) {
                    role = "assistant";
                }
                if (!"user".equals(role) && !"assistant".equals(role)) {
                    continue;
                }
                Map<String, String> m = new HashMap<>();
                m.put("role", role);
                m.put("content", content);
                messages.add(m);
            }
        }

        Map<String, String> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", userText);
        messages.add(userMsg);

        body.put("messages", messages);

        log.info("【AI请求】model={}, persona={}, stream={}, messages数={}",
                model, persona, isStream, messages.size());

        String jsonBody;
        try {
            // Jackson替代 JSON.toJSONString(body)
            jsonBody = OBJECT_MAPPER.writeValueAsString(body);
        } catch (Exception e) {
            throw new AiException("组装AI请求JSON失败", e);
        }
        String fullUrl = baseUrl + "/chat/completions";
        log.info("【完整请求地址】={}", fullUrl);
        RequestBody requestBody = RequestBody.create(jsonBody, JSON_MEDIA);
        Request request = new Request.Builder()
                .url(fullUrl)
                .header("Authorization", "Bearer " + cleanKey)
                .post(requestBody)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (response.code() == 429) {
                throw new AiException("AI接口限流，请稍后再试");
            }
            if (!response.isSuccessful()) {
                String errBody = response.body() != null ? response.body().string() : "空";
                log.error("AI接口错误，状态码={}, 响应={}", response.code(), errBody);
                throw new AiException("AI接口调用失败，状态码：" + response.code());
            }

            if (isStream) {
                return handleStream(response, onChunk);
            } else {
                return handleNonStream(response);
            }
        } catch (IOException e) {
            throw new AiException("网络异常，无法连接AI服务", e);
        }
    }

    public String chatCompletionFallback(String userText,
                                         List<Map<String, String>> history,
                                         String persona,
                                         Consumer<String> onChunk,
                                         Throwable t) {
        log.warn("DashScope 触发降级，原因：{}", t.getMessage());
        if (t instanceof AiException) {
            throw (AiException) t;
        }
        throw new AiException("AI服务繁忙，请稍后再试");
    }

    private String handleNonStream(Response response) throws IOException {
        String respJson = response.body().string();
        try {
            // Jackson解析为Map，替代JSON.parseObject
            Map<String, Object> respMap = OBJECT_MAPPER.readValue(respJson, new TypeReference<Map<String, Object>>() {});
            List<Map<String, Object>> choices = (List<Map<String, Object>>) respMap.get("choices");
            if (choices == null || choices.isEmpty()) {
                throw new AiException("AI接口返回异常：choices为空");
            }
            Map<String, Object> choice = choices.get(0);
            Map<String, String> message = (Map<String, String>) choice.get("message");
            if (message == null || message.get("content") == null) {
                throw new AiException("AI接口返回异常：message为空");
            }
            String content = message.get("content").trim();
            log.info("【AI响应】长度={}", content.length());
            return cleanResponse(content);
        } catch (AiException e) {
            throw e;
        } catch (Exception e) {
            throw new AiException("AI接口响应解析失败", e);
        }
    }

    private String handleStream(Response response, Consumer<String> onChunk) throws IOException {
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(response.body().byteStream(), "UTF-8")
        );
        String line;
        StringBuilder fullContent = new StringBuilder();

        while ((line = reader.readLine()) != null) {
            if (line.startsWith("data: ")) {
                String data = line.substring(6).trim();
                if ("[DONE]".equals(data)) {
                    break;
                }
                if (data.isEmpty()) {
                    continue;
                }
                try {
                    // Jackson解析流式块，替代JSON.parseObject(data)
                    Map<String, Object> json = OBJECT_MAPPER.readValue(data, new TypeReference<Map<String, Object>>() {});
                    List<Map<String, Object>> choices = (List<Map<String, Object>>) json.get("choices");
                    if (choices != null && !choices.isEmpty()) {
                        Map<String, Object> delta = (Map<String, Object>) choices.get(0).get("delta");
                        if (delta != null) {
                            String content = (String) delta.get("content");
                            if (content != null) {
                                fullContent.append(content);
                                onChunk.accept(content);
                            }
                        }
                    }
                } catch (Exception e) {
                    log.warn("解析AI流式响应失败，忽略该行: {}", data);
                }
            }
        }
        String result = fullContent.toString();
        log.info("【AI流式响应完成】总长度={}", result.length());
        return cleanResponse(result);
    }

    private String cleanResponse(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "抱歉，AI没有返回有效内容，请重试。";
        }
        text = text.replaceAll("[`•◆▶]+", "");
        if (text.split("\\|").length > 6) {
            text = text.replace("|", "，");
        }
        text = text.replaceAll("\n{3,}", "\n\n");
        text = text.replaceAll("(?m)^\\s+|\\s+$", "");
        if (text.length() < 20 || text.split("\\|").length > 8) {
            return "【核心建议】\n建议先明确你的具体目标，这样我能给出更精准的建议。\n\n" +
                    "【行动清单】\n1. 补充你的背景信息（学校、专业、年级）\n" +
                    "2. 描述你目前的困惑\n" +
                    "3. 提出一个具体的、可回答的问题";
        }
        return text.trim();
    }
}
