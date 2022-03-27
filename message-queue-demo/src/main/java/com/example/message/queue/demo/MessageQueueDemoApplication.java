package com.example.message.queue.demo;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.internals.Sender;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class MessageQueueDemoApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(MessageQueueDemoApplication.class, args);
        KafkaProducer sender = context.getBean(KafkaProducer.class);
        for (int i = 0; i < 1000; i++) {
            //调用消息发送类中的消息发送方法
            sender.send(new ProducerRecord("test1",i+""));
            System.out.println("生产消息"+i);
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
