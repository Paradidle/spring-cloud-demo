package com.example.springboot.actuator.extend.listener;

import org.hibernate.HibernateException;
import org.hibernate.event.spi.*;
import org.springframework.stereotype.Component;

import java.util.Map;
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
@Component
public class HibernateQueryListener implements PreLoadEventListener, PostLoadEventListener {

    private static final Map<String,Object> slowSqlMap = new ConcurrentHashMap<>();

    public Map<String,Object> getSlowSqlMap(){
        return slowSqlMap;
    }

    @Override
    public void onPostLoad(PostLoadEvent event) {
        int hashCode = event.getSession().hashCode();
        slowSqlMap.put(hashCode+"-end",System.currentTimeMillis());
    }

    @Override
    public void onPreLoad(PreLoadEvent event) {
        int hashCode = event.getSession().hashCode();
        slowSqlMap.put(hashCode+"-start",System.currentTimeMillis());
    }

}
