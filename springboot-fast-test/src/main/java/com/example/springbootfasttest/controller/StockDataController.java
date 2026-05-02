package com.example.springbootfasttest.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.example.springbootfasttest.dto.StockQueryRequest;
import com.example.springbootfasttest.dto.StockQueryResponse;
import com.example.springbootfasttest.response.ApiResponse;
import com.example.springbootfasttest.service.StockDataService;

@RestController
@RequestMapping("/api/stock")
@RequiredArgsConstructor
public class StockDataController {

    private final StockDataService stockDataService;

    @PostMapping("/query")
    public ApiResponse<StockQueryResponse> queryStockData(@RequestBody StockQueryRequest request) {
        StockQueryResponse response = stockDataService.queryStockData(request);
        return ApiResponse.success(response);
    }
}
