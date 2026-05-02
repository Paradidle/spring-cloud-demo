package com.paradidle.crawler.api;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import com.paradidle.crawler.api.service.BrowserSessionManager;
import com.paradidle.crawler.api.service.XiaohongshuCrawlerService;

@SpringBootTest
public class XiaohongshuCrawlerTest {

    @Autowired
    private XiaohongshuCrawlerService xiaohongshuCrawlerService;

    @Autowired
    private BrowserSessionManager sessionManager;

    @Test
    public void testQrCodeLogin() {
        String sessionId = xiaohongshuCrawlerService.loginWithQrCode(sessionManager);
        
        System.out.println("会话ID: " + sessionId);
        System.out.println("请扫描二维码登录");
    }

    @AfterEach
    public void tearDown() {
        sessionManager.closeAllSessions();
    }
}
