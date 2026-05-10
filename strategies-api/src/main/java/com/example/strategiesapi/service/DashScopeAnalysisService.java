package com.example.strategiesapi.service;

import java.util.List;
import java.util.Map;

/**
 * DashScope AI 股票分析服务
 *
 * @author system
 * @since 2026-05-10
 */
public interface DashScopeAnalysisService {

    /**
     * 分析股票箱体位置
     * @param stockCode 股票代码
     * @return 分析结果
     */
    String analyzeBoxPosition(String stockCode);

    /**
     * 获取近N天的股票数据
     * @param stockCode 股票代码
     * @param days 天数
     * @return 股票数据列表
     */
    List<Map<String, Object>> getRecentStockData(String stockCode, int days);
}