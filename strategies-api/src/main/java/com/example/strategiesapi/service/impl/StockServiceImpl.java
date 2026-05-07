package com.example.strategiesapi.service.impl;

import cn.hutool.http.HttpRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
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
    private final JdbcTemplate jdbcTemplate;

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
                                            } catch (NumberFormatException ignored) {
                                            }
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

    @Override
    public String getDailyCount() {
        long dailyCount = stockDailyService.count();
        return String.format("日线数据: %d", dailyCount);
    }

    @Override
    public void initMinuteKlineData() {
        log.info("=== 开始优化版5分钟K线数据初始化 ===");
        // 获取所有股票
        List<StockBasic> allStocks = stockBasicService.list();
        log.info("共有 {} 只股票需要处理", allStocks.size());

        // 统计变量
        AtomicInteger processedCount = new AtomicInteger(0);
        AtomicInteger skipCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        AtomicInteger totalInserted = new AtomicInteger(0);

        // 创建线程池 - 5个并发线程
        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        log.info("使用 {} 个线程并行处理", threadCount);

        // 分批处理，每批50只股票
        int batchSize = 50;
        List<List<StockBasic>> batches = new ArrayList<>();
        for (int i = 0; i < allStocks.size(); i += batchSize) {
            int end = Math.min(i + batchSize, allStocks.size());
            batches.add(allStocks.subList(i, end));
        }

        log.info("共 {} 批，每批 {} 只股票", batches.size(), batchSize);

        for (int batchIdx = 0; batchIdx < batches.size(); batchIdx++) {
            List<StockBasic> batch = batches.get(batchIdx);
            log.info("=== 开始处理第 {}/{} 批 ===", batchIdx + 1, batches.size());

            List<CompletableFuture<Void>> futures = new ArrayList<>();

            for (StockBasic stock : batch) {
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    try {
                        int inserted = processStockMinuteKline(stock);
                        if (inserted > 0) {
                            processedCount.incrementAndGet();
                            totalInserted.addAndGet(inserted);
                        } else {
                            skipCount.incrementAndGet();
                        }
                    } catch (Exception e) {
                        errorCount.incrementAndGet();
                        log.error("处理股票 {} 失败: {}", stock.getStockCode(), e.getMessage());
                    }
                }, executor);
                futures.add(future);
            }

            // 等待本批完成
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            log.info("批次 {}/{} 完成: 成功处理 {} 只, 跳过 {} 只, 失败 {} 只, 本批新增 {} 条",
                    batchIdx + 1, batches.size(), processedCount.get(), skipCount.get(), errorCount.get(), totalInserted.get());

            // 批次间延迟
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        executor.shutdown();
        try {
            executor.awaitTermination(1, TimeUnit.HOURS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        log.info("=== 5分钟K线数据初始化完成 ===");
        log.info("处理完成: {} 只, 跳过: {} 只, 失败: {} 只, 共插入日线数据: {} 条",
                processedCount.get(), skipCount.get(), errorCount.get(), totalInserted.get());
        log.info("当前数据库日线数据总量: {}", stockDailyService.count());
    }

    /**
     * 处理单只股票的5分钟K线数据
     */
    private int processStockMinuteKline(StockBasic stock) throws Exception {
        String stockCode = stock.getStockCode();
        String prefix = stockCode.startsWith("6") ? "sh" : "sz";

        // 预检查：获取已存在的日期集合
        LambdaQueryWrapper<StockDaily> existWrapper = new LambdaQueryWrapper<>();
        existWrapper.eq(StockDaily::getStockCode, stockCode)
                .select(StockDaily::getTradeDate);
        List<StockDaily> existingList = stockDailyService.list(existWrapper);
        Map<LocalDate, Boolean> existingDates = new java.util.HashMap<>();
        for (StockDaily sd : existingList) {
            existingDates.put(sd.getTradeDate(), true);
        }

        // 调用新浪5分钟K线API
        String url = String.format(
                "http://money.finance.sina.com.cn/quotes_service/api/json_v2.php/CN_MarketData.getKLineData?symbol=%s%s&scale=5&ma=no&datalen=2500",
                prefix, stockCode
        );

        String result = httpGet(url);

        if (result == null || result.isEmpty() || result.equals("null")) {
            return 0;
        }

        // 解析JSON
        int start = result.indexOf("[");
        int end = result.lastIndexOf("]");
        if (start == -1 || end == -1) {
            return 0;
        }

        String jsonStr = result.substring(start, end + 1);
        JSONArray jsonArray = JSON.parseArray(jsonStr);

        if (jsonArray == null || jsonArray.isEmpty()) {
            return 0;
        }

        // 按日期分组
        Map<String, List<JSONObject>> dailyData = new LinkedHashMap<>();
        for (int j = 0; j < jsonArray.size(); j++) {
            JSONObject item = jsonArray.getJSONObject(j);
            String day = item.getString("day").substring(0, 10);
            dailyData.computeIfAbsent(day, k -> new ArrayList<>()).add(item);
        }

        // 过滤已存在的日期
        List<String> newDates = new ArrayList<>();
        for (String day : dailyData.keySet()) {
            LocalDate date = LocalDate.parse(day);
            if (!existingDates.containsKey(date)) {
                newDates.add(day);
            }
        }

        if (newDates.isEmpty()) {
            return 0;
        }

        // 批量构建插入对象
        List<StockDaily> toSave = new ArrayList<>();

        for (String day : newDates) {
            List<JSONObject> candles = dailyData.get(day);

            // 计算聚合数据
            double openPrice = candles.get(0).getDouble("open");
            double closePrice = candles.get(candles.size() - 1).getDouble("close");
            double highPrice = 0;
            double lowPrice = Double.MAX_VALUE;
            double totalVolume = 0;

            // 拼接分时数据
            StringBuilder minuteData = new StringBuilder();

            for (JSONObject candle : candles) {
                highPrice = Math.max(highPrice, candle.getDouble("high"));
                lowPrice = Math.min(lowPrice, candle.getDouble("low"));
                totalVolume += candle.getDouble("volume");


                minuteData.append(candle.getString("day").substring(11))
                        .append(",").append(candle.getDouble("open"))
                        .append(",").append(candle.getDouble("close"))
                        .append(",").append(candle.getDouble("high"))
                        .append(",").append(candle.getDouble("low"))
                        .append(",").append(new BigDecimal(String.valueOf(candle.getDouble("volume"))).longValue())
                        .append(";");
            }


            StockDaily daily = new StockDaily();
            daily.setStockCode(stockCode);
            daily.setTradeDate(LocalDate.parse(day));
            daily.setOpenPrice(BigDecimal.valueOf(openPrice));
            daily.setClosePrice(BigDecimal.valueOf(closePrice));
            daily.setHighPrice(BigDecimal.valueOf(highPrice));
            daily.setLowPrice(BigDecimal.valueOf(lowPrice));
            daily.setVolume((long) totalVolume);

            // JSON格式分时数据
            JSONArray minuteArray = new JSONArray();
            for (JSONObject candle : candles) {
                JSONArray point = new JSONArray();
                point.add(candle.getString("day").substring(11));
                point.add(candle.getDouble("open"));
                point.add(candle.getDouble("close"));
                point.add(candle.getDouble("high"));
                point.add(candle.getDouble("low"));

                minuteArray.add(point);
            }
            daily.setMinuteData(minuteArray.toJSONString());

            toSave.add(daily);
        }

        // 批量插入
        if (!toSave.isEmpty()) {
            stockDailyService.saveBatch(toSave);
            return toSave.size();
        }

        return 0;
    }

    /**
     * 获取并保存日线数据
     */
    private int fetchAndSaveDailyData(String prefix, String stockCode, LocalDate startDate, LocalDate endDate) {
        try {
            String url = SINA_STOCK_HISTORY_API + "?symbol=" + prefix + stockCode
                    + "&scale=240&ma=no&datalen=2500";

            String result = httpGet(url);
            if (result == null || result.isEmpty()) {
                return 0;
            }

            int start = result.indexOf("[");
            int end = result.lastIndexOf("]");
            if (start == -1 || end == -1) {
                return 0;
            }

            String jsonStr = result.substring(start, end + 1);
            JSONArray jsonArray = JSON.parseArray(jsonStr);
            if (jsonArray == null || jsonArray.isEmpty()) {
                return 0;
            }

            // 批量查询已存在的日期
            Set<LocalDate> existingDates = new java.util.HashSet<>();
            LambdaQueryWrapper<StockDaily> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(StockDaily::getStockCode, stockCode)
                    .ge(StockDaily::getTradeDate, startDate)
                    .le(StockDaily::getTradeDate, endDate);
            List<StockDaily> existingList = stockDailyService.list(wrapper);
            for (StockDaily existing : existingList) {
                existingDates.add(existing.getTradeDate());
            }

            // 按日期分组
            Map<String, List<JSONObject>> dailyData = new LinkedHashMap<>();
            for (int j = 0; j < jsonArray.size(); j++) {
                JSONObject item = jsonArray.getJSONObject(j);
                String day = item.getString("day").substring(0, 10);
                LocalDate date = LocalDate.parse(day);
                if (!date.isBefore(startDate) && !date.isAfter(endDate)) {
                    dailyData.computeIfAbsent(day, k -> new ArrayList<>()).add(item);
                }
            }

            // 过滤已存在的日期
            List<String> newDates = new ArrayList<>();
            for (String day : dailyData.keySet()) {
                LocalDate date = LocalDate.parse(day);
                if (!existingDates.contains(date)) {
                    newDates.add(day);
                }
            }

            if (newDates.isEmpty()) {
                return 0;
            }

            // 批量构建插入对象
            List<StockDaily> toSave = new ArrayList<>();

            for (String day : newDates) {
                List<JSONObject> candles = dailyData.get(day);

                // 计算聚合数据
                double openPrice = candles.get(0).getDouble("open");
                double closePrice = candles.get(candles.size() - 1).getDouble("close");
                double highPrice = 0;
                double lowPrice = Double.MAX_VALUE;
                double totalVolume = 0;

                for (JSONObject candle : candles) {
                    highPrice = Math.max(highPrice, candle.getDouble("high"));
                    lowPrice = Math.min(lowPrice, candle.getDouble("low"));
                    totalVolume += candle.getDouble("volume");
                }

                StockDaily daily = new StockDaily();
                daily.setStockCode(stockCode);
                daily.setTradeDate(LocalDate.parse(day));
                daily.setOpenPrice(BigDecimal.valueOf(openPrice));
                daily.setClosePrice(BigDecimal.valueOf(closePrice));
                daily.setHighPrice(BigDecimal.valueOf(highPrice));
                daily.setLowPrice(BigDecimal.valueOf(lowPrice));
                daily.setVolume((long) totalVolume);

                toSave.add(daily);
            }

            // 批量插入
            if (!toSave.isEmpty()) {
                stockDailyService.saveBatch(toSave);
                return toSave.size();
            }
        } catch (Exception e) {
            log.warn("获取 {} 日线数据失败", stockCode);
        }
        return 0;
    }

    @Override
    public void initIndexBasic() {
        log.info("=== 开始初始化大盘指数基本信息 ===");

        // 先检查is_index列是否存在
        Integer count = null;
        try {
            log.info("检查is_index列是否存在...");
            count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = 'stock_db' AND table_name = 'stock_basic' AND column_name = 'is_index'",
                Integer.class
            );
            log.info("列检查结果: count={}", count);
        } catch (Exception e) {
            log.error("检查列失败: {}", e.getMessage());
        }

        if (count == null || count == 0) {
            log.info("is_index列不存在，开始添加...");
            try {
                jdbcTemplate.execute("ALTER TABLE stock_basic ADD COLUMN is_index TINYINT(1) DEFAULT 0 COMMENT '是否为大盘指数'");
                log.info("is_index列添加成功");
            } catch (Exception e) {
                log.error("添加is_index列失败: {}", e.getMessage());
                return;
            }
        } else {
            log.info("is_index列已存在");
        }

        // 大盘指数列表
        List<StockBasic> indices = new ArrayList<>();

        // 上证指数
        StockBasic sh000001 = new StockBasic();
        sh000001.setStockCode("000001");
        sh000001.setStockName("上证指数");
        sh000001.setMarket("sh");
        sh000001.setIsIndex(true);
        indices.add(sh000001);

        // 深证成指
        StockBasic sz399001 = new StockBasic();
        sz399001.setStockCode("399001");
        sz399001.setStockName("深证成指");
        sz399001.setMarket("sz");
        sz399001.setIsIndex(true);
        indices.add(sz399001);

        // 创业板指
        StockBasic sz399006 = new StockBasic();
        sz399006.setStockCode("399006");
        sz399006.setStockName("创业板指");
        sz399006.setMarket("sz");
        sz399006.setIsIndex(true);
        indices.add(sz399006);

        // 科创50
        StockBasic sh000688 = new StockBasic();
        sh000688.setStockCode("000688");
        sh000688.setStockName("科创50");
        sh000688.setMarket("sh");
        sh000688.setIsIndex(true);
        indices.add(sh000688);

        // 上证50
        StockBasic sh000016 = new StockBasic();
        sh000016.setStockCode("000016");
        sh000016.setStockName("上证50");
        sh000016.setMarket("sh");
        sh000016.setIsIndex(true);
        indices.add(sh000016);

        // 沪深300
        StockBasic sh000300 = new StockBasic();
        sh000300.setStockCode("000300");
        sh000300.setStockName("沪深300");
        sh000300.setMarket("sh");
        sh000300.setIsIndex(true);
        indices.add(sh000300);

        // 中证500
        StockBasic sh000905 = new StockBasic();
        sh000905.setStockCode("000905");
        sh000905.setStockName("中证500");
        sh000905.setMarket("sh");
        sh000905.setIsIndex(true);
        indices.add(sh000905);

        // 中证1000
        StockBasic sh000852 = new StockBasic();
        sh000852.setStockCode("000852");
        sh000852.setStockName("中证1000");
        sh000852.setMarket("sh");
        sh000852.setIsIndex(true);
        indices.add(sh000852);

        // 批量保存
        int savedCount = 0;
        for (StockBasic index : indices) {
            LambdaQueryWrapper<StockBasic> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(StockBasic::getStockCode, index.getStockCode());
            StockBasic existing = stockBasicService.getOne(wrapper);

            if (existing == null) {
                stockBasicService.save(index);
                savedCount++;
                log.info("添加大盘指数: {} - {}", index.getStockCode(), index.getStockName());
            } else {
                // 更新为指数
                existing.setIsIndex(true);
                existing.setStockName(index.getStockName());
                stockBasicService.updateById(existing);
                log.info("更新大盘指数: {} - {}", index.getStockCode(), index.getStockName());
            }
        }

        log.info("=== 大盘指数基本信息初始化完成，共添加/更新 {} 个指数 ===", indices.size());
    }

    @Override
    public void initIndexDaily() {
        log.info("=== 开始初始化大盘指数日线数据 ===");

        // 确保指数基本信息已存在
        initIndexBasic();

        // 大盘指数代码列表
        String[] indexCodes = {"000001", "399001", "399006", "000688", "000016", "000300", "000905", "000852"};
        String[] prefixes = {"sh", "sz", "sz", "sh", "sh", "sh", "sh", "sh"};
        String[] names = {"上证指数", "深证成指", "创业板指", "科创50", "上证50", "沪深300", "中证500", "中证1000"};

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusYears(1);

        for (int i = 0; i < indexCodes.length; i++) {
            String stockCode = indexCodes[i];
            String prefix = prefixes[i];
            String name = names[i];

            log.info("开始获取 {} 的日线数据...", name);
            int inserted = fetchAndSaveDailyData(prefix, stockCode, startDate, endDate);
            log.info("{} 日线数据初始化完成，新增 {} 条", name, inserted);

            try {
                Thread.sleep(REQUEST_INTERVAL);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        log.info("=== 大盘指数日线数据初始化完成 ===");
    }

    @Override
    public void updateTodayData() {
        log.info("=== 开始更新今日行情数据 ===");
        LocalDate today = LocalDate.now();

        // 更新所有股票
        List<StockBasic> allStocks = stockBasicService.list();
        log.info("需要更新 {} 只股票/指数的今日数据", allStocks.size());

        int successCount = 0;
        int failCount = 0;

        for (StockBasic stock : allStocks) {
            try {
                String stockCode = stock.getStockCode();
                String prefix = stock.getMarket();

                // 获取今日日线数据
                String url = SINA_STOCK_HISTORY_API + "?symbol=" + prefix + stockCode
                        + "&scale=240&ma=no&datalen=2";

                String result = httpGet(url);

                if (result != null && !result.isEmpty()) {
                    int start = result.indexOf("[");
                    int end = result.lastIndexOf("]");
                    if (start != -1 && end != -1) {
                        String jsonStr = result.substring(start, end + 1);
                        JSONArray jsonArray = JSON.parseArray(jsonStr);

                        if (jsonArray != null && !jsonArray.isEmpty()) {
                            // 获取最新一条数据
                            JSONObject latest = jsonArray.getJSONObject(jsonArray.size() - 1);
                            String day = latest.getString("day").substring(0, 10);
                            LocalDate dataDate = LocalDate.parse(day);

                            // 只处理今日数据
                            if (dataDate.equals(today)) {
                                double open = latest.getDouble("open");
                                double close = latest.getDouble("close");
                                double high = latest.getDouble("high");
                                double low = latest.getDouble("low");
                                double volume = latest.getDouble("volume");

                                // 检查是否已存在
                                LambdaQueryWrapper<StockDaily> wrapper = new LambdaQueryWrapper<>();
                                wrapper.eq(StockDaily::getStockCode, stockCode)
                                        .eq(StockDaily::getTradeDate, today);
                                StockDaily existing = stockDailyService.getOne(wrapper);

                                if (existing != null) {
                                    existing.setOpenPrice(BigDecimal.valueOf(open));
                                    existing.setClosePrice(BigDecimal.valueOf(close));
                                    existing.setHighPrice(BigDecimal.valueOf(high));
                                    existing.setLowPrice(BigDecimal.valueOf(low));
                                    existing.setVolume((long) volume);
                                    stockDailyService.updateById(existing);
                                } else {
                                    StockDaily daily = new StockDaily();
                                    daily.setStockCode(stockCode);
                                    daily.setTradeDate(today);
                                    daily.setOpenPrice(BigDecimal.valueOf(open));
                                    daily.setClosePrice(BigDecimal.valueOf(close));
                                    daily.setHighPrice(BigDecimal.valueOf(high));
                                    daily.setLowPrice(BigDecimal.valueOf(low));
                                    daily.setVolume((long) volume);
                                    stockDailyService.save(daily);
                                }
                                successCount++;
                            }
                        }
                    }
                }

                Thread.sleep(REQUEST_INTERVAL);
            } catch (Exception e) {
                failCount++;
                log.warn("更新 {} 今日数据失败", stock.getStockCode());
            }
        }

        log.info("=== 今日行情数据更新完成: 成功 {} 条, 失败 {} 条 ===", successCount, failCount);
    }

    @Override
    public void fillRecentData(int days) {
        log.info("=== 开始补充近 {} 天的行情数据 ===", days);

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days);

        // 获取所有股票
        List<StockBasic> allStocks = stockBasicService.list();
        log.info("需要处理 {} 只股票/指数", allStocks.size());

        int totalSuccess = 0;
        int totalFail = 0;

        // 按日期遍历
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            // 跳过周末
            int dayOfWeek = date.getDayOfWeek().getValue();
            if (dayOfWeek > 5) {
                continue;
            }

            log.info("开始处理 {} 的数据...", date);
            int daySuccess = 0;
            int dayFail = 0;

            for (StockBasic stock : allStocks) {
                try {
                    String stockCode = stock.getStockCode();
                    String prefix = stock.getMarket();

                    // 获取历史数据
                    String url = SINA_STOCK_HISTORY_API + "?symbol=" + prefix + stockCode
                            + "&scale=240&ma=no&datalen=10";

                    String result = httpGet(url);

                    if (result != null && !result.isEmpty()) {
                        int jsonStart = result.indexOf("[");
                        int jsonEnd = result.lastIndexOf("]");
                        if (jsonStart != -1 && jsonEnd != -1) {
                            String jsonStr = result.substring(jsonStart, jsonEnd + 1);
                            JSONArray jsonArray = JSON.parseArray(jsonStr);

                            if (jsonArray != null) {
                                for (int i = 0; i < jsonArray.size(); i++) {
                                    JSONObject item = jsonArray.getJSONObject(i);
                                    String day = item.getString("day").substring(0, 10);
                                    LocalDate dataDate = LocalDate.parse(day);

                                    if (dataDate.equals(date)) {
                                        double open = item.getDouble("open");
                                        double close = item.getDouble("close");
                                        double high = item.getDouble("high");
                                        double low = item.getDouble("low");
                                        double volume = item.getDouble("volume");

                                        // 检查是否已存在
                                        LambdaQueryWrapper<StockDaily> wrapper = new LambdaQueryWrapper<>();
                                        wrapper.eq(StockDaily::getStockCode, stockCode)
                                                .eq(StockDaily::getTradeDate, date);
                                        StockDaily existing = stockDailyService.getOne(wrapper);

                                        if (existing != null) {
                                            existing.setOpenPrice(BigDecimal.valueOf(open));
                                            existing.setClosePrice(BigDecimal.valueOf(close));
                                            existing.setHighPrice(BigDecimal.valueOf(high));
                                            existing.setLowPrice(BigDecimal.valueOf(low));
                                            existing.setVolume((long) volume);
                                            stockDailyService.updateById(existing);
                                        } else {
                                            StockDaily daily = new StockDaily();
                                            daily.setStockCode(stockCode);
                                            daily.setTradeDate(date);
                                            daily.setOpenPrice(BigDecimal.valueOf(open));
                                            daily.setClosePrice(BigDecimal.valueOf(close));
                                            daily.setHighPrice(BigDecimal.valueOf(high));
                                            daily.setLowPrice(BigDecimal.valueOf(low));
                                            daily.setVolume((long) volume);
                                            stockDailyService.save(daily);
                                        }
                                        daySuccess++;
                                        break;
                                    }
                                }
                            }
                        }
                    }

                    Thread.sleep(REQUEST_INTERVAL);
                } catch (Exception e) {
                    dayFail++;
                }
            }

            log.info("{} 数据处理完成: 成功 {} 条, 失败 {} 条", date, daySuccess, dayFail);
            totalSuccess += daySuccess;
            totalFail += dayFail;
        }

        log.info("=== 近 {} 天行情数据补充完成: 成功 {} 条, 失败 {} 条 ===", days, totalSuccess, totalFail);
    }

}
