package com.example.dubbodemoproducer.serviceimpl;

import com.example.dubbodemoapi.service.IOrderService;
import org.apache.dubbo.config.annotation.DubboService;

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
 * CreateDate:2021/8/18
 * </p>
 *
 * @author chenyupeng
 * @history Mender:chenyupeng；Date:2021/8/18；
 */
@DubboService
public class IOrderServiceImpl implements IOrderService {
    @Override
    public String sayHello() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return "Hello";
    }
}
