package com.example.springboot.actuator.extend.controller;

import com.example.springboot.actuator.extend.domain.QueryEntity;
import com.example.springboot.actuator.extend.repository.QueryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
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
 * CreateDate:2021/8/1
 * </p>
 *
 * @author chenyupeng
 * @history Mender:chenyupeng；Date:2021/8/1；
 */
@RestController
public class QueryController {
    @Autowired
    QueryRepository queryRepository;


    @RequestMapping("/query")
    public Object query(){
        return queryRepository.findAll(PageRequest.of(0,10));
    }

    @RequestMapping("/findOne")
    public Object findOne(){
        return queryRepository.findById("c9cec0b5-a238-4a47-80e6-10a070a1f773");
    }
}
