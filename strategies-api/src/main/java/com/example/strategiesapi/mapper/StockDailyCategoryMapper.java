package com.example.strategiesapi.mapper;

import org.apache.ibatis.annotations.Mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.strategiesapi.entity.StockDailyCategory;

/**
 * 行业概念数据Mapper
 */
@Mapper
public interface StockDailyCategoryMapper extends BaseMapper<StockDailyCategory> {
}
