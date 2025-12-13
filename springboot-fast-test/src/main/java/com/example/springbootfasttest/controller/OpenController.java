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


    @RequestMapping("/receiveSubscriptionEvent")
    public String receiveSubscriptionEvent(String xmlData) {
        // 解析XML数据
        // 处理订阅事件
        // 返回处理结果
        return "success";
    }
}
