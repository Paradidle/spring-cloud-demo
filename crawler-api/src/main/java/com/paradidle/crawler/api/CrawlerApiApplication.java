package com.paradidle.crawler.api;

import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import com.paradidle.crawler.api.service.BrowserSessionManager;


@SpringBootApplication
public class CrawlerApiApplication {

    @Autowired
    private BrowserSessionManager sessionManager;

    public static void main(String[] args) {
        SpringApplication.run(CrawlerApiApplication.class, args);
    }

    @PreDestroy
    public void cleanup() {
        System.out.println("应用关闭，正在清理所有浏览器会话...");
        sessionManager.closeAllSessions();
        System.out.println("所有浏览器会话已关闭");
    }
}
