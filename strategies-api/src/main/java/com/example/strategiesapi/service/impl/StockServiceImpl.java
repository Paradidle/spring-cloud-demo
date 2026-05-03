package com.example.strategiesapi.service.impl;

import cn.hutool.http.HttpRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.strategiesapi.entity.StockBasic;
import com.example.strategiesapi.entity.StockDaily;
import com.example.strategiesapi.model.StockInfo;
import com.example.strategiesapi.service.IStockBasicService;
import com.example.strategiesapi.service.IStockDailyService;
import com.example.strategiesapi.service.StockService;

@Service
@Slf4j
@RequiredArgsConstructor
public class StockServiceImpl implements StockService {

    private final IStockBasicService stockBasicService;
    private final IStockDailyService stockDailyService;

    // 新浪API
    private static final String SINA_STOCK_DETAIL_API = "http://hq.sinajs.cn/list=";
    private static final String SINA_STOCK_HISTORY_API = "http://money.finance.sina.com.cn/quotes_service/api/json_v2.php/CN_MarketData.getKLineData";
    
    // 东方财富API - 获取全量A股
    private static final String EAST_MONEY_STOCK_API = "http://push2.eastmoney.com/api/qt/clist/get";
    
    // 请求间隔（毫秒），避免被限流
    private static final int REQUEST_INTERVAL = 300;

    private String httpGet(String url) {
        try {
            return HttpRequest.get(url)
                    .header("Referer", "https://finance.sina.com.cn")
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .timeout(10000)
                    .execute()
                    .body();
        } catch (Exception e) {
            log.error("HTTP请求失败: {}", url, e);
            return null;
        }
    }

    @Override
    public List<String> getStockList() {
        log.info("使用东方财富API获取全量A股股票列表");
        List<String> stockCodes = new ArrayList<>();
        
        try {
            // 东方财富API - 获取沪深两市股票（沪A、深A、创业板）
            // m:1+t:2 = 上海主板 (600xxx)
            // m:0+t:6 = 深圳主板 (000xxx, 001xxx)
            // m:0+t:80 = 创业板 (300xxx)
            String[] markets = {"m:1+t:2", "m:0+t:6", "m:0+t:80"};
            
            for (String market : markets) {
                int page = 1;
                int totalPages = 1;
                
                while (page <= totalPages) {  // 根据总数动态计算页数
                    String url = EAST_MONEY_STOCK_API + "?cb=jQuery&pn=" + page + "&pz=500&po=1&np=1&ut=&fltt=2&invt=2&fid=f3&fs=" + market + "&fields=f12,f14";
                    String result = httpGet(url);
                    
                    if (result != null && !result.isEmpty()) {
                        // 解析JSONP响应
                        int start = result.indexOf("(");
                        int end = result.lastIndexOf(")");
                        if (start != -1 && end != -1) {
                            String jsonStr = result.substring(start + 1, end);
                            JSONObject json = JSON.parseObject(jsonStr);
                            JSONObject data = json.getJSONObject("data");
                            
                            if (data != null) {
                                // 注意：虽然请求pz=500，但实际API每页只返回100条
                                int pageSize = 100;
                                if (page == 1) {
                                    long total = data.getLong("total") != null ? data.getLong("total") : 0;
                                    totalPages = (int) Math.ceil((double) total / pageSize);
                                    log.info("市场 {} 预计 {} 页，共 {} 只股票", market, totalPages, total);
                                }
                                
                                // diff 可能是对象格式 {"0":{...},"1":{...}} 或数组格式 [{...},{...}]
                                Object diffObj = data.get("diff");
                                int pageCount = 0;
                                
                                if (diffObj instanceof JSONArray) {
                                    // 数组格式
                                    JSONArray stocks = (JSONArray) diffObj;
                                    for (int i = 0; i < stocks.size(); i++) {
                                        JSONObject stock = stocks.getJSONObject(i);
                                        String code = stock.getString("f12");
                                        String name = stock.getString("f14");
                                        
                                        if (code != null && name != null && 
                                            !name.contains("ST") && !name.contains("*") &&
                                            !name.contains("N ") &&
                                            !code.startsWith("688") &&
                                            !(code.length() == 4 && code.startsWith("8"))) {
                                            stockCodes.add(code);
                                        }
                                        pageCount++;
                                    }
                                } else if (diffObj instanceof JSONObject) {
                                    // 对象格式
                                    JSONObject stocksObj = (JSONObject) diffObj;
                                    for (String key : stocksObj.keySet()) {
                                        JSONObject stock = stocksObj.getJSONObject(key);
                                        String code = stock.getString("f12");
                                        String name = stock.getString("f14");
                                        
                                        if (code != null && name != null && 
                                            !name.contains("ST") && !name.contains("*") &&
                                            !name.contains("N ") &&
                                            !code.startsWith("688") &&
                                            !(code.length() == 4 && code.startsWith("8"))) {
                                            stockCodes.add(code);
                                        }
                                        pageCount++;
                                    }
                                }
                                
                                if (pageCount > 0) {
                                    log.info("市场 {} 第 {} 页，获取 {} 只股票，累计 {} 只", market, page, pageCount, stockCodes.size());
                                }
                            }
                        }
                    }
                    
                    page++;
                    Thread.sleep(REQUEST_INTERVAL);
                }
            }
            
            log.info("东方财富API共获取 {} 只股票", stockCodes.size());
        } catch (Exception e) {
            log.error("获取股票列表失败", e);
        }
        
        return stockCodes;
    }
    
