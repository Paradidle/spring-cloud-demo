package com.example.dubbodemoconsumer.controller;

import com.example.dubbodemoapi.service.IOrderService;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
@RestController
public class OrderServiceController {
    @DubboReference
    IOrderService iOrderService;

    @RequestMapping("/test")
    public String test(){
        return iOrderService.sayHello();
    }

}
