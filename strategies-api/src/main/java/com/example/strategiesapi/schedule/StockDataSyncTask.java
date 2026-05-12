//package com.example.strategiesapi.schedule;
//
//import com.example.strategiesapi.service.StockService;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Component;
//
///**
// * 股票数据定时更新任务
// * 每日收盘后自动更新行情数据
// */
//@Component
//@Slf4j
//@RequiredArgsConstructor
//public class StockDataSyncTask {
//
//    private final StockService stockService;
//
//    /**
//     * 每日收盘后自动更新行情数据
//     * 执行时间：每天 15:30（A股收盘后）
//     *
//     * Cron表达式说明：
//     * - 秒 分 时 日 月 周
//     * - 0 30 15 * * ? = 每天15:30:00执行
//     */
//    @Scheduled(cron = "0 30 15 * * ?")
//    public void updateDailyDataAfterClose() {
//        log.info("=== 定时任务：收盘后更新行情数据 ===");
//        try {
//            stockService.updateTodayData();
//            log.info("=== 定时任务完成 ===");
//        } catch (Exception e) {
//            log.error("收盘后更新行情数据失败", e);
//        }
//    }
//
//    /**
//     * 每周一至周五 09:05 自动更新开盘前数据
//     * 用于补充前一交易日的数据
//     */
//    @Scheduled(cron = "0 5 9 * * MON-FRI")
//    public void updatePreMarketData() {
//        log.info("=== 定时任务：开盘前补充数据 ===");
//        try {
//            stockService.updateTodayData();
//            log.info("=== 开盘前数据补充完成 ===");
//        } catch (Exception e) {
//            log.error("开盘前数据补充失败", e);
//        }
//    }
//
//    /**
//     * 每日凌晨 02:00 清理过期数据和执行数据校验
//     * 这个时间点系统负载最低
//     */
//    @Scheduled(cron = "0 0 2 * * ?")
//    public void dailyDataMaintenance() {
//        log.info("=== 定时任务：数据维护 ===");
//        try {
//            // 可以在这里添加数据清理、校验等维护操作
//            log.info("=== 数据维护完成 ===");
//        } catch (Exception e) {
//            log.error("数据维护失败", e);
//        }
//    }
//
//    /**
//     * 每日收盘后爬取行业概念涨幅前5数据
//     * 执行时间：每天 15:35（A股收盘后5分钟）
//     */
//    @Scheduled(cron = "0 35 15 * * ?")
//    public void fetchCategoryTop5AfterClose() {
//        log.info("=== 定时任务：爬取行业概念涨幅前5数据 ===");
//        try {
//            stockService.fetchCategoryTop5();
//            log.info("=== 行业概念涨幅前5数据爬取完成 ===");
//        } catch (Exception e) {
//            log.error("爬取行业概念涨幅前5数据失败", e);
//        }
//    }
//}
