package com.example.strategiesapi.model;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StockInfo {
    private String code;        // 股票代码
    private String name;        // 股票名称
    private Double highPrice;   // 最高价
    private Double lowPrice;    // 最低价
    private Double openPrice;   // 开盘价
    private List<Double> trendData; // 实时走势数据
}