package com.example.strategiesapi.service;

/**
 * 股票数据定时任务 Service
 *
 * @author system
 * @since 2026-05-02
 */
public interface IStockScheduleService {

    /**
     * 定时拉取分时数据并入库
     */
    void fetchMinuteData();
}
