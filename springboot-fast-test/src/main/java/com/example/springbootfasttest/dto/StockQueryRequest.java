package com.example.springbootfasttest.dto;

import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public class StockQueryRequest {
    private List<SkillRule> skillRules;
    private String apiKey;
    
    @Data
    public static class SkillRule {
        private String skillName;
        private Double weight;
        private Map<String, Object> params;
    }
}
