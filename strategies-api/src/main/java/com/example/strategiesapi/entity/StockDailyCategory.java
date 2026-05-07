package com.example.strategiesapi.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 行业概念涨幅数据实体类
 * 记录财联社网站的行业和概念涨幅前5数据
 *
 * @author system
 */
@Data
@TableName("stock_daily_category")
public class StockDailyCategory {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 数据日期
     */
    private LocalDate dataDate;

    /**
     * 行业涨幅前5数据（JSON格式）
     * 格式：[{"name":"行业名称","changeRate":1.5},...]
     */
    private String industryTop5;

    /**
     * 概念涨幅前5数据（JSON格式）
     * 格式：[{"name":"概念名称","changeRate":2.3},...]
     */
    private String conceptTop5;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
