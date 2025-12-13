package com.example.springbootfasttest.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 *
 * </p>
 *
 * <p>
 * CreateDate:2025/12/13
 * </p>
 *
 * @author chenyupeng
 * @history Mender:chenyupeng；Date:2025/12/13；
 */
@RestController
@RequestMapping("/open")
public class OpenController {

    private final static String APP_ID = "wx4963ced332c06b33";

    private final static String APP_SECRET = "2c4029e8ec7959614361d006943b7e5f";

    private final static String TOKEN = "test";
    private final static String ENCODING_AES_KEY = "6RYGllQyIbtMgxVWEdqpR5xPbdMgo7bUhFMTYewJkZk";

    @RequestMapping("/receiveSubscriptionEvent")
    public String receiveSubscriptionEvent(String signature, String nonce, Long timestamp, String echostr) {
        // signature=e417866efed469f22747dc0e2ee70f20f33ec180, nonce=1096287447, echostr=130213875726254039, timestamp=1765620611
        // 解析XML数据
        // 处理订阅事件
        // 返回处理结果
        return echostr;
    }
}
