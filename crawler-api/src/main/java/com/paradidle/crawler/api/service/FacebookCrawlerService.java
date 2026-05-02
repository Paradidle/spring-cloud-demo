package com.paradidle.crawler.api.service;

import java.io.File;
import org.openqa.selenium.By;
import org.springframework.stereotype.Service;

@Service
public class FacebookCrawlerService extends BaseCrawlerService {

    private static final String BASE_URL = "https://www.facebook.com";
    private static final String LOGIN_URL = "https://www.facebook.com/login";
    private static final String PLATFORM = "facebook";

    public String loginWithQrCode(BrowserSessionManager sessionManager) {
        BrowserSession session = sessionManager.createSession(PLATFORM);
        this.driver = session.getDriver();
        this.wait = session.getWait();
        
        try {
            navigateTo(LOGIN_URL);
            Thread.sleep(3000);
            
            clickElement(By.cssSelector("button[aria-label*='QR']"), "QR code button");
            Thread.sleep(2000);
            
            File qrFile = captureQrCodeWithCustomLocator(
                driver -> {
                    try {
                        return driver.findElement(By.cssSelector("canvas[data-testid='qr-code']"));
                    } catch (Exception e) {
                        return driver.findElement(By.xpath("//img[contains(@src, 'qr') or contains(@alt, 'QR')]"));
                    }
                },
                getOutputPath()
            );
            
            System.out.println("请扫描二维码登录Facebook，会话ID: " + session.getSessionId());
            
            return session.getSessionId();
        } catch (Exception e) {
            sessionManager.closeSession(session.getSessionId());
            throw new RuntimeException("Facebook登录失败: " + e.getMessage(), e);
        }
    }

    private void waitForLoginSuccess() {
        try {
            wait.until(driver -> {
                String currentUrl = driver.getCurrentUrl();
                return !currentUrl.contains("login");
            });
            System.out.println("登录成功！");
        } catch (Exception e) {
            System.out.println("等待登录超时，请手动检查");
        }
    }

    private String getOutputPath() {
        return getModuleResourcePath("facebook_qr.png");
    }
}
