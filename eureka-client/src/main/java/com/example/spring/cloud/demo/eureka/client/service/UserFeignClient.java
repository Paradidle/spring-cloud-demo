package com.example.spring.cloud.demo.eureka.client.service;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

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
 * CreateDate:2020/3/4
 * </p>
 *
 * @author chenyupeng
 * @history Mender:chenyupeng；Date:2020/3/4；
 */
@FeignClient(value = "service-provider")
public interface UserFeignClient {
    @RequestMapping("/provider/user/list")
    ResponseEntity<String> getUserList(@RequestBody GetListRequest request);
}
