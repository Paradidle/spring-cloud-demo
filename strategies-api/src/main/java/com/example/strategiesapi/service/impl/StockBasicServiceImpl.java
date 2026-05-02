package com.example.strategiesapi.service.impl;

import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.strategiesapi.entity.StockBasic;
import com.example.strategiesapi.mapper.StockBasicMapper;
import com.example.strategiesapi.service.IStockBasicService;

/**
 * 股票基本信息 Service 实现类
 *
 * @author system
 * @since 2026-05-02
 */
@Service
public class StockBasicServiceImpl extends ServiceImpl<StockBasicMapper, StockBasic> implements IStockBasicService {
}
