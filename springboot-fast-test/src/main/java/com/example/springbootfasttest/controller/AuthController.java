package com.example.springbootfasttest.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.example.springbootfasttest.result.JwtTokenResult;
import com.example.springbootfasttest.services.JwtTokenProvider;
import com.example.springbootfasttest.services.NonceService;

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
@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private NonceService nonceService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        try {
            // 使用Spring Security的认证管理器进行认证
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()
                    )
            );

            // 认证成功，生成token
            String token = jwtTokenProvider.generateToken(loginRequest.getUsername());

            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "登录成功");

            Map<String,Object> dataMap = new HashMap<>();
            dataMap.put("token", token);
            dataMap.put("username", loginRequest.getUsername());
            dataMap.put("expiresIn", jwtTokenProvider.getJwtExpiration() / 1000);

            response.put("data", dataMap);

            return ResponseEntity.ok(response);

        } catch (AuthenticationException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("code", 401);
            response.put("message", "用户名或密码错误");
            return ResponseEntity.status(401).body(response);
        }
    }

    // 添加一个token验证接口
    @PostMapping("/verify")
    public ResponseEntity<?> verifyToken(@RequestHeader("Authorization") String authHeader) {

        Map<String,Object> dataMap = new HashMap<>();

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            dataMap.put("valid", false);
            dataMap.put("message", "Token格式错误");
            return ResponseEntity.badRequest().body(dataMap);
        }

        String token = authHeader.substring(7);
        boolean isValid = jwtTokenProvider.validateToken(token);

        if (isValid) {

            JwtTokenResult jwtTokenResult = jwtTokenProvider.getJwtTokenResultFromToken(token);
            if(Objects.isNull(jwtTokenResult)){
                dataMap.put("valid", false);
                dataMap.put("message", "Token无效或已过期");
                return ResponseEntity.ok(dataMap);
            }

            dataMap.put("valid", true);
            dataMap.put("username", jwtTokenResult.getUsername());
            return ResponseEntity.ok(dataMap);
        }

        dataMap.put("valid", false);
        dataMap.put("message", "Token无效或已过期");
        return ResponseEntity.ok(dataMap);
    }

    /**
     * 获取可用的 Nonce（前端可以选择使用）
     */
    @GetMapping("/nonce")
    public ResponseEntity<?> getNonce() {
        // 生成一个随机 Nonce
        String nonce = generateNonce();

        Map<String,Object> dataMap = new HashMap<>();
        dataMap.put("nonce", nonce);
        dataMap.put("expiresIn", 300);

        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("data", dataMap);

        return ResponseEntity.ok(response);
    }

    private String generateNonce() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    @Data
    public static class LoginRequest {
        private String username;
        private String password;
    }
}
