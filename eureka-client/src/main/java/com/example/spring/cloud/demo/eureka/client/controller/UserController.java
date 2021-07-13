package com.example.spring.cloud.demo.eureka.client.controller;

import com.example.spring.cloud.demo.eureka.client.domain.User;
import com.example.spring.cloud.demo.eureka.client.service.GetListRequest;
import com.example.spring.cloud.demo.eureka.client.service.UserFeignClient;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixProperty;
import com.netflix.ribbon.proxy.annotation.Hystrix;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.circuitbreaker.EnableCircuitBreaker;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.core.env.Environment;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * <p>
 *
 * </p>
 *
 * <p>
 * Copyright: 2020 . All rights reserved.
 * </p>
 * <p>
 * Company: Zsoft
 * </p>
 * <p>
 * CreateDate:2020/1/25
 * </p>
 *
 * @author chenyupeng
 * @history Mender:chenyupeng；Date:2020/1/25；
 */
@RestController
@RequestMapping("/user")
@RefreshScope
@EnableCircuitBreaker
public class UserController {
    @Autowired
    UserFeignClient userFeignClient;
    @Value("${eureka.client.config}")
    private String defaultZone;

    @RequestMapping("/list")
    @HystrixCommand(commandProperties = {
            @HystrixProperty(name ="execution.isolation.thread.timeoutInMilliseconds",value = "5000")
    },fallbackMethod = "fallBack")
    public String findUser(){
        System.out.println("config"+defaultZone);
        ResponseEntity<String> result = userFeignClient.getUserList(new GetListRequest());
        System.out.println(result.getBody());
        return result.getBody();
    }

    public String fallBack(){
        return "啊不行了";
    }

    @RequestMapping("/config")
    public String getConfig(){
        return defaultZone;
    }
}
