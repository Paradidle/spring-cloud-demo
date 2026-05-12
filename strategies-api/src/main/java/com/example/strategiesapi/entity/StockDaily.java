package com.example.strategiesapi.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 股票日线数据实体类
 *
 * @author system
 * @since 2026-05-02
 */
@Data
@TableName("stock_daily")
public class StockDaily {

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
     * 交易日期
     */
    private LocalDate tradeDate;

    /**
     * 开盘价
     */
    private BigDecimal openPrice;

    /**
     * 最高价
     */
    private BigDecimal highPrice;

    /**
     * 最低价
     */
    private BigDecimal lowPrice;

    /**
     * 收盘价
     */
    private BigDecimal closePrice;

    /**
     * 成交量(手)
     */
    private Long volume;

    /**
     * 总市值(元)
     */
    private BigDecimal totalMarketValue;

    /**
     * 净流入(元)
     */
    private BigDecimal netInflow;

    /**
     * 净流出(元)
     */
    private BigDecimal netOutflow;

    /**
     * 主力净流入(元)
     */
    private BigDecimal mainNetInflow;

    /**
     * 主力净流出(元)
     */
    private BigDecimal mainNetOutflow;

    /**
     * 分时数据JSON数组，存储5分钟K线收盘价
     */
    private String minuteData;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
