package com.example.strategiesapi.controller;

import java.util.List;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.example.strategiesapi.model.StockInfo;
import com.example.strategiesapi.service.IStockScheduleService;
import com.example.strategiesapi.service.PythonSkillExecutor;
import com.example.strategiesapi.service.StockService;

@RestController
@RequestMapping("/api/strategies")
@RequiredArgsConstructor
public class StrategiesController {

    private final PythonSkillExecutor pythonSkillExecutor;
    private final StockService stockService;
    private final IStockScheduleService stockScheduleService;

    @PostMapping("/execute")
    public String executeSkill(@RequestBody SkillRequest request) {
        return pythonSkillExecutor.executeSkill(request.getSkillName(), request.getPrompt());
    }

    // 获取股票列表
    @GetMapping("/stocks")
    public List<String> getStockList() {
        return stockService.getStockList();
    }

    // 获取单只股票详情
    @GetMapping("/stock/{code}")
    public StockInfo getStockDetail(@PathVariable String code) {
        return stockService.getStockDetail(code);
    }

    // 获取指定日期的股票详情
    @GetMapping("/stock/{code}/{date}")
    public StockInfo getStockDetailByDate(@PathVariable String code, @PathVariable String date) {
        return stockService.getStockDetailByDate(code, date);
    }

    // 初始化历史数据
    @PostMapping("/init-history")
    public String initHistoricalData() {
        new Thread(() -> stockService.initHistoricalData()).start();
        return "数据初始化任务已启动，请查看日志了解进度";
    }

    // 手动触发分时数据拉取
    @PostMapping("/fetch-minute-data")
    public String fetchMinuteData() {
        new Thread(() -> stockScheduleService.fetchMinuteData()).start();
        return "分时数据拉取任务已启动，请查看日志了解进度";
    }
    
    // 快速初始化股票基本信息（只插入基本信息，不获取历史数据）
    @PostMapping("/init-basic")
    public String initBasicData() {
        new Thread(() -> stockService.initBasicDataOnly()).start();
        return "股票基本信息初始化任务已启动，请查看日志了解进度";
    }
    
    // 获取股票基本信息数量
    @GetMapping("/basic-count")
    public String getBasicCount() {
        return stockService.getBasicCount();
    }

    // 初始化5分钟K线数据（从新浪API获取近一年数据）
    @PostMapping("/init-minute-kline")
    public String initMinuteKlineData() {
        new Thread(() -> stockService.initMinuteKlineData()).start();
        return "5分钟K线数据初始化任务已启动，请查看日志了解进度";
    }

    // 获取日线数据数量
    @GetMapping("/daily-count")
    public String getDailyCount() {
        return stockService.getDailyCount();
    }

    // 初始化大盘指数基本信息
    @PostMapping("/init-index-basic")
    public String initIndexBasic() {
        new Thread(() -> stockService.initIndexBasic()).start();
        return "大盘指数基本信息初始化任务已启动";
    }

    // 初始化大盘指数日线数据
    @PostMapping("/init-index-daily")
    public String initIndexDaily() {
        new Thread(() -> stockService.initIndexDaily()).start();
        return "大盘指数日线数据初始化任务已启动";
    }

    // 更新今日行情数据
    @PostMapping("/update-today")
    public String updateTodayData() {
        new Thread(() -> stockService.updateTodayData()).start();
        return "今日行情数据更新任务已启动";
    }

    // 补充近几天缺失的行情数据
    @PostMapping("/fill-recent")
    public String fillRecentData(@RequestParam(defaultValue = "5") int days) {
        final int daysToFill = days;
        new Thread(() -> stockService.fillRecentData(daysToFill)).start();
        return String.format("补充近 %d 天行情数据任务已启动", days);
    }

    // 爬取财联社新闻
    @PostMapping("/fetch-news")
    public String fetchClsNews() {
        new Thread(() -> stockService.fetchClsNews()).start();
        return "财联社新闻爬取任务已启动";
    }

    // 爬取行业概念涨幅前5数据
    @PostMapping("/fetch-category")
    public String fetchCategoryTop5() {
        new Thread(() -> stockService.fetchCategoryTop5()).start();
        return "行业概念涨幅前5数据爬取任务已启动";
    }

    // 初始化新闻和行业概念表
    @PostMapping("/init-news-category-tables")
    public String initNewsAndCategoryTables() {
        stockService.initNewsAndCategoryTables();
        return "新闻和行业概念表初始化完成";
    }

    // 清空今日行业概念数据
    @PostMapping("/clear-today-category")
    public String clearTodayCategory() {
        stockService.clearTodayCategoryData();
        return "已清空今日行业概念数据";
    }

    @Data
    public static class SkillRequest {
        private String skillName;
        private String prompt;
    }

}
