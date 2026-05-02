package com.example.strategiesapi.mapper;

import org.apache.ibatis.annotations.Mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.strategiesapi.entity.StockBasic;

/**
 * 股票基本信息 Mapper 接口
 *
 * @author system
 * @since 2026-05-02
 */
@Mapper
public interface StockBasicMapper extends BaseMapper<StockBasic> {
}
