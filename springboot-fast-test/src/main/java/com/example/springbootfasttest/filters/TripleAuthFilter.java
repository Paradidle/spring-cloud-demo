package com.example.springbootfasttest.filters;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import com.example.springbootfasttest.services.TripleAuthService;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * <p>
 *
 * </p>
 *
 * <p>
 * Copyright: 2025 . All rights reserved.
 * </p>
 * <p>
 * Company: Zsoft
 * </p>
 * <p>
 * CreateDate:2025/11/5
 * </p>
 *
 * @author chenyupeng
 * @history Mender:chenyupeng；Date:2025/11/5；
 */
@Component
public class TripleAuthFilter extends OncePerRequestFilter {

    @Autowired
    private TripleAuthService tripleAuthService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // 开放接口直接放行
        if (isOpenPath(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 执行三要素认证
        TripleAuthService.AuthResult authResult = tripleAuthService.validateTripleAuth(request);

        if (!authResult.isSuccess()) {
            sendAuthErrorResponse(response, authResult);
            return;
        }

        // 认证成功，设置 Spring Security 上下文
        if (authResult.getUsername() != null &&
                SecurityContextHolder.getContext().getAuthentication() == null) {

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(authResult.getUsername(), null, null);
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }

    private boolean isOpenPath(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.startsWith("/open/") ||
                path.startsWith("/auth/login") ||
                path.startsWith("/auth/register");
    }

    private void sendAuthErrorResponse(HttpServletResponse response,
                                       TripleAuthService.AuthResult authResult) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");

        Map<String, Object> error = new HashMap<>();
        error.put("code", 401);
        error.put("message", authResult.getMessage());
        error.put("errorCode", authResult.getErrorCode());
        error.put("timestamp", System.currentTimeMillis());

        response.getWriter().write(new ObjectMapper().writeValueAsString(error));
    }
}
