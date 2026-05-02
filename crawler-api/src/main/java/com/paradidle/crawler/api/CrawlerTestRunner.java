package com.paradidle.crawler.api;

import java.io.File;
import org.openqa.selenium.By;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import com.paradidle.crawler.api.service.XiaohongshuCrawlerService;

@Component
public class CrawlerTestRunner implements CommandLineRunner {

    @Autowired
    private XiaohongshuCrawlerService xiaohongshuCrawlerService;

    @Value("${app.run-test-on-startup:false}")
    private boolean runTestOnStartup;

    @Override
    public void run(String... args) throws Exception {
        if (!runTestOnStartup) {
            System.out.println("测试已禁用，设置 app.run-test-on-startup=true 启用");
            return;
        }
        
        System.out.println("开始测试小红书二维码登录...");
        
        try {
            xiaohongshuCrawlerService.initDriver();
            xiaohongshuCrawlerService.navigateTo("https://www.xiaohongshu.com/explore");
            
            Thread.sleep(5000);
            
            File qrFile = xiaohongshuCrawlerService.captureQrCodeWithCustomLocator(
                driver -> driver.findElement(By.cssSelector("img.qrcode-img")),
                xiaohongshuCrawlerService.getModuleResourcePath("xiaohongshu_test.png")
            );
            
            System.out.println("===========================================");
            System.out.println("二维码已保存！");
            System.out.println("文件路径: " + qrFile.getAbsolutePath());
            System.out.println("文件存在: " + qrFile.exists());
            System.out.println("文件大小: " + qrFile.length() + " bytes");
            System.out.println("===========================================");
            
            Thread.sleep(10000);
            
        } catch (Exception e) {
            System.err.println("测试失败: " + e.getMessage());
            e.printStackTrace();
        } finally {
            xiaohongshuCrawlerService.closeDriver();
            System.exit(0);
        }
    }
}
