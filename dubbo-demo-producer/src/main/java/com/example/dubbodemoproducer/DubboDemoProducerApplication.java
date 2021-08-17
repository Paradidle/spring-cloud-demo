package com.example.dubbodemoproducer;

import org.apache.dubbo.config.spring.context.annotation.DubboComponentScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@DubboComponentScan(basePackages = "com/example/dubbodemoproducer/serviceimpl")
@SpringBootApplication
public class DubboDemoProducerApplication {

    public static void main(String[] args) {
        SpringApplication.run(DubboDemoProducerApplication.class, args);
    }

}
