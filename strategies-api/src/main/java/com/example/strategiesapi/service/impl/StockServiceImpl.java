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

    private static final String SINA_STOCK_LIST_API = "http://vip.stock.finance.sina.com.cn/quotes_service/api/json_v2.php/Market_Center.getHQNodeData";
    private static final String SINA_STOCK_DETAIL_API = "http://hq.sinajs.cn/list=";
    private static final String SINA_STOCK_HISTORY_API = "http://money.finance.sina.com.cn/quotes_service/api/json_v2.php/CN_MarketData.getKLineData";

    private String httpGet(String url) {
        return HttpRequest.get(url)
                .header("Referer", "https://finance.sina.com.cn")
                .timeout(10000)
                .execute()
                .body();
    }

    @Override
    public List<String> getStockList() {
        log.info("获取沪深主板股票列表");
        List<String> stockCodes = new ArrayList<>();
        
        try {
            List<String> shStocks = fetchSinaStocksByPage("sh_a", 1, 80);
            stockCodes.addAll(shStocks);
            
            List<String> szStocks = fetchSinaStocksByPage("sz_a", 1, 80);
            stockCodes.addAll(szStocks);
            
            log.info("获取到{}只主板股票", stockCodes.size());
        } catch (Exception e) {
            log.error("获取股票列表失败", e);
        }
        
        return stockCodes;
    }

    private List<String> fetchSinaStocksByPage(String node, int page, int count) {
        List<String> stocks = new ArrayList<>();
        try {
            String url = SINA_STOCK_LIST_API + "?page=" + page + "&num=" + count + "&sort=symbol&asc=1&node=" + node + "&symbol=";
            String result = httpGet(url);
            
            JSONArray jsonArray = JSON.parseArray(result);
            
            if (jsonArray != null) {
                for (int i = 0; i < jsonArray.size(); i++) {
                    JSONObject item = jsonArray.getJSONObject(i);
                    String code = item.getString("code");
                    String name = item.getString("name");
                    
                    if (!name.contains("ST") && !name.contains("*")) {
                        stocks.add(code);
                    }
                }
            }
        } catch (Exception e) {
            log.error("获取{}股票列表失败", node, e);
        }
        return stocks;
    }

    @Override
    public StockInfo getStockDetail(String stockCode) {
        log.info("获取股票详情: {}", stockCode);
        
        try {
            String prefix = stockCode.startsWith("6") ? "sh" : "sz";
            String url = SINA_STOCK_DETAIL_API + prefix + stockCode;
            String result = httpGet(url);
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
        log.info("开始初始化股票数据");
        
        List<String> stockList = getStockList();
        log.info("共需要处理{}只股票", stockList.size());
        
        int basicCount = 0;
        int dailyCount = 0;
        
        for (String stockCode : stockList) {
            try {
                String prefix = stockCode.startsWith("6") ? "sh" : "sz";
                
                StockBasic basic = new StockBasic();
                basic.setStockCode(stockCode);
                basic.setMarket(prefix);
                
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
                                basic.setStockName(fields[0]);
                                stockBasicService.save(basic);
                                basicCount++;
                            }
                        }
                    }
                }
                
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
                    }
                    
                    if (existDaily != null) {
                        stockDailyService.updateById(daily);
                    } else {
                        stockDailyService.save(daily);
                    }
                    dailyCount++;
                }
                
                if ((basicCount + dailyCount) % 50 == 0) {
                    log.info("已处理: 基本信息{}条, 日线数据{}条", basicCount, dailyCount);
                }
                
                Thread.sleep(100);
            } catch (Exception e) {
                log.error("处理股票{}时出错", stockCode, e);
            }
        }
        
        log.info("数据初始化完成: 基本信息{}条, 日线数据{}条", basicCount, dailyCount);
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
                log.info("获取到{}条分时数据", prices.size());
            }
        } catch (Exception e) {
            log.error("获取分时数据失败: {}{}", prefix, stockCode, e);
        }
        return prices;
    }

}