package com.example.springbootfasttest.services;

import java.util.Objects;
import javax.servlet.http.HttpServletRequest;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.example.springbootfasttest.result.JwtTokenResult;

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
public class TripleAuthService {

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private NonceService nonceService;

    // 时间窗口（5分钟）
    private static final long TIME_WINDOW = 5 * 60 * 1000;

    /**
     * 三要素验证
     */
    public AuthResult validateTripleAuth(HttpServletRequest request) {
        String token = getTokenFromRequest(request);

        AuthResult result = new AuthResult();

        // 2. 验证 JWT Token
        if (!jwtTokenProvider.validateToken(token)) {
            result.setSuccess(false);
            result.setErrorCode("INVALID_TOKEN");
            result.setMessage("Token无效或已过期");
            return result;
        }

        JwtTokenResult jwtTokenResult = jwtTokenProvider.getJwtTokenResultFromToken(token);
        if(Objects.isNull(jwtTokenResult)){
            result.setErrorCode("INVALID_TOKEN");
            result.setMessage("Token无效或已过期");
            return result;
        }

        String timestampStr = jwtTokenResult.getTimestamp().toString();
        String nonce = jwtTokenResult.getNonce();

        result.setUsername(jwtTokenResult.getUsername());

        // 3. 验证时间戳
        try {
            long timestamp = Long.parseLong(timestampStr);
            long currentTime = System.currentTimeMillis();

            // 允许5分钟的时间偏差
            if (Math.abs(currentTime - timestamp) > TIME_WINDOW) {
                result.setSuccess(false);
                result.setErrorCode("TIMESTAMP_EXPIRED");
                result.setMessage("请求时间戳已过期");
                return result;
            }
        } catch (NumberFormatException e) {
            result.setSuccess(false);
            result.setErrorCode("INVALID_TIMESTAMP");
            result.setMessage("时间戳格式错误");
            return result;
        }

        // 4. 验证 Nonce
        if (!nonceService.validateNonce(nonce)) {
            result.setSuccess(false);
            result.setErrorCode("DUPLICATE_REQUEST");
            result.setMessage("重放攻击");
            return result;
        }

        result.setSuccess(true);
        return result;
    }

    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    @Data
    public static class AuthResult {
        private boolean success;
        private String username;
        private String errorCode;
        private String message;
    }
}
