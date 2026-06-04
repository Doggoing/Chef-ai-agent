package com.wu.aiagent.auth;

import com.wu.aiagent.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * JWT 登录拦截器（支持 Header 与 query token，便于 SSE）。
 */
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    public static final String ATTR_USER_ID = "userId";

    public static final String ATTR_USERNAME = "username";

    private final JwtUtil jwtUtil;

    @Value("${auth.enabled:true}")
    private boolean authEnabled;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 浏览器跨域预检不带 token，必须放行（堆栈里 doOptions 即此类请求）
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        if (!authEnabled) {
            request.setAttribute(ATTR_USER_ID, 0L);
            request.setAttribute(ATTR_USERNAME, "test-user");
            return true;
        }
        String token = resolveToken(request);
        if (token == null) {
            throw new BusinessException(401, "请先登录");
        }
        try {
            Long userId = jwtUtil.getUserId(token);
            request.setAttribute(ATTR_USER_ID, userId);
            request.setAttribute(ATTR_USERNAME, jwtUtil.parseToken(token).get("username", String.class));
            return true;
        } catch (Exception ex) {
            throw new BusinessException(401, "登录已过期，请重新登录");
        }
    }

    private String resolveToken(HttpServletRequest request) {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }
        String queryToken = request.getParameter("token");
        if (queryToken != null && !queryToken.isBlank()) {
            return queryToken;
        }
        return null;
    }
}
