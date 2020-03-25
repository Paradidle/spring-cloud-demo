package com.example.eureka.service.provider.controller;

import com.example.eureka.service.provider.domain.User;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
@RequestMapping("/provider/user")
public class UserProviderController {
    @RequestMapping("/list")
    public List<User> providerUserList() {
        List<User> result = new ArrayList<>();
        result.add(new User("test", "age", UUID.randomUUID().toString()));
        return result;
    }
}
