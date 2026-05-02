package com.example.springbootfasttest.service.impl;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.example.springbootfasttest.dto.StockQueryRequest;
import com.example.springbootfasttest.dto.StockQueryResponse;
import com.example.springbootfasttest.service.PythonSkillExecutor;
import com.example.springbootfasttest.service.StockDataService;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockDataServiceImpl implements StockDataService {

    private final PythonSkillExecutor pythonSkillExecutor;

    @Override
    public StockQueryResponse queryStockData(StockQueryRequest request) {
        log.info("收到股票筛选请求,规则数: {}", request.getSkillRules().size());

        List<StockQueryRequest.SkillRule> rules = request.getSkillRules();
        Map<String, Map<String, Object>> stockDataMap = new HashMap<>();

        for (StockQueryRequest.SkillRule rule : rules) {
            try {
                JSONObject result = pythonSkillExecutor.executeSkill(
                    rule.getSkillName(), 
                    rule.getParams()
                );
                
                JSONArray stocks = result.getJSONArray("stocks");
                if (stocks != null) {
                    for (int i = 0; i < stocks.size(); i++) {
                        JSONObject stock = stocks.getJSONObject(i);
                        String code = stock.getStr("code");
                        
                        stockDataMap.computeIfAbsent(code, k -> new HashMap<>());
                        Map<String, Object> data = stockDataMap.get(code);
                        data.put(rule.getSkillName(), stock);
                        data.put("name", stock.getStr("name"));
                    }
                }
            } catch (Exception e) {
                log.error("调用skill异常: {}", rule.getSkillName(), e);
            }
        }

        List<StockQueryResponse.StockResult> results = calculateWeightedScore(stockDataMap, rules);

        StockQueryResponse response = new StockQueryResponse();
        response.setStocks(results);
        response.setTotalCount(results.size());
        
        return response;
    }

    private List<StockQueryResponse.StockResult> calculateWeightedScore(
            Map<String, Map<String, Object>> stockDataMap,
            List<StockQueryRequest.SkillRule> rules) {
        
        List<StockQueryResponse.StockResult> results = new ArrayList<>();

        for (Map.Entry<String, Map<String, Object>> entry : stockDataMap.entrySet()) {
            String code = entry.getKey();
            Map<String, Object> data = entry.getValue();
            
            double totalScore = 0;
            double totalWeight = 0;

            for (StockQueryRequest.SkillRule rule : rules) {
                if (data.containsKey(rule.getSkillName())) {
                    Double weight = rule.getWeight() != null ? rule.getWeight() : 1.0;
                    JSONObject skillData = (JSONObject) data.get(rule.getSkillName());
                    
                    Double score = extractScore(skillData, rule.getSkillName());
                    if (score != null) {
                        totalScore += score * weight;
                        totalWeight += weight;
                    }
                }
            }

            if (totalWeight > 0) {
                StockQueryResponse.StockResult result = new StockQueryResponse.StockResult();
                result.setCode(code);
                result.setName((String) data.get("name"));
                result.setScore(Math.round(totalScore / totalWeight * 100.0) / 100.0);
                result.setSkillData(data);
                results.add(result);
            }
        }

        results.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
        return results;
    }

    private Double extractScore(JSONObject skillData, String skillName) {
        switch (skillName.toLowerCase()) {
            case "technical":
                return skillData.getDouble("technicalScore");
            case "fundamental":
                return skillData.getDouble("fundamentalScore");
            case "capital_flow":
                return skillData.getDouble("flowScore");
            case "sentiment":
                return skillData.getDouble("sentimentScore");
            case "valuation":
                return skillData.getDouble("valuationScore");
            default:
                return skillData.getDouble("score");
        }
    }
}
