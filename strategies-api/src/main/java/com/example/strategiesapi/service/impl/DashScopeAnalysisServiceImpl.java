package com.example.strategiesapi.service.impl;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.strategiesapi.entity.StockDaily;
import com.example.strategiesapi.service.DashScopeAnalysisService;
import com.example.strategiesapi.service.IStockDailyService;

/**
 * DashScope AI 股票分析服务实现类（使用HTTP直接调用DashScope API）
 *
 * @author system
 * @since 2026-05-10
 */
@Service
@Slf4j
public class DashScopeAnalysisServiceImpl implements DashScopeAnalysisService {

    private final IStockDailyService stockDailyService;

    @Value("${spring.ai.dashscope.api-key}")
    private String apiKey;

    private static final String DASHSCOPE_API_URL = "https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation";
    private static final String MODEL_NAME = "qwen-turbo";  // 使用turbo模型，速度更快

    public DashScopeAnalysisServiceImpl(IStockDailyService stockDailyService) {
        this.stockDailyService = stockDailyService;
    }

    @Override
    public String analyzeBoxPosition(String stockCode) {
        try {
            List<Map<String, Object>> stockData = getRecentStockData(stockCode, 120);
            
            if (stockData == null || stockData.isEmpty()) {
                return "未找到股票 " + stockCode + " 的历史数据";
            }

            String prompt = buildAnalysisPrompt(stockCode, stockData);
            String result = callDashScopeAPI(prompt);
            
            // 尝试解析JSON并格式化输出
            String formattedResult = formatAnalysisResult(result, stockCode);
            
            log.info("股票 {} 箱体分析完成", stockCode);
            return formattedResult;
            
        } catch (Exception e) {
            log.error("分析股票 {} 箱体位置时出错", stockCode, e);
            return "分析失败: " + e.getMessage();
        }
    }

    /**
     * 格式化分析结果
     */
    private String formatAnalysisResult(String jsonResult, String stockCode) {
        try {
            JSONObject json = JSON.parseObject(jsonResult);
            
            StringBuilder sb = new StringBuilder();
            sb.append("【股票 ").append(stockCode).append(" 箱体分析结果】\n\n");
            sb.append("箱顶价格: ").append(json.getDouble("boxTop")).append(" 元\n");
            sb.append("箱底价格: ").append(json.getDouble("boxBottom")).append(" 元\n");
            sb.append("当前价格: ").append(json.getDouble("currentPrice")).append(" 元\n");
            sb.append("相对位置R: ").append(String.format("%.2f", json.getDouble("R"))).append("\n");
            sb.append("位置判断: ").append(json.getString("position")).append("\n\n");
            sb.append("操作建议: ").append(json.getString("advice"));
            
            return sb.toString();
        } catch (Exception e) {
            // 如果JSON解析失败，直接返回原始结果
            log.warn("JSON解析失败，返回原始结果", e);
            return jsonResult;
        }
    }

    /**
     * 直接调用 DashScope API
     */
    private String callDashScopeAPI(String prompt) {
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", MODEL_NAME);
        
        JSONObject input = new JSONObject();
        JSONArray messages = new JSONArray();
        
        JSONObject message = new JSONObject();
        message.put("role", "user");
        message.put("content", prompt);
        messages.add(message);
        
        input.put("messages", messages);
        requestBody.put("input", input);
        
        JSONObject parameters = new JSONObject();
        parameters.put("result_format", "message");
        parameters.put("max_tokens", 500);  // 限制输出长度
        parameters.put("temperature", 0.3);  // 降低温度，使输出更确定性、更快
        requestBody.put("parameters", parameters);
        
        HttpResponse response = HttpRequest.post(DASHSCOPE_API_URL)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .body(requestBody.toJSONString())
                .timeout(30000)  // 30秒超时
                .execute();
        
        if (!response.isOk()) {
            throw new RuntimeException("DashScope API调用失败: " + response.body());
        }
        
        JSONObject responseBody = JSON.parseObject(response.body());
        JSONObject output = responseBody.getJSONObject("output");
        
        if (output != null) {
            JSONArray choices = output.getJSONArray("choices");
            if (choices != null && !choices.isEmpty()) {
                JSONObject firstChoice = choices.getJSONObject(0);
                JSONObject msg = firstChoice.getJSONObject("message");
                if (msg != null) {
                    return msg.getString("content");
                }
            }
        }
        
        throw new RuntimeException("无法解析DashScope API响应");
    }

    @Override
    public List<Map<String, Object>> getRecentStockData(String stockCode, int days) {
        try {
            LocalDate endDate = LocalDate.now();
            LocalDate startDate = endDate.minusDays(days);

            LambdaQueryWrapper<StockDaily> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(StockDaily::getStockCode, stockCode)
                   .between(StockDaily::getTradeDate, startDate, endDate)
                   .orderByAsc(StockDaily::getTradeDate);

            List<StockDaily> stockDailies = stockDailyService.list(wrapper);

            return stockDailies.stream().map(daily -> {
                Map<String, Object> data = new HashMap<>();
                data.put("date", daily.getTradeDate().toString());
                data.put("open", daily.getOpenPrice());
                data.put("high", daily.getHighPrice());
                data.put("low", daily.getLowPrice());
                data.put("close", daily.getClosePrice());
                return data;
            }).collect(Collectors.toList());

        } catch (Exception e) {
            log.error("获取股票 {} 近{}天数据时出错", stockCode, days, e);
            return new ArrayList<>();
        }
    }

    private String buildAnalysisPrompt(String stockCode, List<Map<String, Object>> stockData) {
        StringBuilder sb = new StringBuilder();
        
        sb.append("【任务】股票箱体分析 - 请直接返回JSON格式结果，不要解释过程\n\n");
        sb.append("【规则】\n");
        sb.append("1.局部高点:High[i]>High[i-1]且High[i]>High[i+1]\n");
        sb.append("2.局部低点:Low[i]<Low[i-1]且Low[i]<Low[i+1]\n");
        sb.append("3.聚类(价差≤0.5%),取最大簇平均值为箱顶/箱底\n");
        sb.append("4.R=(现价-箱底)/(箱顶-箱底)\n");
        sb.append("5.R>0.85顶部,R<0.15底部,0.4-0.6中部,R>1突破上,R<0突破下\n\n");
        
        sb.append("【数据】近120日K线(日期,开,高,低,收):\n");

        for (int i = 0; i < stockData.size(); i++) {
            Map<String, Object> data = stockData.get(i);
            sb.append(String.format("%s,%s,%s,%s,%s\n",
                    data.get("date"),
                    data.get("open"),
                    data.get("high"),
                    data.get("low"),
                    data.get("close")));
        }
        
        sb.append("\n【要求】直接返回JSON,格式:{\"boxTop\":箱顶价格,\"boxBottom\":箱底价格,\"currentPrice\":最新收盘价,\"R\":R值,\"position\":位置(顶部/底部/中部/突破上/突破下),\"advice\":一句话建议}\n");
        sb.append("不要任何解释,只要JSON!");
        
        return sb.toString();
    }
}
