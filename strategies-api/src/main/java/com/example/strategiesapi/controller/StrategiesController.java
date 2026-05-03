package com.example.strategiesapi.controller;

import java.util.List;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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

    @Data
    public static class SkillRequest {
        private String skillName;
        private String prompt;
    }

}
