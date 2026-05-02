package com.paradidle.crawler.api;

import java.io.File;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import com.paradidle.crawler.api.service.XiaohongshuCrawlerService;

@SpringBootTest
public class QrCodeTest {

    @Autowired
    private XiaohongshuCrawlerService xiaohongshuCrawlerService;

    @Test
    public void testQrCodeCapture() throws InterruptedException {
        try {
            xiaohongshuCrawlerService.initDriver();
            xiaohongshuCrawlerService.navigateTo("https://www.xiaohongshu.com/explore");
            
            Thread.sleep(5000);
            
            File qrFile = xiaohongshuCrawlerService.captureQrCodeWithCustomLocator(
                driver -> driver.findElement(org.openqa.selenium.By.cssSelector("img.qrcode-img")),
                xiaohongshuCrawlerService.getModuleResourcePath("test_qr.png")
            );
            
            System.out.println("二维码文件路径: " + qrFile.getAbsolutePath());
            System.out.println("文件是否存在: " + qrFile.exists());
            System.out.println("文件大小: " + qrFile.length() + " bytes");
            
        } finally {
            xiaohongshuCrawlerService.closeDriver();
        }
    }
}
