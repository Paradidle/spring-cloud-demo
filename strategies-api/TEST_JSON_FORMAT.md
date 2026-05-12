# 测试批量分析功能

## 1. 清理旧数据（可选）

如果要重新开始测试，先删除旧的结果文件：

```powershell
# PowerShell
Remove-Item -Recurse -Force stock_analysis_results -ErrorAction SilentlyContinue
```

## 2. 启动服务

确保后端服务已启动。

## 3. 测试单个股票分析

先测试一个股票，查看返回的格式：

```bash
GET http://localhost:8083/api/strategies/analyze-box/600000
```

预期返回格式化的文本：
```
【股票 600000 箱体分析结果】

箱顶价格: 10.78 元
箱底价格: 9.6 元
当前价格: 10.77 元
相对位置R: 0.95
位置判断: 顶部

操作建议: 当前价格处于箱体顶部，建议关注压力位
```

## 4. 测试批量分析（少量股票）

为了快速测试，可以先修改代码只分析前5只股票：

在 `DashScopeAnalysisServiceImpl.java` 第115行后添加：
```java
// 测试用：只分析前5只股票
stocks = stocks.stream().limit(5).collect(Collectors.toList());
```

然后调用：
```bash
POST http://localhost:8083/api/strategies/analyze-all-stocks
```

## 5. 检查生成的JSON文件

分析完成后，查看 `stock_analysis_results` 目录下的JSON文件：

```powershell
# 查看目录内容
Get-ChildItem stock_analysis_results\*.json | Select-Object -First 1 | Get-Content
```

**正确的JSON格式应该是**：
```json
{
  "stockCode": "600000",
  "stockName": "浦发银行",
  "boxTop": 10.78,
  "boxBottom": 9.6,
  "currentPrice": 10.77,
  "R": 0.95,
  "position": "顶部",
  "advice": "当前价格处于箱体顶部，建议关注压力位",
  "analysisResult": "【股票 600000 箱体分析结果】\n\n箱顶价格: 10.78 元\n..."
}
```

**关键点**：
- ✅ `boxTop`、`boxBottom`、`currentPrice`、`R` 应该是**数字类型**（没有引号）
- ✅ `position`、`advice` 应该是**字符串类型**（有引号）
- ✅ `analysisResult` 是格式化后的完整文本

## 6. 导出生成Excel

```bash
POST http://localhost:8083/api/strategies/export-excel
```

## 7. 检查Excel文件

打开 `stock_analysis_results/stock_box_analysis_YYYY-MM-DD.xlsx`，确认：

| 列 | 预期值 | 常见问题 |
|----|--------|---------|
| 股票代码 | 600000 | ✅ 正常显示 |
| 股票名称 | 浦发银行 | ✅ 正常显示 |
| 箱顶价格 | 10.78 | ❌ 之前显示为0 |
| 箱底价格 | 9.6 | ❌ 之前显示为0 |
| 当前价格 | 10.77 | ❌ 之前显示为0 |
| R值 | 0.95 | ❌ 之前显示为0 |
| 位置判断 | 顶部 | ❌ 之前显示为空 |
| 操作建议 | 当前价格... | ❌ 之前显示为空 |

## 8. 调试技巧

### 如果Excel中还是显示0或空：

1. **检查JSON文件格式**：
   ```powershell
   cat stock_analysis_results\600000.json
   ```
   确认数值字段确实是数字类型，不是字符串。

2. **查看日志**：
   查找是否有警告信息：
   ```
   WARN - 无法解析股票 XXXXX 的分析结果为JSON
   ```

3. **手动测试JSON解析**：
   创建一个测试文件 `test_json.java`：
   ```java
   import com.alibaba.fastjson.JSON;
   import com.alibaba.fastjson.JSONObject;
   
   public class TestJson {
       public static void main(String[] args) {
           String jsonStr = "{\"boxTop\":10.78,\"boxBottom\":9.6,\"currentPrice\":10.77,\"R\":0.95}";
           JSONObject obj = JSON.parseObject(jsonStr);
           System.out.println("boxTop: " + obj.getDoubleValue("boxTop"));
           System.out.println("boxBottom: " + obj.getDoubleValue("boxBottom"));
       }
   }
   ```

## 9. 完整测试（所有股票）

移除测试限制代码后，运行完整分析：

```bash
POST http://localhost:8083/api/strategies/analyze-all-stocks
```

预计时间：30分钟 - 2小时（取决于API响应速度）

## 常见问题

### Q1: JSON文件中数值字段显示为null？

**原因**：AI返回的格式不正确，或者JSON提取失败。

**解决**：
1. 检查 `formatAnalysisResult` 方法是否能正确解析AI返回的JSON
2. 查看日志中的错误信息
3. 手动调用单个股票分析接口，查看原始返回

### Q2: Excel中仍然显示0？

**原因**：JSON读取时类型转换失败。

**解决**：
1. 打开JSON文件，确认数值字段是数字类型（没有引号）
2. 检查 `loadAllCompletedResults` 方法中的解析逻辑
3. 添加调试日志打印解析后的值

### Q3: 如何查看AI的原始返回？

在 `callDashScopeAPI` 方法中添加日志：
```java
String content = msg.getString("content");
log.debug("AI原始返回: {}", content);
return content;
```
