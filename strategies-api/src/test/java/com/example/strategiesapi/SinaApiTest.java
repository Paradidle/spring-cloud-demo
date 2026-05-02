package com.example.strategiesapi;

import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

public class SinaApiTest {
    
    public static void main(String[] args) {
        testStockList();
        testStockDetail();
        testStockHistory();
    }
    
    private static void testStockList() {
        System.out.println("=== 测试股票列表 ===");
        try {
            String url = "http://vip.stock.finance.sina.com.cn/quotes_service/api/json_v2.php/Market_Center.getHQNodeData";
            String result = HttpUtil.get(url + "?page=1&num=5&sort=symbol&asc=1&node=sh_a&symbol=");
            System.out.println("返回数据: " + result);
            
            JSONArray jsonArray = JSON.parseArray(result);
            if (jsonArray != null && !jsonArray.isEmpty()) {
                for (int i = 0; i < Math.min(jsonArray.size(), 3); i++) {
                    JSONObject item = jsonArray.getJSONObject(i);
                    System.out.println("代码: " + item.getString("code") + ", 名称: " + item.getString("name"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println();
    }
    
    private static void testStockDetail() {
        System.out.println("=== 测试股票详情 (600519 贵州茅台) ===");
        try {
            String url = "http://hq.sinajs.cn/list=sh600519";
            String result = HttpUtil.get(url);
            System.out.println("返回数据: " + result);
            
            String[] parts = result.split("=");
            if (parts.length >= 2) {
                String data = parts[1].replace("\"", "").replace(";", "");
                String[] fields = data.split(",");
                System.out.println("股票名称: " + fields[0]);
                System.out.println("开盘价: " + fields[1]);
                System.out.println("最高价: " + fields[4]);
                System.out.println("最低价: " + fields[5]);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println();
    }
    
    private static void testStockHistory() {
        System.out.println("=== 测试历史数据 (600519 2024-01-02) ===");
        try {
            String url = "http://money.finance.sina.com.cn/quotes_service/api/json_v2.php/CN_MarketData.getKLineData";
            String result = HttpUtil.get(url + "?symbol=sh600519&scale=240&ma=no&datalen=1&d1=2024-01-02");
            System.out.println("返回数据: " + result);
            
            JSONArray jsonArray = JSON.parseArray(result);
            if (jsonArray != null && !jsonArray.isEmpty()) {
                JSONObject data = jsonArray.getJSONObject(0);
                System.out.println("股票名称: " + data.getString("name"));
                System.out.println("开盘价: " + data.getDouble("open"));
                System.out.println("最高价: " + data.getDouble("high"));
                System.out.println("最低价: " + data.getDouble("low"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println();
    }
}
