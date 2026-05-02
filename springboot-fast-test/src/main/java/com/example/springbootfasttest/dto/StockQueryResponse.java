package com.example.springbootfasttest.dto;

import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public class StockQueryResponse {
    private List<StockResult> stocks;
    private Integer totalCount;
    
    @Data
    public static class StockResult {
        private String code;
        private String name;
        private Double score;
        private Map<String, Object> skillData;
    }
}
