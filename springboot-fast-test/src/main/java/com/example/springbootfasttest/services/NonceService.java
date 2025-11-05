package com.example.springbootfasttest.services;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;

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
public class NonceService {

    private final Map<String, Boolean> usedNonces = new HashMap<>();

    /**
     * 验证 Nonce 是否有效
     */
    public boolean validateNonce(String nonce) {
        if (nonce == null || nonce.length() < 8) {
            return false;
        }

        // 检查 Nonce 是否已使用
        if (usedNonces.get(nonce) != null) {
            return false;  // 已使用，拒绝请求
        }

        // 记录已使用的 Nonce
        usedNonces.put(nonce, true);
        return true;
    }

    /**
     * 生成带时间戳的 Nonce 键
     */
    public String generateTimeNonceKey(String timestamp, String nonce) {
        return timestamp + ":" + nonce;
    }


    /**
     * 批量验证多个 Nonce（可选）
     */
    public Map<String, Boolean> validateNonces(List<String> nonces) {
        Map<String, Boolean> results = new HashMap<>();
        for (String nonce : nonces) {
            results.put(nonce, validateNonce(nonce));
        }
        return results;
    }
}
