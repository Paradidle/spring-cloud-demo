package com.example.strategiesapi.service.impl;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.strategiesapi.entity.StockBasic;
import com.example.strategiesapi.entity.StockDaily;
import com.example.strategiesapi.service.DashScopeAnalysisService;
import com.example.strategiesapi.service.IStockBasicService;
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
    private final IStockBasicService stockBasicService;

    @Value("${spring.ai.dashscope.api-key}")
    private String apiKey;

    private static final String DASHSCOPE_API_URL = "https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation";
    private static final String MODEL_NAME = "qwen-turbo";  // 使用turbo模型，速度更快
    private static final int THREAD_POOL_SIZE = 5; // 多线程池大小
    private static final String RESULT_DIR = "stock_analysis_results";
    private static final String PROGRESS_FILE = "analysis_progress.txt";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public DashScopeAnalysisServiceImpl(IStockDailyService stockDailyService, IStockBasicService stockBasicService) {
        this.stockDailyService = stockDailyService;
        this.stockBasicService = stockBasicService;
    }

    @Override
    public String analyzeBoxPosition(String stockCode) {
        try {
            List<Map<String, Object>> stockData = getRecentStockData(stockCode, 120);
            
            if (stockData == null || stockData.isEmpty()) {
                return "未找到股票 " + stockCode + " 的历史数据";
            }

            String prompt = buildAnalysisPrompt(stockCode, stockData);
            String rawJson = callDashScopeAPI(prompt);
            
            // 尝试解析JSON并格式化输出
            String formattedResult = formatAnalysisResult(rawJson, stockCode);
            
            log.info("股票 {} 箱体分析完成", stockCode);
            return formattedResult;
            
        } catch (Exception e) {
            log.error("分析股票 {} 箱体位置时出错", stockCode, e);
            return "分析失败: " + e.getMessage();
        }
    }
    
    /**
     * 分析股票并返回结构化数据（用于批量分析）
     */
    public Map<String, Object> analyzeBoxPositionWithStructuredData(String stockCode) {
        Map<String, Object> result = new HashMap<>();
        result.put("stockCode", stockCode);
        
        try {
            List<Map<String, Object>> stockData = getRecentStockData(stockCode, 120);
            
            if (stockData == null || stockData.isEmpty()) {
                result.put("analysisResult", "未找到股票 " + stockCode + " 的历史数据");
                return result;
            }

            String prompt = buildAnalysisPrompt(stockCode, stockData);
            String rawJson = callDashScopeAPI(prompt);
            
            log.debug("股票 {} AI原始返回: {}", stockCode, rawJson);
            
            // 先提取JSON部分（AI可能返回额外文本）
            String jsonPart = extractJsonFromResult(rawJson);
            if (jsonPart == null) {
                log.warn("股票 {} 无法从返回中提取JSON，使用原始返回", stockCode);
                jsonPart = rawJson;
            } else {
                log.debug("股票 {} 提取的JSON: {}", stockCode, jsonPart);
            }
            
            // 解析JSON获取结构化数据
            JSONObject jsonObject;
            try {
                jsonObject = JSON.parseObject(jsonPart);
            } catch (Exception parseEx) {
                log.error("股票 {} JSON解析失败，原始内容: {}", stockCode, jsonPart, parseEx);
                result.put("analysisResult", "JSON解析失败: " + parseEx.getMessage());
                return result;
            }
            
            // 直接映射JSON字段到result（平铺结构）
            result.put("boxTop", jsonObject.getDoubleValue("boxTop"));
            result.put("boxBottom", jsonObject.getDoubleValue("boxBottom"));
            result.put("currentPrice", jsonObject.getDoubleValue("currentPrice"));
            result.put("R", jsonObject.getDoubleValue("R"));
            result.put("position", jsonObject.getString("position"));
            result.put("advice", jsonObject.getString("advice"));
            
            // 生成简洁的格式化文本（仅用于显示，不存储到JSON文件）
            String formattedResult = String.format(
                "箱顶: %.2f | 箱底: %.2f | 现价: %.2f | R: %.2f | %s",
                jsonObject.getDoubleValue("boxTop"),
                jsonObject.getDoubleValue("boxBottom"),
                jsonObject.getDoubleValue("currentPrice"),
                jsonObject.getDoubleValue("R"),
                jsonObject.getString("position")
            );
            result.put("summary", formattedResult);
            
            log.info("股票 {} 箱体分析完成: {}", stockCode, formattedResult);
            return result;
            
        } catch (Exception e) {
            log.error("分析股票 {} 箱体位置时出错", stockCode, e);
            result.put("analysisResult", "分析失败: " + e.getMessage());
            return result;
        }
    }

    @Override
    public String analyzeAllStocksAndExportToExcel() {
        log.info("开始批量分析所有股票的箱体位置");
        
        // 创建结果目录
        try {
            Files.createDirectories(Paths.get(RESULT_DIR));
        } catch (IOException e) {
            log.error("创建结果目录失败", e);
            return "创建结果目录失败: " + e.getMessage();
        }
        
        // 获取所有非指数股票代码
        LambdaQueryWrapper<StockBasic> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StockBasic::getIsIndex, false)
               .select(StockBasic::getStockCode, StockBasic::getStockName).last("limit 100");
        List<StockBasic> stocks = stockBasicService.list(wrapper);
        
        if (stocks.isEmpty()) {
            return "没有找到可分析的股票";
        }
        
        // 加载已完成的股票列表（断点续传）
        Set<String> completedStocks = loadCompletedStocks();
        log.info("共找到 {} 只股票需要分析，已完成 {} 只", stocks.size(), completedStocks.size());
        
        // 过滤出未完成的股票
        List<StockBasic> pendingStocks = stocks.stream()
                .filter(stock -> !completedStocks.contains(stock.getStockCode()))
                .collect(Collectors.toList());
        
        if (pendingStocks.isEmpty()) {
            log.info("所有股票已分析完成，直接生成Excel");
            return generateFinalExcel(stocks);
        }
        
        log.info("待分析股票数量: {}", pendingStocks.size());
        
        // 创建线程池进行并行处理
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        List<Future<Map<String, Object>>> futures = new ArrayList<>();
        
        int totalStocks = stocks.size();
        int completedCount = completedStocks.size();
        AtomicInteger processedCount = new AtomicInteger(completedCount);
        
        // 提交分析任务
        for (StockBasic stock : pendingStocks) {
            futures.add(executor.submit(() -> {
                try {
                    // 使用新方法直接获取结构化数据
                    Map<String, Object> result = analyzeBoxPositionWithStructuredData(stock.getStockCode());
                    
                    // 补充股票名称
                    result.put("stockName", stock.getStockName());
                    
                    // 保存单个股票的结果到文件
                    saveSingleStockResult(result);
                    
                    // 记录进度
                    int current = processedCount.incrementAndGet();
                    double progress = (current * 100.0) / totalStocks;
                    log.info("进度: [{}/{}] {}% - 股票 {} ({}) 分析完成", 
                            current, totalStocks, String.format("%.2f", progress), 
                            stock.getStockCode(), stock.getStockName());
                    
                    // 更新进度文件
                    updateProgressFile(stock.getStockCode());
                    
                    // 每处理50只股票，生成一次临时Excel
                    if (current % 50 == 0) {
                        generateTemporaryExcel(current, totalStocks);
                    }
                    
                    return result;
                } catch (Exception e) {
                    log.error("分析股票 {} 时发生异常", stock.getStockCode(), e);
                    Map<String, Object> errorResult = new HashMap<>();
                    errorResult.put("stockCode", stock.getStockCode());
                    errorResult.put("stockName", stock.getStockName());
                    errorResult.put("analysisResult", "分析失败: " + e.getMessage());
                    
                    // 保存错误结果
                    saveSingleStockResult(errorResult);
                    updateProgressFile(stock.getStockCode());
                    
                    int current = processedCount.incrementAndGet();
                    log.error("进度: [{}/{}] - 股票 {} ({}) 分析失败", 
                            current, totalStocks, stock.getStockCode(), stock.getStockName());
                    
                    return errorResult;
                }
            }));
        }
        
        // 收集结果
        List<Map<String, Object>> allResults = new ArrayList<>();
        for (Future<Map<String, Object>> future : futures) {
            try {
                allResults.add(future.get(5, TimeUnit.MINUTES)); // 设置超时时间
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                log.error("获取分析结果时发生异常", e);
            }
        }
        
        // 关闭线程池
        executor.shutdown();
        
        // 生成最终Excel
        String excelFilePath = generateFinalExcel(stocks);
        
        log.info("批量分析完成，结果已导出到: {}", excelFilePath);
        return "批量分析完成，共分析 " + stocks.size() + " 只股票，结果已导出到: " + excelFilePath;
    }
    
    /**
     * 加载已完成的股票列表
     */
    private Set<String> loadCompletedStocks() {
        Set<String> completed = new HashSet<>();
        Path progressPath = Paths.get(RESULT_DIR, PROGRESS_FILE);
        
        if (Files.exists(progressPath)) {
            try {
                List<String> lines = Files.readAllLines(progressPath);
                for (String line : lines) {
                    if (line.trim().startsWith("COMPLETED:")) {
                        String stockCode = line.substring("COMPLETED:".length()).trim();
                        completed.add(stockCode);
                    }
                }
                log.info("从进度文件加载了 {} 个已完成的股票", completed.size());
            } catch (IOException e) {
                log.warn("读取进度文件失败，将从头开始", e);
            }
        }
        
        return completed;
    }
    
    /**
     * 更新进度文件
     */
    private synchronized void updateProgressFile(String stockCode) {
        Path progressPath = Paths.get(RESULT_DIR, PROGRESS_FILE);
        String timestamp = LocalDateTime.now().format(DATE_TIME_FORMATTER);
        
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(progressPath.toFile(), true))) {
            writer.write(String.format("COMPLETED:%s at %s%n", stockCode, timestamp));
            writer.flush();
        } catch (IOException e) {
            log.error("更新进度文件失败", e);
        }
    }
    
    /**
     * 保存单个股票的分析结果到文件
     */
    private void saveSingleStockResult(Map<String, Object> result) {
        String stockCode = (String) result.get("stockCode");
        String fileName = stockCode + ".json";
        Path filePath = Paths.get(RESULT_DIR, fileName);
        
        try {
            // 创建简化的JSON结构，只包含关键字段
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("stockCode", result.get("stockCode"));
            jsonObject.put("stockName", result.get("stockName"));
            jsonObject.put("boxTop", result.get("boxTop"));
            jsonObject.put("boxBottom", result.get("boxBottom"));
            jsonObject.put("currentPrice", result.get("currentPrice"));
            jsonObject.put("R", result.get("R"));
            jsonObject.put("position", result.get("position"));
            jsonObject.put("advice", result.get("advice"));
            jsonObject.put("summary", result.get("summary"));
            
            String jsonContent = jsonObject.toJSONString();
            Files.write(filePath, jsonContent.getBytes("UTF-8"));
            
            log.debug("股票 {} 结果已保存到: {}", stockCode, filePath);
        } catch (IOException e) {
            log.error("保存股票 {} 的结果文件失败", stockCode, e);
        }
    }
    
    /**
     * 生成临时Excel文件
     */
    private void generateTemporaryExcel(int completed, int total) {
        String fileName = String.format("stock_box_analysis_temp_%d_of_%d_%s.xlsx", 
                completed, total, LocalDate.now().toString());
        String filePath = RESULT_DIR + "/" + fileName;
        
        try {
            // 读取所有已完成的结果文件
            List<Map<String, Object>> results = loadAllCompletedResults();
            
            if (!results.isEmpty()) {
                exportResultsToExcelFile(results, filePath);
                log.info("临时Excel已生成: {} ({}个股票)", filePath, results.size());
            }
        } catch (Exception e) {
            log.error("生成临时Excel失败", e);
        }
    }
    
    /**
     * 手动导出Excel（从已有的JSON结果文件）
     */
    public String exportExcelFromExistingResults() {
        log.info("开始从已有结果导出Excel");
        
        // 创建结果目录
        try {
            Files.createDirectories(Paths.get(RESULT_DIR));
        } catch (IOException e) {
            log.error("创建结果目录失败", e);
            return "创建结果目录失败: " + e.getMessage();
        }
        
        String fileName = "stock_box_analysis_" + LocalDate.now().toString() + ".xlsx";
        String filePath = RESULT_DIR + "/" + fileName;
        
        try {
            // 读取所有已完成的结果文件
            List<Map<String, Object>> results = loadAllCompletedResults();
            
            if (results.isEmpty()) {
                log.warn("没有找到任何分析结果");
                return "没有找到任何分析结果，请先运行批量分析";
            }
            
            exportResultsToExcelFile(results, filePath);
            log.info("Excel文件已生成: {}, 共 {} 个股票", filePath, results.size());
            return String.format("Excel文件已生成: %s, 共 %d 个股票", filePath, results.size());
            
        } catch (Exception e) {
            log.error("生成Excel失败", e);
            return "生成Excel失败: " + e.getMessage();
        }
    }
    
    /**
     * 生成最终Excel文件（内部调用）
     */
    private String generateFinalExcel(List<StockBasic> allStocks) {
        return exportExcelFromExistingResults();
    }
    
    /**
     * 加载所有已完成的结果
     */
    private List<Map<String, Object>> loadAllCompletedResults() {
        List<Map<String, Object>> results = new ArrayList<>();
        
        try {
            Path dirPath = Paths.get(RESULT_DIR);
            if (!Files.exists(dirPath)) {
                return results;
            }
            
            Files.list(dirPath)
                .filter(path -> path.toString().endsWith(".json"))
                .forEach(path -> {
                    try {
                        String content = new String(Files.readAllBytes(path), "UTF-8");
                        JSONObject jsonObject = JSON.parseObject(content);
                        
                        // 直接映射JSON字段到Map（平铺结构）
                        Map<String, Object> result = new HashMap<>();
                        result.put("stockCode", jsonObject.getString("stockCode"));
                        result.put("stockName", jsonObject.getString("stockName"));
                        result.put("boxTop", jsonObject.getDoubleValue("boxTop"));
                        result.put("boxBottom", jsonObject.getDoubleValue("boxBottom"));
                        result.put("currentPrice", jsonObject.getDoubleValue("currentPrice"));
                        result.put("R", jsonObject.getDoubleValue("R"));
                        result.put("position", jsonObject.getString("position"));
                        result.put("advice", jsonObject.getString("advice"));
                        result.put("summary", jsonObject.getString("summary"));
                        
                        results.add(result);
                    } catch (Exception e) {
                        log.error("读取结果文件失败: {}", path, e);
                    }
                });
            
            log.info("从结果目录加载了 {} 个股票的分析结果", results.size());
        } catch (IOException e) {
            log.error("遍历结果目录失败", e);
        }
        
        return results;
    }
    
    /**
     * 从分析结果文本中提取数值（用于旧格式的JSON文件）
     */
    private void extractDataFromAnalysisText(Map<String, Object> result, String text) {
        if (text == null || text.isEmpty()) {
            return;
        }
        
        try {
            // 提取箱顶价格: 10.78 元
            java.util.regex.Pattern boxTopPattern = java.util.regex.Pattern.compile("箱顶价格:\\s*([\\d.]+)");
            java.util.regex.Matcher boxTopMatcher = boxTopPattern.matcher(text);
            if (boxTopMatcher.find()) {
                result.put("boxTop", Double.parseDouble(boxTopMatcher.group(1)));
            }
            
            // 提取箱底价格: 9.6 元
            java.util.regex.Pattern boxBottomPattern = java.util.regex.Pattern.compile("箱底价格:\\s*([\\d.]+)");
            java.util.regex.Matcher boxBottomMatcher = boxBottomPattern.matcher(text);
            if (boxBottomMatcher.find()) {
                result.put("boxBottom", Double.parseDouble(boxBottomMatcher.group(1)));
            }
            
            // 提取当前价格: 10.77 元
            java.util.regex.Pattern currentPricePattern = java.util.regex.Pattern.compile("当前价格:\\s*([\\d.]+)");
            java.util.regex.Matcher currentPriceMatcher = currentPricePattern.matcher(text);
            if (currentPriceMatcher.find()) {
                result.put("currentPrice", Double.parseDouble(currentPriceMatcher.group(1)));
            }
            
            // 提取相对位置R: 0.95
            java.util.regex.Pattern rPattern = java.util.regex.Pattern.compile("相对位置R:\\s*([\\d.]+)");
            java.util.regex.Matcher rMatcher = rPattern.matcher(text);
            if (rMatcher.find()) {
                result.put("R", Double.parseDouble(rMatcher.group(1)));
            }
            
            // 提取位置判断: 顶部
            java.util.regex.Pattern positionPattern = java.util.regex.Pattern.compile("位置判断:\\s*(\\S+)");
            java.util.regex.Matcher positionMatcher = positionPattern.matcher(text);
            if (positionMatcher.find()) {
                result.put("position", positionMatcher.group(1));
            }
            
            // 提取操作建议
            java.util.regex.Pattern advicePattern = java.util.regex.Pattern.compile("操作建议:\\s*(.+)", java.util.regex.Pattern.DOTALL);
            java.util.regex.Matcher adviceMatcher = advicePattern.matcher(text);
            if (adviceMatcher.find()) {
                result.put("advice", adviceMatcher.group(1).trim());
            }
            
        } catch (Exception e) {
            log.warn("从文本中提取数据失败", e);
        }
    }
    
    /**
     * 将结果导出到指定的Excel文件
     */
    private void exportResultsToExcelFile(List<Map<String, Object>> results, String filePath) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("股票箱体分析结果");
            
            // 创建表头
            Row headerRow = sheet.createRow(0);
            String[] headers = {"股票代码", "股票名称", "箱顶价格", "箱底价格", "当前价格", "R值", "位置判断", "操作建议", "摘要"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
            }
            
            // 填充数据
            int rowNum = 1;
            for (Map<String, Object> result : results) {
                Row row = sheet.createRow(rowNum++);
                
                // 股票代码和名称
                row.createCell(0).setCellValue(getStringValue(result, "stockCode"));
                row.createCell(1).setCellValue(getStringValue(result, "stockName"));
                
                // 数值字段 - 直接映射
                row.createCell(2).setCellValue(getDoubleValue(result, "boxTop"));
                row.createCell(3).setCellValue(getDoubleValue(result, "boxBottom"));
                row.createCell(4).setCellValue(getDoubleValue(result, "currentPrice"));
                row.createCell(5).setCellValue(getDoubleValue(result, "R"));
                
                // 文本字段
                row.createCell(6).setCellValue(getStringValue(result, "position"));
                row.createCell(7).setCellValue(getStringValue(result, "advice"));
                row.createCell(8).setCellValue(getStringValue(result, "summary"));
            }
            
            // 自动调整列宽
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
                // 设置最小列宽
                if (sheet.getColumnWidth(i) < 2000) {
                    sheet.setColumnWidth(i, 2000);
                }
                // 设置最大列宽（摘要列可以宽一些）
                if (i == 8) {
                    if (sheet.getColumnWidth(i) > 15000) {
                        sheet.setColumnWidth(i, 15000);
                    }
                } else {
                    if (sheet.getColumnWidth(i) > 8000) {
                        sheet.setColumnWidth(i, 8000);
                    }
                }
            }
            
            // 写入文件
            try (FileOutputStream fileOut = new FileOutputStream(filePath)) {
                workbook.write(fileOut);
            }
            
            log.info("Excel文件已生成: {}, 共 {} 条记录", filePath, results.size());
            
        } catch (IOException e) {
            log.error("导出Excel文件时发生错误", e);
            throw new RuntimeException("导出Excel文件失败", e);
        }
    }
    
    /**
     * 安全获取字符串值
     */
    private String getStringValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return "";
        }
        return value.toString();
    }
    
    /**
     * 安全获取Double值
     */
    private double getDoubleValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return 0.0;
        }
        
        // 如果已经是Number类型
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        
        // 如果是字符串，尝试转换
        if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                return 0.0;
            }
        }
        
        return 0.0;
    }
    
    /**
     * 从分析结果中提取JSON部分
     */
    private String extractJsonFromResult(String result) {
        if (result == null) return null;
        
        // 查找JSON对象的开始和结束位置
        int startIdx = result.indexOf('{');
        int endIdx = result.lastIndexOf('}');
        
        if (startIdx != -1 && endIdx != -1 && endIdx > startIdx) {
            return result.substring(startIdx, endIdx + 1);
        }
        
        return null;
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
            LambdaQueryWrapper<StockDaily> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(StockDaily::getStockCode, stockCode)
                   .orderByDesc(StockDaily::getTradeDate)
                    .last("limit " + days);

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
