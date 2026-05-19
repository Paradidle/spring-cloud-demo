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

            log.info("股票 {} 箱体分析完成", stockCode);
            return rawJson;
            
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
            result.put("validStrategy", jsonObject.getBooleanValue("validStrategy"));
            
            // 如果策略有效，添加箱体日期范围
            if (jsonObject.getBooleanValue("validStrategy")) {
                result.put("boxStartDate", jsonObject.getString("boxStartDate"));
                result.put("boxEndDate", jsonObject.getString("boxEndDate"));
            }
            
            // 如果策略无效，添加原因
            if (!jsonObject.getBooleanValue("validStrategy")) {
                result.put("reason", jsonObject.getString("reason"));
            }
            
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
               .select(StockBasic::getStockCode, StockBasic::getStockName)
                .orderByDesc(StockBasic::getStockCode);
//                .last("limit 100");
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
        
        // 第一步：分析上证指数当前位置
        String marketStrategy = analyzeMarketIndexPosition();
        log.info("大盘策略分析完成: {}", marketStrategy);
        
        // 创建线程池进行并行处理
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        List<Future<Map<String, Object>>> futures = new ArrayList<>();
        
        int totalStocks = stocks.size();
        int completedCount = completedStocks.size();
        AtomicInteger processedCount = new AtomicInteger(completedCount);
        
        // 提交分析任务
        for (StockBasic stock : pendingStocks) {
            String finalMarketStrategy = marketStrategy; // 用于lambda表达式
            futures.add(executor.submit(() -> {
                try {
                    // 使用新方法直接获取结构化数据
                    Map<String, Object> result = analyzeBoxPositionWithStructuredData(stock.getStockCode());
                    
                    // 补充股票名称
                    result.put("stockName", stock.getStockName());
                    
                    // 应用后置策略：根据大盘位置判断板块匹配度
                    String strategyAdvice = applyPostStrategy(stock.getStockName(), result, finalMarketStrategy);
                    result.put("strategyAdvice", strategyAdvice);
                    
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
            jsonObject.put("validStrategy", result.get("validStrategy"));
            
            // 如果策略有效，添加箱体日期范围
            if (result.containsKey("boxStartDate")) {
                jsonObject.put("boxStartDate", result.get("boxStartDate"));
            }
            if (result.containsKey("boxEndDate")) {
                jsonObject.put("boxEndDate", result.get("boxEndDate"));
            }
            
            // 如果策略无效，添加原因
            if (result.containsKey("reason")) {
                jsonObject.put("reason", result.get("reason"));
            }
            
            jsonObject.put("summary", result.get("summary"));
            
            // 添加后置策略建议（包含AI评级）
            if (result.containsKey("strategyAdvice")) {
                jsonObject.put("strategyAdvice", result.get("strategyAdvice"));
            }
            
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
                        result.put("validStrategy", jsonObject.getBooleanValue("validStrategy"));
                        
                        // 如果策略有效，添加箱体日期范围
                        if (jsonObject.containsKey("boxStartDate")) {
                            result.put("boxStartDate", jsonObject.getString("boxStartDate"));
                        }
                        if (jsonObject.containsKey("boxEndDate")) {
                            result.put("boxEndDate", jsonObject.getString("boxEndDate"));
                        }
                        
                        // 如果策略无效，添加原因
                        if (jsonObject.containsKey("reason")) {
                            result.put("reason", jsonObject.getString("reason"));
                        }
                        
                        result.put("summary", jsonObject.getString("summary"));
                        
                        // 加载后置策略建议
                        if (jsonObject.containsKey("strategyAdvice")) {
                            String strategyAdvice = jsonObject.getString("strategyAdvice");
                            result.put("strategyAdvice", strategyAdvice);
                            
                            // 解析出星级和理由（单独字段）
                            Map<String, String> parsedStrategy = parseStrategyAdvice(strategyAdvice);
                            result.put("rating", parsedStrategy.get("rating"));
                            result.put("reason", parsedStrategy.get("reason"));
                            result.put("sector", parsedStrategy.get("sector"));
                            result.put("suggestion", parsedStrategy.get("suggestion"));
                        }
                        
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
     * 将结果导出到指定的Excel文件
     */
    private void exportResultsToExcelFile(List<Map<String, Object>> results, String filePath) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("股票箱体分析结果");
            
            // 创建表头 - 精简版，星级和理由放在前面
            Row headerRow = sheet.createRow(0);
            String[] headers = {"股票代码", "股票名称", "推荐星级", "推荐理由", "所属板块", "操作建议", "箱顶价格", "箱底价格", "当前价格", "R值", "位置判断", "基础建议"};
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
                
                // 推荐星级和理由（放在前面）
                row.createCell(2).setCellValue(getStringValue(result, "rating"));
                row.createCell(3).setCellValue(getStringValue(result, "reason"));
                row.createCell(4).setCellValue(getStringValue(result, "sector"));
                row.createCell(5).setCellValue(getStringValue(result, "suggestion"));
                
                // 数值字段
                row.createCell(6).setCellValue(getDoubleValue(result, "boxTop"));
                row.createCell(7).setCellValue(getDoubleValue(result, "boxBottom"));
                row.createCell(8).setCellValue(getDoubleValue(result, "currentPrice"));
                row.createCell(9).setCellValue(getDoubleValue(result, "R"));
                
                // 文本字段
                row.createCell(10).setCellValue(getStringValue(result, "position"));
                row.createCell(11).setCellValue(getStringValue(result, "advice"));
            }
            
            // 自动调整列宽
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
                // 设置最小列宽
                if (sheet.getColumnWidth(i) < 2000) {
                    sheet.setColumnWidth(i, 2000);
                }
                // 设置最大列宽（推荐理由和建议列可以宽一些）
                if (i == 3 || i == 5 || i == 11) {
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
                data.put("volume", daily.getVolume()); // 添加交易量数据
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
        sb.append("【箱体定义与识别规则】\n");
        sb.append("核心思想：箱体从价格首次确立水平支撑或阻力区间开始，到价格有效突破该区间且后续不再返回箱体内部为止。\n\n");
        
        sb.append("1.局部高点:High[i]>High[i-1]且High[i]>High[i+1]\n");
        sb.append("2.局部低点:Low[i]<Low[i-1]且Low[i]<Low[i+1]\n");
        sb.append("3.聚类(价差≤0.5%),取最大簇平均值为箱顶/箱底\n\n");
        
        sb.append("4.箱体有效性条件（必须同时满足）：\n");
        sb.append("  a.箱体必须至少包含35个交易日数据\n");
        sb.append("  b.箱顶-箱底 ≥ 过去20根K线平均真实波幅(ATR) × 0.5\n");
        sb.append("  c.振幅 ≥ 2%（(箱顶-箱底)/箱底 ≥ 0.02）\n");
        sb.append("  d.箱顶/箱底各自至少有两个局部极值点触碰\n\n");
        
        sb.append("5.箱体起点定义：箱底第一次被触及的日期或箱顶第一次被触及的日期，取两者中较早的那个日期\n");
        sb.append("6.箱体终点定义：有效突破确认的那一天（见下方突破判定规则）\n\n");
        
        sb.append("7.有效突破判定：\n");
        sb.append("  向上突破：连续两根K线的收盘价 > 箱顶 × 1.01，且突破后第一根K线成交量 ≥ 过去20日均量 × 1.2\n");
        sb.append("  向下突破：连续两根K线的收盘价 < 箱底 × 0.99，成交量条件可放宽\n");
        sb.append("  一旦确认有效突破，箱体终点 = 第二根突破K线的日期\n\n");
        
        sb.append("8.注意事项：\n");
        sb.append("  a.不能简单将一个极值作为超大箱体，必须有明确的横盘震荡特征\n");
        sb.append("  b.多个箱体按时间顺序排列，后一个箱体通常在前一个箱体被突破后才开始形成\n");
        sb.append("  c.如果无法识别出符合要求的箱体，标记为不符合策略\n");
        sb.append("  d.最后一个箱体如果没有被突破，则其终点为数据的最后一天\n\n");
        
        sb.append("9.R值计算：R=(现价-箱底)/(箱顶-箱底)\n");
        sb.append("10.R值位置判断：R>0.85顶部,R<0.15底部,0.4-0.6中部,R>1突破上,R<0突破下\n\n");
        
        sb.append("11.放量定义：当天交易量相比前7天平均交易量超过50%；缩量定义：当天交易量相比前7天平均交易量低于50%\n");
        sb.append("12.站稳定义：股价在某一价位连续3个交易日收盘价均不低于该价位，且期间未出现单日跌幅超过3%的情况\n");
        sb.append("12.交易信号判断：\n");
        sb.append("  a.突破前高或箱体顶部出现上影线且放量->卖出\n");
        sb.append("  b.箱体高位放量滞涨->卖出\n");
        sb.append("  c.箱体底部缩量并且站稳->买入\n");
        sb.append("  d.箱体中高位突破上/下->观望\n\n");
        
        sb.append("【数据】近120日K线(日期,开,高,低,收,成交量):\n");

        for (int i = 0; i < stockData.size(); i++) {
            Map<String, Object> data = stockData.get(i);
            sb.append(String.format("%s,%s,%s,%s,%s,%s\n",
                    data.get("date"),
                    data.get("open"),
                    data.get("high"),
                    data.get("low"),
                    data.get("close"),
                    data.get("volume")));
        }
        
        sb.append("\n【要求】直接返回JSON,格式:\n");
        sb.append("如果符合策略:{\"boxTop\":箱顶价格,\"boxBottom\":箱底价格,\"currentPrice\":最新收盘价,\"R\":R值,\"position\":位置(顶部/底部/中部/突破上/突破下),\"advice\":建议（卖出/买入/观望）,\"validStrategy\":true,\"boxStartDate\":箱体开始日期(YYYY-MM-DD格式),\"boxEndDate\":箱体结束日期(YYYY-MM-DD格式)}\n");
        sb.append("如果不符合策略:{\"validStrategy\":false,\"reason\":\"原因说明（如：箱体交易日不足35天/振幅不足2%/极值点不足等）\"}\n");
        sb.append("重要：必须返回明确的箱体开始和结束日期，不能简单将第一个和最后一个数据点作为箱体范围！\n");
        sb.append("不要任何解释,只要JSON!");
        
        return sb.toString();
    }
    
    /**
     * 分析上证指数当前位置
     */
    private String analyzeMarketIndexPosition() {
        log.info("开始分析上证指数(000001)箱体位置");
        
        try {
            // 获取上证指数的K线数据
            Map<String, Object> marketResult = analyzeBoxPositionWithStructuredData("sh000001");
            
            if (marketResult == null || !Boolean.TRUE.equals(marketResult.get("validStrategy"))) {
                log.warn("上证指数箱体分析失败，使用默认策略");
                return "AGGRESSIVE"; // 默认进攻型
            }
            
            Double rValue = (Double) marketResult.get("R");
            String position = (String) marketResult.get("position");
            
            log.info("上证指数 R值: {}, 位置: {}", rValue, position);
            
            // 根据R值判断大盘策略
            if (rValue != null) {
                if (rValue > 0.75) {
                    log.info("大盘处于箱顶区域 -> 防守型策略");
                    return "DEFENSIVE";
                } else if (rValue < 0.25) {
                    log.info("大盘处于箱底区域 -> 复苏型策略");
                    return "RECOVERY";
                } else {
                    log.info("大盘处于中部区域 -> 进攻型策略");
                    return "AGGRESSIVE";
                }
            }
            
            return "AGGRESSIVE"; // 默认
            
        } catch (Exception e) {
            log.error("分析上证指数失败", e);
            return "AGGRESSIVE"; // 出错时使用默认策略
        }
    }
    
    /**
     * 应用后置策略：调用AI模型识别板块并综合评估
     */
    private String applyPostStrategy(String stockName, Map<String, Object> stockResult, String marketStrategy) {
        // 检查策略是否有效
        Boolean validStrategy = (Boolean) stockResult.get("validStrategy");
        if (validStrategy == null || !validStrategy) {
            return "不符合箱体策略";
        }
        
        // 获取个股箱体信息
        String position = (String) stockResult.get("position");
        Double rValue = (Double) stockResult.get("R");
        Double boxTop = (Double) stockResult.get("boxTop");
        Double boxBottom = (Double) stockResult.get("boxBottom");
        Double currentPrice = (Double) stockResult.get("currentPrice");
        String advice = (String) stockResult.get("advice");
        
        // 构建AI分析的上下文信息
        String marketContext = getMarketContextDescription(marketStrategy);
        
        // 调用AI模型进行板块识别和综合评估
        try {
            String aiPrompt = buildPostStrategyPrompt(
                stockName, marketStrategy, marketContext,
                position, rValue, boxTop, boxBottom, currentPrice, advice
            );
            
            String aiResponse = callDashScopeAPI(aiPrompt);
            
            // 解析AI返回的JSON
            JSONObject jsonObject = JSON.parseObject(extractJsonFromResult(aiResponse));
            
            // 构建格式化的策略建议
            StringBuilder resultAdvice = new StringBuilder();
            resultAdvice.append("【大盘】").append(marketContext);
            resultAdvice.append(" | 【个股】").append(position != null ? position : "未知");
            
            // 添加AI识别的板块
            String identifiedSector = jsonObject.getString("identifiedSector");
            if (identifiedSector != null && !identifiedSector.isEmpty()) {
                resultAdvice.append(" | 【板块】").append(identifiedSector);
            }
            
            // 添加AI的评级和建议
            String rating = jsonObject.getString("rating");
            String reason = jsonObject.getString("reason");
            String suggestion = jsonObject.getString("suggestion");
            
            if (rating != null && !rating.isEmpty()) {
                resultAdvice.append(" | ").append(rating);
            }
            if (reason != null && !reason.isEmpty()) {
                resultAdvice.append(" | 理由:").append(reason);
            }
            if (suggestion != null && !suggestion.isEmpty()) {
                resultAdvice.append(" | 建议:").append(suggestion);
            }
            
            return resultAdvice.toString();
            
        } catch (Exception e) {
            log.error("AI评估失败，使用规则降级: {}", stockName, e);
            // AI失败时使用基于规则的降级方案
            return applyRuleBasedStrategy(stockName, stockResult, marketStrategy);
        }
    }
    
    /**
     * 获取大盘策略描述
     */
    private String getMarketContextDescription(String marketStrategy) {
        switch (marketStrategy) {
            case "DEFENSIVE":
                return "箱顶-防守为主";
            case "RECOVERY":
                return "箱底-布局复苏";
            case "AGGRESSIVE":
            default:
                return "中部-积极进攻";
        }
    }
    
    /**
     * 构建后置策略AI Prompt
     */
    private String buildPostStrategyPrompt(
        String stockName, String marketStrategy, String marketContext,
        String position, Double rValue, Double boxTop, Double boxBottom, 
        Double currentPrice, String advice
    ) {
        StringBuilder sb = new StringBuilder();
        
        sb.append("【任务】股票投资策略综合评估（含板块识别）\n\n");
        
        sb.append("【当前市场环境】\n");
        sb.append("大盘状态: ").append(marketContext).append("\n");
        sb.append("策略类型: ").append(marketStrategy).append("\n\n");
        
        sb.append("【个股信息】\n");
        sb.append("股票名称: ").append(stockName).append("\n");
        sb.append("箱体位置: ").append(position != null ? position : "未知").append("\n");
        if (rValue != null) {
            sb.append("R值: ").append(String.format("%.2f", rValue)).append("\n");
        }
        if (boxTop != null && boxBottom != null && currentPrice != null) {
            sb.append(String.format("箱顶: %.2f, 箱底: %.2f, 现价: %.2f\n", boxTop, boxBottom, currentPrice));
        }
        if (advice != null) {
            sb.append("基础建议: ").append(advice).append("\n");
        }
        
        sb.append("\n【评估要求】\n");
        sb.append("1. **板块识别**：根据股票名称，判断该股票属于哪个行业板块\n");
        sb.append("   - 例如：贵州茅台 → 白酒/消费板块\n");
        sb.append("   - 例如：宁德时代 → 新能源/锂电池板块\n");
        sb.append("   - 例如：工商银行 → 银行板块\n");
        sb.append("   - 例如：中国旅游 → 旅游板块\n");
        sb.append("2. **板块匹配度**：判断该股票所属板块是否符合当前大盘推荐的板块类型\n");
        sb.append("   - 防守型(DEFENSIVE)推荐：银行、农业、军工、能源等\n");
        sb.append("   - 进攻型(AGGRESSIVE)推荐：科技、半导体、光刻机、机器人等\n");
        sb.append("   - 复苏型(RECOVERY)推荐：旅游、酒店、物流、航运、中药等\n");
        sb.append("3. **位置评估**：评估个股在箱体中的相对位置（R值）\n");
        sb.append("4. **综合评级**：综合考虑大盘环境、板块匹配、个股位置，给出投资评级\n\n");
        
        sb.append("【评级标准】\n");
        sb.append("⭐⭐⭐ 完美选股：大盘底部+个股箱底+板块匹配复苏策略，或大盘顶部+个股箱顶+板块匹配防守策略\n");
        sb.append("⭐⭐ 优秀选股：大盘与个股位置合理，板块基本匹配\n");
        sb.append("⭐ 关注：有一定机会但存在风险\n");
        sb.append("⚠️ 观望：位置不佳或板块不匹配\n");
        sb.append("❌ 不推荐：明显不符合当前策略\n\n");
        
        sb.append("【输出格式】直接返回JSON：\n");
        sb.append("{\n");
        sb.append("  \"identifiedSector\": \"识别出的板块名称\",\n");
        sb.append("  \"sectorMatch\": \"板块是否匹配当前策略（是/否）\",\n");
        sb.append("  \"rating\": \"评级（⭐⭐⭐/⭐⭐/⭐/⚠️/❌）\",\n");
        sb.append("  \"reason\": \"简短说明理由（30字以内）\",\n");
        sb.append("  \"suggestion\": \"操作建议（买入/观望/卖出，10字以内）\"\n");
        sb.append("}\n\n");
        
        sb.append("注意：只返回JSON，不要任何解释！");
        
        return sb.toString();
    }
    
    /**
     * 基于规则的降级策略（当AI失败时使用）
     */
    private String applyRuleBasedStrategy(String stockName, Map<String, Object> stockResult, String marketStrategy) {
        StringBuilder sb = new StringBuilder();
        
        sb.append("【大盘】").append(getMarketContextDescription(marketStrategy));
        sb.append(" | 【个股】").append(stockResult.get("position"));
        sb.append(" | 【板块】基于规则评估");
        
        // 简单的板块判断逻辑
        String sectorType = inferSectorType(stockName);
        boolean sectorMatch = checkSectorMatch(sectorType, marketStrategy);
        
        sb.append(" | ").append(sectorMatch ? "板块匹配" : "板块不匹配");
        
        // 简单评级
        String rating = sectorMatch ? "⭐⭐" : "⭐";
        sb.append(" | ").append(rating);
        
        sb.append(" | 理由:规则评估");
        sb.append(" | 建议:").append(sectorMatch ? "可关注" : "谨慎");
        
        return sb.toString();
    }
    
    /**
     * 推断股票所属板块类型（简化版）
     */
    private String inferSectorType(String stockName) {
        if (stockName == null) return "UNKNOWN";
        
        // 防守型板块关键词
        if (stockName.contains("银行") || stockName.contains("保险") || 
            stockName.contains("农业") || stockName.contains("能源") ||
            stockName.contains("石油") || stockName.contains("煤炭")) {
            return "DEFENSIVE";
        }
        
        // 进攻型板块关键词
        if (stockName.contains("科技") || stockName.contains("电子") || 
            stockName.contains("芯片") || stockName.contains("半导体") ||
            stockName.contains("机器人") || stockName.contains("智能")) {
            return "AGGRESSIVE";
        }
        
        // 复苏型板块关键词
        if (stockName.contains("旅游") || stockName.contains("酒店") || 
            stockName.contains("航空") || stockName.contains("物流") ||
            stockName.contains("中药") || stockName.contains("医药")) {
            return "RECOVERY";
        }
        
        return "UNKNOWN";
    }
    
    /**
     * 检查板块是否匹配当前策略
     */
    private boolean checkSectorMatch(String sectorType, String marketStrategy) {
        if ("UNKNOWN".equals(sectorType)) {
            return false;
        }
        // 板块类型与策略类型一致即为匹配
        return sectorType.equals(marketStrategy);
    }
    
    /**
     * 解析策略建议字符串，提取星级、理由、板块、建议等字段
     */
    private Map<String, String> parseStrategyAdvice(String strategyAdvice) {
        Map<String, String> result = new HashMap<>();
        
        if (strategyAdvice == null || strategyAdvice.isEmpty()) {
            result.put("rating", "");
            result.put("reason", "");
            result.put("sector", "");
            result.put("suggestion", "");
            return result;
        }
        
        // 示例格式：【大盘】箱底-布局复苏 | 【个股】底部 | 【板块】白酒/消费 | ⭐⭐⭐ | 理由:xxx | 建议:xxx
        
        // 提取板块
        int sectorStart = strategyAdvice.indexOf("【板块】");
        if (sectorStart != -1) {
            int sectorEnd = strategyAdvice.indexOf(" |", sectorStart + 4);
            if (sectorEnd == -1) sectorEnd = strategyAdvice.length();
            String sector = strategyAdvice.substring(sectorStart + 4, sectorEnd).trim();
            result.put("sector", sector);
        } else {
            result.put("sector", "");
        }
        
        // 提取星级（查找⭐符号）
        int ratingStart = strategyAdvice.indexOf("⭐");
        if (ratingStart != -1) {
            int ratingEnd = strategyAdvice.indexOf(" |", ratingStart);
            if (ratingEnd == -1) ratingEnd = strategyAdvice.length();
            String rating = strategyAdvice.substring(ratingStart, ratingEnd).trim();
            result.put("rating", rating);
        } else {
            result.put("rating", "");
        }
        
        // 提取理由
        int reasonStart = strategyAdvice.indexOf("理由:");
        if (reasonStart != -1) {
            int reasonEnd = strategyAdvice.indexOf(" |", reasonStart + 3);
            if (reasonEnd == -1) reasonEnd = strategyAdvice.length();
            String reason = strategyAdvice.substring(reasonStart + 3, reasonEnd).trim();
            result.put("reason", reason);
        } else {
            result.put("reason", "");
        }
        
        // 提取建议
        int suggestionStart = strategyAdvice.indexOf("建议:");
        if (suggestionStart != -1) {
            int suggestionEnd = strategyAdvice.indexOf(" |", suggestionStart + 3);
            if (suggestionEnd == -1) suggestionEnd = strategyAdvice.length();
            String suggestion = strategyAdvice.substring(suggestionStart + 3, suggestionEnd).trim();
            result.put("suggestion", suggestion);
        } else {
            result.put("suggestion", "");
        }
        
        return result;
    }
}