    /**
     * 获取股票代码和名称的映射（直接从东方财富获取，不调用新浪API）
     */
    private Map<String, String> getStockCodeNameMap() {
        log.info("使用东方财富API获取股票代码和名称映射");
        Map<String, String> stockMap = new LinkedHashMap<>();
        
        try {
            String[] markets = {"m:1+t:2", "m:0+t:6", "m:0+t:80"};
            
            for (String market : markets) {
                int page = 1;
                int totalPages = 1;
                
                while (page <= totalPages) {
                    String url = EAST_MONEY_STOCK_API + "?cb=jQuery&pn=" + page + "&pz=500&po=1&np=1&ut=&fltt=2&invt=2&fid=f3&fs=" + market + "&fields=f12,f14";
                    String result = httpGet(url);
                    
                    if (result != null && !result.isEmpty()) {
                        int start = result.indexOf("(");
                        int end = result.lastIndexOf(")");
                        if (start != -1 && end != -1) {
                            String jsonStr = result.substring(start + 1, end);
                            JSONObject json = JSON.parseObject(jsonStr);
                            JSONObject data = json.getJSONObject("data");
                            
                            if (data != null) {
                                int pageSize = 100;
                                if (page == 1) {
                                    long total = data.getLong("total") != null ? data.getLong("total") : 0;
                                    totalPages = (int) Math.ceil((double) total / pageSize);
                                    log.info("市场 {} 预计 {} 页，共 {} 只股票", market, totalPages, total);
                                }
                                
                                Object diffObj = data.get("diff");
                                
                                if (diffObj instanceof JSONArray) {
                                    JSONArray stocks = (JSONArray) diffObj;
                                    for (int i = 0; i < stocks.size(); i++) {
                                        JSONObject stock = stocks.getJSONObject(i);
                                        String code = stock.getString("f12");
                                        String name = stock.getString("f14");
                                        
                                        if (code != null && name != null && 
                                            !name.contains("ST") && !name.contains("*") &&
                                            !name.contains("N ") &&
                                            !code.startsWith("688") &&
                                            !(code.length() == 4 && code.startsWith("8"))) {
                                            stockMap.put(code, name);
                                        }
                                    }
                                } else if (diffObj instanceof JSONObject) {
                                    JSONObject stocksObj = (JSONObject) diffObj;
                                    for (String key : stocksObj.keySet()) {
                                        JSONObject stock = stocksObj.getJSONObject(key);
                                        String code = stock.getString("f12");
                                        String name = stock.getString("f14");
                                        
                                        if (code != null && name != null && 
                                            !name.contains("ST") && !name.contains("*") &&
                                            !name.contains("N ") &&
                                            !code.startsWith("688") &&
                                            !(code.length() == 4 && code.startsWith("8"))) {
                                            stockMap.put(code, name);
                                        }
                                    }
                                }
                                
                                log.info("市场 {} 第 {} 页，累计获取 {} 只股票", market, page, stockMap.size());
                            }
                        }
                    }
                    
                    page++;
                    Thread.sleep(REQUEST_INTERVAL);
                }
            }
            
            log.info("东方财富API共获取 {} 只股票（代码+名称）", stockMap.size());
        } catch (Exception e) {
            log.error("获取股票代码名称映射失败", e);
        }
        
        return stockMap;
    }

