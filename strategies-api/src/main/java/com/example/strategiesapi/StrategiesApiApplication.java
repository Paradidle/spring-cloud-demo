package com.example.strategiesapi;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.example.strategiesapi.mapper")
@EnableScheduling
public class StrategiesApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(StrategiesApiApplication.class, args);
    }

}
