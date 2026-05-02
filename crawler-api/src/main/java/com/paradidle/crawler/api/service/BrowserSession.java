package com.paradidle.crawler.api.service;

import java.time.Duration;
import java.util.UUID;
import lombok.Data;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;

@Data
public class BrowserSession {
    private String sessionId;
    private WebDriver driver;
    private WebDriverWait wait;
    private long createTime;
    private String platform;

    public BrowserSession(String platform, WebDriver driver) {
        this.sessionId = UUID.randomUUID().toString();
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(300));
        this.createTime = System.currentTimeMillis();
        this.platform = platform;
    }

    public boolean isExpired(long maxAgeMs) {
        return System.currentTimeMillis() - createTime > maxAgeMs;
    }
}