    @Override
    public StockInfo getStockDetail(String stockCode) {
        log.info("获取股票详情: {}", stockCode);
        
        try {
            String prefix = stockCode.startsWith("6") ? "sh" : "sz";
            String url = SINA_STOCK_DETAIL_API + prefix + stockCode;
            String result = httpGet(url);
            log.info("股票信息: {}", result);
            if (result == null || result.isEmpty()) {
                log.warn("未获取到股票信息: {}", stockCode);
                return null;
            }
            
            String[] parts = result.split("=");
            if (parts.length < 2) {
                return null;
            }
            
            String data = parts[1].replace("\"", "").replace(";", "");
            String[] fields = data.split(",");
            
            if (fields.length < 33) {
                return null;
            }
            
            StockInfo stockInfo = new StockInfo();
            stockInfo.setCode(stockCode);
            stockInfo.setName(fields[0]);
            stockInfo.setOpenPrice(Double.parseDouble(fields[1]));
            stockInfo.setHighPrice(Double.parseDouble(fields[4]));
            stockInfo.setLowPrice(Double.parseDouble(fields[5]));
            
            List<Double> trendData = getMinuteData(prefix, stockCode);
            stockInfo.setTrendData(trendData);
            
            return stockInfo;
        } catch (Exception e) {
            log.error("获取股票详情失败: {}", stockCode, e);
            return null;
        }
    }

    @Override
    public StockInfo getStockDetailByDate(String stockCode, String date) {
        log.info("获取股票指定日期详情: {}, 日期: {}", stockCode, date);
        
        try {
            String prefix = stockCode.startsWith("6") ? "sh" : "sz";
            String url = SINA_STOCK_HISTORY_API + "?symbol=" + prefix + stockCode + "&scale=240&ma=no&datalen=1&d1=" + date;
            String result = httpGet(url);
            JSONArray jsonArray = JSON.parseArray(result);
            
            if (jsonArray == null || jsonArray.isEmpty()) {
                log.warn("未获取到股票历史数据: {}, 日期: {}", stockCode, date);
                return null;
            }
            
            JSONObject data = jsonArray.getJSONObject(0);
            
            StockInfo stockInfo = new StockInfo();
            stockInfo.setCode(stockCode);
            stockInfo.setName(data.getString("name"));
            stockInfo.setOpenPrice(data.getDouble("open"));
            stockInfo.setHighPrice(data.getDouble("high"));
            stockInfo.setLowPrice(data.getDouble("low"));
            stockInfo.setTrendData(new ArrayList<>());
            
            return stockInfo;
        } catch (Exception e) {
            log.error("获取股票历史详情失败: {}, 日期: {}", stockCode, date, e);
            return null;
        }
    }

