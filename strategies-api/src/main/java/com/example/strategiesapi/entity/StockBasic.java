package com.example.strategiesapi.entity;

import java.time.LocalDateTime;
import lombok.Data;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 股票基本信息实体类
 *
 * @author system
 * @since 2026-05-02
 */
@Data
@TableName("stock_basic")
public class StockBasic {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 股票代码
     */
    private String stockCode;

    /**
     * 市场标识: sh-上海, sz-深圳
     */
    private String market;

    /**
     * 股票名称
     */
    private String stockName;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 是否为大盘指数
     */
    private Boolean isIndex;
}
