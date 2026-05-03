package com.example.strategiesapi.service.impl;

import cn.hutool.http.HttpRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
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
            // 东方财富全量A股 API - 沪A、深A、创业板、科创板、北交所
            String[] markets = {"m:1+t:23", "m:0+t:6", "m:0+t:80", "m:1+t:8", "m:0+t:81"};
            // m:1+t:23 = 上海A股
            // m:0+t:6 = 深圳主板
            // m:0+t:80 = 创业板
            // m:1+t:8 = 科创板
            // m:0+t:81 = 北交所
            
            for (String market : markets) {
                int page = 1;
                int totalPages = 1;
                
                while (page <= totalPages && page <= 100) {  // 每市场最多100页
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
                                if (page == 1) {
                                    long total = data.getLong("total") != null ? data.getLong("total") : 0;
                                    totalPages = (int) Math.ceil((double) total / 500);
                                    log.info("市场 {} 预计 {} 页，共 {} 只股票", market, totalPages, total);
                                }
                                
                                JSONArray stocks = data.getJSONArray("diff");
                                if (stocks != null && !stocks.isEmpty()) {
                                    for (int i = 0; i < stocks.size(); i++) {
                                        JSONObject stock = stocks.getJSONObject(i);
                                        String code = stock.getString("f12");
                                        String name = stock.getString("f14");
                                        
                                        if (code != null && name != null && 
                                            !name.contains("ST") && !name.contains("*") &&
                                            !name.contains("N ")) {
                                            stockCodes.add(code);
                                        }
                                    }
                                    log.info("市场 {} 第 {} 页，获取 {} 只股票，累计 {} 只", market, page, stocks.size(), stockCodes.size());
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
    @Transactional(rollbackFor = Exception.class)
    public void initHistoricalData() {
        log.info("使用东方财富API开始初始化股票数据");
        
        List<String> stockList = getStockList();
        log.info("共需要处理 {} 只股票", stockList.size());
        
        int basicCount = 0;
        int dailyCount = 0;
        int minuteCount = 0;
        
        for (int i = 0; i < stockList.size(); i++) {
            String stockCode = stockList.get(i);
            try {
                String prefix = stockCode.startsWith("6") ? "sh" : "sz";
                
                // 获取股票基本信息
                LambdaQueryWrapper<StockBasic> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(StockBasic::getStockCode, stockCode);
                StockBasic existBasic = stockBasicService.getOne(wrapper);
                
                if (existBasic == null) {
                    String url = SINA_STOCK_DETAIL_API + prefix + stockCode;
                    String result = httpGet(url);
                    if (result != null && !result.isEmpty()) {
                        String[] parts = result.split("=");
                        if (parts.length >= 2) {
                            String data = parts[1].replace("\"", "").replace(";", "");
                            String[] fields = data.split(",");
                            if (fields.length >= 33) {
                                StockBasic basic = new StockBasic();
                                basic.setStockCode(stockCode);
                                basic.setMarket(prefix);
                                basic.setStockName(fields[0]);
                                stockBasicService.save(basic);
                                basicCount++;
                                log.info("新增股票: {} - {}", stockCode, fields[0]);
                            }
                        }
                    }
                    Thread.sleep(REQUEST_INTERVAL);
                }
                
                // 获取日线数据和分时数据
                StockInfo detail = getStockDetail(stockCode);
                if (detail != null) {
                    LocalDate today = LocalDate.now();
                    
                    LambdaQueryWrapper<StockDaily> wrapper2 = new LambdaQueryWrapper<>();
                    wrapper2.eq(StockDaily::getStockCode, stockCode)
                            .eq(StockDaily::getTradeDate, today);
                    StockDaily existDaily = stockDailyService.getOne(wrapper2);
                    
                    StockDaily daily = existDaily != null ? existDaily : new StockDaily();
                    daily.setStockCode(stockCode);
                    daily.setTradeDate(today);
                    daily.setOpenPrice(BigDecimal.valueOf(detail.getOpenPrice()));
                    daily.setHighPrice(BigDecimal.valueOf(detail.getHighPrice()));
                    daily.setLowPrice(BigDecimal.valueOf(detail.getLowPrice()));
                    
                    if (detail.getTrendData() != null && !detail.getTrendData().isEmpty()) {
                        daily.setMinuteData(JSON.toJSONString(detail.getTrendData()));
                        minuteCount++;
                    }
                    
                    if (existDaily != null) {
                        stockDailyService.updateById(daily);
                    } else {
                        stockDailyService.save(daily);
                    }
                    dailyCount++;
                }
                
                // 打印进度
                if ((i + 1) % 10 == 0) {
                    log.info("进度: {}/{} ({}%), 累计: 基本信息{}条, 日线{}条, 分时{}条", 
                             i + 1, stockList.size(), 
                             (i + 1) * 100 / stockList.size(),
                             basicCount, dailyCount, minuteCount);
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

}
