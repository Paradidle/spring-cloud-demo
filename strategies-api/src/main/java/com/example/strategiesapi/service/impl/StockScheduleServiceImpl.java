package com.example.strategiesapi.service.impl;

import cn.hutool.http.HttpRequest;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.strategiesapi.entity.StockBasic;
import com.example.strategiesapi.entity.StockDaily;
import com.example.strategiesapi.service.IStockBasicService;
import com.example.strategiesapi.service.IStockDailyService;
import com.example.strategiesapi.service.IStockScheduleService;

/**
 * 股票数据定时任务 Service 实现类
 *
 * @author system
 * @since 2026-05-02
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class StockScheduleServiceImpl implements IStockScheduleService {

    private final IStockBasicService stockBasicService;
    private final IStockDailyService stockDailyService;

    private static final String SINA_STOCK_MINUTE_API = "http://money.finance.sina.com.cn/quotes_service/api/json_v2.php/CN_MarketData.getKLineData";

    @Override
    @Scheduled(cron = "0 */5 9-15 * * MON-FRI")
    @Transactional(rollbackFor = Exception.class)
    public void fetchMinuteData() {
        log.info("开始定时拉取分时数据");
        
        LambdaQueryWrapper<StockBasic> wrapper = new LambdaQueryWrapper<>();
        List<StockBasic> stockList = stockBasicService.list(wrapper);
        
        log.info("共需要处理{}只股票", stockList.size());
        
        int successCount = 0;
        int failCount = 0;
        
        for (StockBasic stock : stockList) {
            try {
                String url = SINA_STOCK_MINUTE_API + "?symbol=" + stock.getMarket() + stock.getStockCode() 
                        + "&scale=5&ma=no&datalen=48";
                
                String result = HttpRequest.get(url)
                        .header("Referer", "https://finance.sina.com.cn")
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                        .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                        .timeout(10000)
                        .execute()
                        .body();
                
                if (result == null || result.isEmpty()) {
                    failCount++;
                    continue;
                }
                
                int start = result.indexOf("[");
                int end = result.lastIndexOf("]");
                if (start == -1 || end == -1) {
                    failCount++;
                    continue;
                }
                
                String jsonStr = result.substring(start, end + 1);
                JSONArray jsonArray = JSON.parseArray(jsonStr);
                
                if (jsonArray != null && !jsonArray.isEmpty()) {
                    LocalDate today = LocalDate.now();
                    
                    LambdaQueryWrapper<StockDaily> queryWrapper = new LambdaQueryWrapper<>();
                    queryWrapper.eq(StockDaily::getStockCode, stock.getStockCode())
                               .eq(StockDaily::getTradeDate, today);
                    StockDaily existDaily = stockDailyService.getOne(queryWrapper);
                    
                    StockDaily daily = existDaily != null ? existDaily : new StockDaily();
                    daily.setStockCode(stock.getStockCode());
                    daily.setTradeDate(today);
                    daily.setMinuteData(jsonStr);
                    
                    if (existDaily != null) {
                        stockDailyService.updateById(daily);
                    } else {
                        stockDailyService.save(daily);
                    }
                    
                    successCount++;
                } else {
                    failCount++;
                }
                
                Thread.sleep(50);
            } catch (Exception e) {
                log.error("处理股票{}分时数据失败", stock.getStockCode(), e);
                failCount++;
            }
        }
        
        log.info("分时数据拉取完成: 成功{}条, 失败{}条", successCount, failCount);
    }
}
