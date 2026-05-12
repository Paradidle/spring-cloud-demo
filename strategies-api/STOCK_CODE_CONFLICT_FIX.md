# 股票代码冲突问题修复方案

## 问题描述

当前系统存在股票代码冲突问题：
- `sh000001`（上证指数）和 `sz000001`（平安银行）在数据库中都被存储为 `stock_code = '000001'`
- 导致数据混淆，无法正确区分不同市场的相同代码股票/指数

## 解决方案

采用**方案2**：在 `stock_daily` 表中添加 `market` 字段，使用 `market + stock_code` 组合确保唯一性。

### 优势
1. **保持现有架构**：不需要创建新的表结构
2. **数据一致性**：通过 market + stock_code 组合确保唯一性
3. **向后兼容**：可以逐步迁移现有数据
4. **性能良好**：只需修改索引和查询条件

## 实施步骤

### 1. 数据库表结构修改

执行SQL脚本：`strategies-api/sql/fix_stock_code_conflict.sql`

主要变更：
- 在 `stock_daily` 表中添加 `market` 字段（VARCHAR(2)）
- 删除原有的 `uk_stock_date` 唯一索引
- 创建新的 `uk_market_stock_date` 唯一索引（market, stock_code, trade_date）
- 添加 `idx_market` 索引以提高查询性能
- 更新现有数据的 market 字段（根据 stock_code 推断）
- 更新视图 `v_latest_stock_data` 的 JOIN 条件

### 2. 实体类修改

**文件**: `StockDaily.java`

添加字段：
```java
/**
 * 市场标识: sh-上海, sz-深圳
 */
private String market;
```

### 3. 服务层修改

**文件**: `StockServiceImpl.java`

#### 3.1 添加辅助方法
```java
/**
 * 根据股票代码判断市场标识
 * @param stockCode 股票代码
 * @return sh-上海, sz-深圳
 */
private String determineMarket(String stockCode) {
    if (stockCode == null || stockCode.isEmpty()) {
        return "sh";
    }
    // 上海证券交易所：6开头（包括600、601、603、605、688等）
    if (stockCode.startsWith("6") || stockCode.startsWith("9")) {
        return "sh";
    }
    // 深圳证券交易所：0开头（000、001、002等）、1开头、2开头、3开头（创业板）
    if (stockCode.startsWith("0") || stockCode.startsWith("1") || 
        stockCode.startsWith("2") || stockCode.startsWith("3")) {
        return "sz";
    }
    // 默认返回sh
    return "sh";
}
```

#### 3.2 修改数据插入逻辑

在所有插入 `StockDaily` 的地方添加 market 字段：

**示例1**: `initHistoricalData` 方法
```java
// 确定市场标识
String market = determineMarket(stockCode);

LambdaQueryWrapper<StockDaily> wrapper2 = new LambdaQueryWrapper<>();
wrapper2.eq(StockDaily::getStockCode, stockCode)
        .eq(StockDaily::getMarket, market)
        .eq(StockDaily::getTradeDate, tradeDate);
StockDaily existDaily = stockDailyService.getOne(wrapper2);

StockDaily daily = existDaily != null ? existDaily : new StockDaily();
daily.setStockCode(stockCode);
daily.setMarket(market);  // 新增
daily.setTradeDate(tradeDate);
// ... 其他字段
```

**示例2**: `fetchAndSaveDailyData` 方法
```java
// 确定市场标识
String market = determineMarket(stockCode);

LambdaQueryWrapper<StockDaily> wrapper = new LambdaQueryWrapper<>();
wrapper.eq(StockDaily::getStockCode, stockCode)
        .eq(StockDaily::getMarket, market)  // 新增
        .ge(StockDaily::getTradeDate, startDate)
        .le(StockDaily::getTradeDate, endDate);

// 插入时
StockDaily daily = new StockDaily();
daily.setStockCode(stockCode);
daily.setMarket(market);  // 新增
// ... 其他字段
```

#### 3.3 修改的位置清单

已修改以下方法中的数据查询和插入逻辑：
1. `initHistoricalData()` - 历史数据初始化
2. `fetchAndSaveMinuteKlineData()` - 分时K线数据获取
3. `fetchAndSaveDailyData()` - 日线数据获取
4. `fillRecentData()` - 补充近期数据
5. `fillAllHistoryData()` - 补充全量历史数据

### 4. SQL脚本和视图更新

**文件**: `stock_tables.sql`

- 更新视图 `v_latest_stock_data` 的 JOIN 条件，添加 `AND sb.market = sd.market`
- 更新索引说明文档
- 更新使用示例，展示如何在查询中使用 market 字段

## 数据迁移

### 自动迁移
执行 `fix_stock_code_conflict.sql` 脚本时会自动：
1. 为现有数据设置 market 字段（根据 stock_code 推断）
2. 更新唯一索引
3. 验证数据完整性

### 手动验证
执行以下SQL验证数据：
```sql
-- 检查各市场的数据分布
SELECT 
    market,
    COUNT(*) as record_count,
    COUNT(DISTINCT stock_code) as unique_stocks
FROM stock_daily 
GROUP BY market;

-- 检查是否有重复记录
SELECT market, stock_code, trade_date, COUNT(*) as cnt
FROM stock_daily
GROUP BY market, stock_code, trade_date
HAVING cnt > 1;
```

## 测试验证

### 1. 单元测试
- 测试 `determineMarket()` 方法的正确性
- 测试不同市场股票的查询和插入

### 2. 集成测试
- 执行数据初始化任务
- 验证 sh000001（上证指数）和 sz000001（平安银行）的数据是否正确分离
- 检查唯一索引是否生效

### 3. 性能测试
- 对比修改前后的查询性能
- 验证新索引的效果

## 注意事项

1. **执行顺序**：
   - 先执行数据库迁移脚本
   - 再部署代码变更

2. **数据备份**：
   - 执行迁移前务必备份数据库

3. **停机时间**：
   - 建议在低峰期执行迁移
   - 大表添加字段和重建索引可能需要较长时间

4. **兼容性**：
   - 确保所有调用 StockDaily 的地方都已更新
   - 检查是否有硬编码的SQL语句需要修改

## 回滚方案

如果出现问题，可以：
1. 恢复数据库备份
2. 回滚代码到之前的版本
3. 重新评估解决方案

## 后续优化建议

1. **API接口优化**：
   - 考虑在API层面支持 market 参数
   - 提供按市场筛选的功能

2. **前端展示**：
   - 在股票列表中显示市场标识
   - 支持按市场过滤

3. **数据质量**：
   - 添加数据校验逻辑，确保 market 字段正确
   - 定期运行数据一致性检查

## 总结

本次修改通过在 `stock_daily` 表中添加 `market` 字段，成功解决了股票代码冲突问题。
修改后的系统能够正确区分不同市场的相同代码股票/指数，保证了数据的准确性和一致性。
