package com.example.message.queue.demo.service;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
 * CreateDate:2021/6/19
 * </p>
 *
 * @author chenyupeng
 * @history Mender:chenyupeng；Date:2021/6/19；
 */
@Configuration
public class KafkaConfig {
    @Autowired
    KafkaProperties kafkaProperties;

    @Bean
    public Producer<Integer, Object> getKafkaProducer() {
        //KafkaProducer是线程安全的，可以在多个线程中共享单个实例
        return new KafkaProducer<Integer, Object>(kafkaProperties.buildProducerProperties());
    }
}
