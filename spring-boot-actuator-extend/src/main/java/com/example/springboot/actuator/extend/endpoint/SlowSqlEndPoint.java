package com.example.springboot.actuator.extend.endpoint;

import com.example.springboot.actuator.extend.listener.HibernateQueryListener;
import org.hibernate.HibernateException;
import org.hibernate.event.spi.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

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
 * CreateDate:2021/8/1
 * </p>
 *
 * @author chenyupeng
 * @history Mender:chenyupeng；Date:2021/8/1；
 */
@Endpoint(id = "slow-sql")
@Component
public class SlowSqlEndPoint {

    @Autowired
    HibernateQueryListener hibernateQueryListener;

    @Value("${slow.sql.time.limit:0}")
    private long slowSqlTimeLimit;

    @ReadOperation
    public Map<String,Object> getSlowSql(){
        return hibernateQueryListener.getSlowSqlMap();
    }


}
