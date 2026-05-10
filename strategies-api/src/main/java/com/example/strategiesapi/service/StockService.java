package com.example.strategiesapi.service;

import java.util.List;
import com.example.strategiesapi.model.StockInfo;

public interface StockService {
    
    /**
     * 获取股票列表，主要是上深两市的主板票，过滤st/北交所/创业板股票
     */
    List<String> getStockList();
    
    /**
     * 获取单只股票的详情，包括代码/名称/最高价/最低价/开盘价/实时走势数据
     */
    StockInfo getStockDetail(String stockCode);
    
    /**
     * 获取指定日期的股票详情数据
     */
    StockInfo getStockDetailByDate(String stockCode, String date);
    
    /**
     * 初始化历史数据，需要近1年的所有股票详情数据
     */
    void initHistoricalData();
    
    /**
     * 快速初始化股票基本信息（只插入基本信息，不获取历史数据和分时）
     */
    void initBasicDataOnly();
    
    /**
     * 获取股票基本信息数量
     */
    String getBasicCount();

    /**
     * 初始化5分钟K线数据（从新浪API获取近一年数据）
     */
    void initMinuteKlineData();

    /**
     * 获取日线数据数量
     */
    String getDailyCount();

    /**
     * 初始化大盘指数基本信息（上证、深证、创业板、科创50等）
     */
    void initIndexBasic();

    /**
     * 初始化大盘指数日线数据（近一年）
     */
    void initIndexDaily();

    /**
     * 更新今日行情数据（所有股票+大盘指数）
     */
    void updateTodayData();

    /**
     * 补充近几天缺失的行情数据
     * @param days 补充最近几天的数据
     */
    void fillRecentData(int days);

    /**
     * 爬取财联社新闻（加红栏目）
     */
    void fetchClsNews();

    /**
     * 爬取行业概念涨幅前5数据
     */
    void fetchCategoryTop5();

    /**
     * 初始化新闻和行业概念表
     */
    void initNewsAndCategoryTables();

    /**
     * 清空今日行业概念数据（用于重新测试）
     */
    void clearTodayCategoryData();

    void fillAllHistoryData();
}