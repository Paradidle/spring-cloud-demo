package com.example.spring.cloud.demo.eureka.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.context.refresh.ContextRefresher;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.CollectionUtils;

import java.util.Set;


@SpringBootApplication
@EnableEurekaClient
@EnableFeignClients("com.example.spring.cloud.demo.eureka.client.service")
public class EurekaClientApplication {
    private final ContextRefresher contextRefresher;

    @Autowired
    public EurekaClientApplication(ContextRefresher contextRefresher) {
        this.contextRefresher = contextRefresher;
    }

    public static void main(String[] args) {
        SpringApplication.run(EurekaClientApplication.class, args);
    }

    @Scheduled(fixedRate = 5 * 1000,initialDelay = 3 * 1000)
    public void autoRefreshConfig(){
        Set<String> params = this.contextRefresher.refresh();
        if(!CollectionUtils.isEmpty(params)) {
            System.out.printf("当前刷新属性：%s", params);
        }
    }
}
