package com.example.strategiesapi.schedule;

import com.example.strategiesapi.service.StockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 财经新闻定时爬取任务
 * 每5分钟爬取一次财联社加红栏目新闻
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class NewsFetchTask {

    private final StockService stockService;

    /**
     * 每5分钟爬取一次财联社新闻
     * 执行时间：每5分钟执行一次
     * 
     * fixedRateString 从配置文件读取，支持动态调整
     */
    @Scheduled(fixedRate = 300000) // 5分钟 = 300000毫秒
    public void fetchClsNews() {
        log.info("=== 定时任务：爬取财联社新闻 ===");
        try {
            stockService.fetchClsNews();
            log.info("=== 财联社新闻爬取完成 ===");
        } catch (Exception e) {
            log.error("爬取财联社新闻失败", e);
        }
    }
}
