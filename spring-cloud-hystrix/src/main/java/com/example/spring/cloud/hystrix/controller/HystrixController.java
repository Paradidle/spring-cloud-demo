package com.example.spring.cloud.hystrix.controller;

import com.example.spring.cloud.hystrix.annotation.IHystrixCommand;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.web.annotation.RestControllerEndpoint;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.lang.annotation.Retention;
import java.util.*;

/**
 * <p>
 *
 * </p>
 *
 * <p>
 * Copyright: 2021 . All rights reserved.
 * </p>
 * <p>
 * Company: Zsoft
 * </p>
 * <p>
 * CreateDate:2021/7/18
 * </p>
 *
 * @author chenyupeng
 * @history Mender:chenyupeng；Date:2021/7/18；
 */
@RestController
public class HystrixController {

    @Autowired
    RestTemplate restTemplate;

    @RequestMapping("/test")
    @IHystrixCommand(fallBack = "testFallBack",timeOut = 1000)
    public String test(){
        restTemplate.getForEntity("http://localhost:8082/user/list",String.class);
        return "服务请求成功";
    }

    public String testFallBack(){
        return "服务熔断";
    }

}