    @Override
    public void initHistoricalData() {
        log.info("使用东方财富API开始初始化股票数据");
        
        // 直接从东方财富获取股票代码和名称映射（不调用新浪API）
        Map<String, String> stockMap = getStockCodeNameMap();
        log.info("共需要处理 {} 只股票", stockMap.size());
        
        int basicCount = 0;
        int dailyCount = 0;
        int minuteCount = 0;
        
        // 计算近一年的日期范围
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusYears(1);
        String beginStr = startDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String endStr = endDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        log.info("历史数据范围: {} 至 {}", startDate, endDate);
        
        for (Map.Entry<String, String> entry : stockMap.entrySet()) {
            String stockCode = entry.getKey();
            String stockName = entry.getValue();
            
            try {
                String prefix = stockCode.startsWith("6") ? "sh" : "sz";
                int marketId = stockCode.startsWith("6") ? 1 : 0;  // 1=上海, 0=深圳
                
                // 直接使用东方财富返回的名称插入股票基本信息（不调用新浪API）
                LambdaQueryWrapper<StockBasic> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(StockBasic::getStockCode, stockCode);
                StockBasic existBasic = stockBasicService.getOne(wrapper);
                
                if (existBasic == null) {
                    StockBasic basic = new StockBasic();
                    basic.setStockCode(stockCode);
                    basic.setMarket(prefix);
                    basic.setStockName(stockName);
                    stockBasicService.save(basic);
                    basicCount++;
                    log.info("新增股票: {} - {}", stockCode, stockName);
                }
                
                // 获取历史日线数据（近一年）
                try {
                    String histUrl = String.format(
                        "https://push2his.eastmoney.com/api/qt/stock/kline/get?secid=%d.%s&fields1=f1,f2,f3,f4,f5,f6&fields2=f51,f52,f53,f54,f55,f56,f57,f58&klt=101&fqt=1&beg=%s&end=%s",
                        marketId, stockCode, beginStr, endStr);
                    String histResult = httpGet(histUrl);
                    
                    if (histResult != null && !histResult.isEmpty()) {
                        int jsonStart = histResult.indexOf("{");
                        int jsonEnd = histResult.lastIndexOf("}") + 1;
                        if (jsonStart != -1 && jsonEnd > jsonStart) {
                            String jsonStr = histResult.substring(jsonStart, jsonEnd);
                            JSONObject histJson = JSON.parseObject(jsonStr);
                            JSONObject data = histJson.getJSONObject("data");
                            
                            if (data != null) {
                                JSONArray klines = data.getJSONArray("klines");
                                if (klines != null && !klines.isEmpty()) {
                                    for (int j = 0; j < klines.size(); j++) {
                                        String kline = klines.getString(j);
                                        String[] fields = kline.split(",");
                                        if (fields.length >= 6) {
                                            LocalDate tradeDate = LocalDate.parse(fields[0], DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                                            
                                            LambdaQueryWrapper<StockDaily> wrapper2 = new LambdaQueryWrapper<>();
                                            wrapper2.eq(StockDaily::getStockCode, stockCode)
                                                    .eq(StockDaily::getTradeDate, tradeDate);
                                            StockDaily existDaily = stockDailyService.getOne(wrapper2);
                                            
                                            StockDaily daily = existDaily != null ? existDaily : new StockDaily();
                                            daily.setStockCode(stockCode);
                                            daily.setTradeDate(tradeDate);
                                            daily.setOpenPrice(BigDecimal.valueOf(Double.parseDouble(fields[1])));
                                            daily.setClosePrice(BigDecimal.valueOf(Double.parseDouble(fields[2])));
                                            daily.setHighPrice(BigDecimal.valueOf(Double.parseDouble(fields[3])));
                                            daily.setLowPrice(BigDecimal.valueOf(Double.parseDouble(fields[4])));
                                            
                                            if (existDaily != null) {
                                                stockDailyService.updateById(daily);
                                            } else {
                                                stockDailyService.save(daily);
                                            }
                                            dailyCount++;
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    log.warn("获取历史日线数据失败: {}", stockCode, e);
                }
                
                // 跳过新浪API获取分时数据（太慢），只获取今日分时
                try {
                    // 使用东方财富API获取今日分时数据（更快）
                    String minuteUrl = String.format(
                        "https://push2.eastmoney.com/api/qt/stock/trends2/get?secid=%d.%s&fields1=f1,f2,f3,f4,f5,f6,f7,f8,f9,f10,f11,f12,f13&fields2=f51,f52,f53,f54,f55,f56,f57,f58&iscr=0",
                        marketId, stockCode);
                    String minuteResult = httpGet(minuteUrl);
                    
                    if (minuteResult != null && !minuteResult.isEmpty()) {
                        int jsonStart = minuteResult.indexOf("{");
                        int jsonEnd = minuteResult.lastIndexOf("}") + 1;
                        if (jsonStart != -1 && jsonEnd > jsonStart) {
                            String jsonStr = minuteResult.substring(jsonStart, jsonEnd);
                            JSONObject minuteJson = JSON.parseObject(jsonStr);
                            JSONObject data = minuteJson.getJSONObject("data");
                            
                            if (data != null) {
                                JSONArray trends = data.getJSONArray("trends");
                                if (trends != null && !trends.isEmpty()) {
                                    List<Double> minuteList = new ArrayList<>();
                                    for (int k = 0; k < trends.size(); k++) {
                                        String trend = trends.getString(k);
                                        String[] parts = trend.split(",");
                                        if (parts.length >= 2) {
                                            try {
                                                minuteList.add(Double.parseDouble(parts[1]));
                                            } catch (NumberFormatException ignored) {}
                                        }
                                    }
                                    
                                    if (!minuteList.isEmpty()) {
                                        LocalDate today = LocalDate.now();
                                        LambdaQueryWrapper<StockDaily> wrapper3 = new LambdaQueryWrapper<>();
                                        wrapper3.eq(StockDaily::getStockCode, stockCode)
                                                .eq(StockDaily::getTradeDate, today);
                                        StockDaily todayDaily = stockDailyService.getOne(wrapper3);
                                        
                                        if (todayDaily != null) {
                                            todayDaily.setMinuteData(JSON.toJSONString(minuteList));
                                            stockDailyService.updateById(todayDaily);
                                            minuteCount++;
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    log.debug("获取分时数据失败: {}", stockCode);
                }
                
                // 打印进度
                if (basicCount % 100 == 0 && basicCount > 0) {
                    log.info("已处理 {} 只股票，累计: 基本信息{}条, 日线{}条, 分时{}条", 
                             basicCount, basicCount, dailyCount, minuteCount);
                }
                
                Thread.sleep(REQUEST_INTERVAL);
            } catch (Exception e) {
                log.error("处理股票 {} 时出错", stockCode, e);
            }
        }
        
        log.info("数据初始化完成: 基本信息 {} 条, 日线数据 {} 条, 分时数据 {} 条", basicCount, dailyCount, minuteCount);
    }

    private List<Double> getMinuteData(String prefix, String stockCode) {
        List<Double> prices = new ArrayList<>();
        try {
            String url = "http://money.finance.sina.com.cn/quotes_service/api/json_v2.php/CN_MarketData.getKLineData?symbol=" + prefix + stockCode + "&scale=5&ma=no&datalen=48";
            String result = httpGet(url);
            
            log.debug("分时数据原始响应: {}", result);
            
            if (result == null || result.isEmpty()) {
                return prices;
            }
            
            int start = result.indexOf("[");
            int end = result.lastIndexOf("]");
            if (start == -1 || end == -1) {
                log.warn("无法解析分时数据JSON: {}", result);
                return prices;
            }
            
            String jsonStr = result.substring(start, end + 1);
            JSONArray jsonArray = JSON.parseArray(jsonStr);
            
            if (jsonArray != null && !jsonArray.isEmpty()) {
                for (int i = 0; i < jsonArray.size(); i++) {
                    JSONObject item = jsonArray.getJSONObject(i);
                    Double close = item.getDouble("close");
                    if (close != null) {
                        prices.add(close);
                    }
                }
                log.info("获取到 {} 条分时数据", prices.size());
            }
        } catch (Exception e) {
            log.error("获取分时数据失败: {}{}", prefix, stockCode, e);
        }
        return prices;
    }
    
    @Override
    public void initBasicDataOnly() {
        log.info("=== 开始快速初始化股票基本信息 ===");
        
        // 获取东方财富API的股票代码和名称映射
        Map<String, String> nameMap = getStockCodeNameMap();
        log.info("东方财富API共获取 {} 只股票", nameMap.size());
        
        int count = 0;
        for (Map.Entry<String, String> entry : nameMap.entrySet()) {
            String stockCode = entry.getKey();
            String stockName = entry.getValue();
            
            // 检查是否已存在
            LambdaQueryWrapper<StockBasic> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(StockBasic::getStockCode, stockCode);
            StockBasic existing = stockBasicService.getOne(wrapper);
            
            if (existing == null) {
                StockBasic basic = new StockBasic();
                basic.setStockCode(stockCode);
                basic.setStockName(stockName);
                
                // 判断市场
                String market = "sh";
                if (stockCode.startsWith("0") || stockCode.startsWith("1") || stockCode.startsWith("2") || stockCode.startsWith("3")) {
                    market = "sz";
                }
                basic.setMarket(market);
                
                stockBasicService.save(basic);
                count++;
            }
            
            if (count % 100 == 0) {
                log.info("已插入 {} 只股票基本信息", count);
            }
        }
        
        log.info("=== 股票基本信息初始化完成，共插入 {} 只股票 ===", count);
        log.info("当前数据库股票数量: {}", stockBasicService.count());
    }
    
    @Override
    public String getBasicCount() {
        long count = stockBasicService.count();
        long dailyCount = stockDailyService.count();
        return String.format("股票基本信息: %d, 日线数据: %d", count, dailyCount);
    }

}
