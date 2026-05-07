package com.example.strategiesapi.mapper;

import org.apache.ibatis.annotations.Mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.strategiesapi.entity.StockDailyNews;

/**
 * 财经新闻Mapper
 */
@Mapper
public interface StockDailyNewsMapper extends BaseMapper<StockDailyNews> {
}
