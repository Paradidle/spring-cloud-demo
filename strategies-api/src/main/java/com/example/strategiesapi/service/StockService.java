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
}