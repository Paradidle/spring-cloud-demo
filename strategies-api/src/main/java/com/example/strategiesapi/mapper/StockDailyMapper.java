package com.example.strategiesapi.mapper;

import org.apache.ibatis.annotations.Mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.strategiesapi.entity.StockDaily;

/**
 * 股票日线数据 Mapper 接口
 *
 * @author system
 * @since 2026-05-02
 */
@Mapper
public interface StockDailyMapper extends BaseMapper<StockDaily> {
}
