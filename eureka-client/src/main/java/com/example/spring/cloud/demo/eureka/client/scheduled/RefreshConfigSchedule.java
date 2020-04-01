package com.example.spring.cloud.demo.eureka.client.scheduled;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.refresh.ContextRefresher;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Set;

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
 * CreateDate:2020/3/30
 * </p>
 *
 * @author chenyupeng
 * istory Mender:chenyupeng；Date:2020/3/30；
 */
@EnableScheduling
@Component
public class RefreshConfigSchedule {
    @Autowired
    private final ContextRefresher contextRefresher;

    public RefreshConfigSchedule(ContextRefresher contextRefresher) {
        this.contextRefresher = contextRefresher;
    }

    @Scheduled(fixedRate = 60 * 1000,initialDelay = 5 * 1000)
    public void autoRefreshConfig(){
        Set<String> params = this.contextRefresher.refresh();
        if(!CollectionUtils.isEmpty(params)) {
            System.out.printf("当前刷新属性：%s", params);
        }
    }
}
