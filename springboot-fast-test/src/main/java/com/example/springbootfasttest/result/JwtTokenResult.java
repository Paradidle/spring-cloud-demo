package com.example.springbootfasttest.result;

import lombok.Data;
import lombok.experimental.Accessors;

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
@Data
@Accessors(chain = true)
public class JwtTokenResult {

    private String username;
    private String nonce;
    private Long timestamp;
}
