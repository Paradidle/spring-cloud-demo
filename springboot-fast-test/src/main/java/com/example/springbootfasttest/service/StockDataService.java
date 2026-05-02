package com.example.springbootfasttest.service;

import com.example.springbootfasttest.dto.StockQueryRequest;
import com.example.springbootfasttest.dto.StockQueryResponse;

public interface StockDataService {
    StockQueryResponse queryStockData(StockQueryRequest request);
}
