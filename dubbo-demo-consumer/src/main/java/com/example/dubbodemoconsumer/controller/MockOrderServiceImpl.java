package com.example.dubbodemoconsumer.controller;

import com.example.dubbodemoapi.service.IOrderService;

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
 * CreateDate:2021/8/24
 * </p>
 *
 * @author chenyupeng
 * @history Mender:chenyupeng；Date:2021/8/24；
 */
public class MockOrderServiceImpl implements IOrderService {

    @Override
    public String sayHello() {
        return "服务降级";
    }
}
