package com.graduate.graidaicommunity.interceptor;

import com.graduate.graidaicommunity.common.Result;
import com.graduate.graidaicommunity.common.UserContext;
import com.graduate.graidaicommunity.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;

@Slf4j
@Component
public class JwtInterceptor implements HandlerInterceptor {
    @Resource
    private JwtUtil jwtUtil;
    @Resource
    private ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        UserContext.clear(); // 防御性清理，防止线程复用串号

        if ("OPTIONS".equals(request.getMethod())) {
            return true;
        }

        String token = request.getHeader("Authorization");
        if (token == null || token.trim().isEmpty()) {
            return writeError(response, 401, "未登录，请先登录");
        }
        // 兼容标准写法：Authorization: Bearer <token>，解析前去掉 "Bearer " 前缀
        // （原代码把含前缀的完整值直接传给 parseToken，导致 jjwt 解析失败返回 401）
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        try {
            Long userId = jwtUtil.parseToken(token);
            UserContext.setUserId(userId);
        } catch (Exception e) {
            log.warn("token校验失败", e);
            return writeError(response, 401, "登录已失效，请重新登录");
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        UserContext.clear();
    }

    private boolean writeError(HttpServletResponse response, int code, String msg) {
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter writer = null;
        try {
            writer = response.getWriter();
            String json = objectMapper.writeValueAsString(Result.fail(code, msg));
            writer.write(json);
            writer.flush();
        } catch (Exception e) {
            log.error("拦截器输出响应异常", e);
        }
        // ❌ 不要关闭 response.getWriter()，由 Servlet 容器管理
        return false;
    }
}
