package com.example.strategiesapi.service.impl;

import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.strategiesapi.entity.StockDaily;
import com.example.strategiesapi.mapper.StockDailyMapper;
import com.example.strategiesapi.service.IStockDailyService;

/**
 * 股票日线数据 Service 实现类
 *
 * @author system
 * @since 2026-05-02
 */
@Service
public class StockDailyServiceImpl extends ServiceImpl<StockDailyMapper, StockDaily> implements IStockDailyService {
}
