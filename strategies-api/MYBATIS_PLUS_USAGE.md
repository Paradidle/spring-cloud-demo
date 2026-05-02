# MyBatis-Plus 使用示例

## 已创建的文件结构

```
src/main/java/com/example/strategiesapi/
├── entity/
│   ├── StockBasic.java          # 股票基本信息实体
│   └── StockDaily.java          # 股票日线数据实体
├── mapper/
│   ├── StockBasicMapper.java    # 股票基本信息Mapper
│   └── StockDailyMapper.java    # 股票日线数据Mapper
└── service/
    ├── IStockBasicService.java           # 股票基本信息Service接口
    ├── IStockDailyService.java           # 股票日线数据Service接口
    └── impl/
        ├── StockBasicServiceImpl.java    # 股票基本信息Service实现
        └── StockDailyServiceImpl.java    # 股票日线数据Service实现
```

## 基础CRUD操作示例

### 1. 股票基本信息 (StockBasic)

#### 新增
```java
@Autowired
private IStockBasicService stockBasicService;

// 单个新增
StockBasic stock = new StockBasic();
stock.setStockCode("600519");
stock.setMarket("sh");
stock.setStockName("贵州茅台");
stockBasicService.save(stock);

// 批量新增
List<StockBasic> stocks = new ArrayList<>();
// ... 添加数据
stockBasicService.saveBatch(stocks);
```

#### 查询
```java
// 根据ID查询
StockBasic stock = stockBasicService.getById(1L);

// 根据股票代码查询
LambdaQueryWrapper<StockBasic> wrapper = new LambdaQueryWrapper<>();
wrapper.eq(StockBasic::getStockCode, "600519");
StockBasic stock = stockBasicService.getOne(wrapper);

// 查询所有沪市股票
wrapper = new LambdaQueryWrapper<>();
wrapper.eq(StockBasic::getMarket, "sh");
List<StockBasic> shStocks = stockBasicService.list(wrapper);

// 分页查询
Page<StockBasic> page = new Page<>(1, 10);
IPage<StockBasic> result = stockBasicService.page(page);
```

#### 更新
```java
// 根据ID更新
StockBasic stock = stockBasicService.getById(1L);
stock.setStockName("新名称");
stockBasicService.updateById(stock);

// 条件更新
LambdaUpdateWrapper<StockBasic> updateWrapper = new LambdaUpdateWrapper<>();
updateWrapper.eq(StockBasic::getStockCode, "600519")
             .set(StockBasic::getStockName, "贵州茅台");
stockBasicService.update(updateWrapper);
```

#### 删除
```java
// 根据ID删除
stockBasicService.removeById(1L);

// 条件删除
LambdaQueryWrapper<StockBasic> wrapper = new LambdaQueryWrapper<>();
wrapper.eq(StockBasic::getStockCode, "600519");
stockBasicService.remove(wrapper);
```

### 2. 股票日线数据 (StockDaily)

#### 新增
```java
@Autowired
private IStockDailyService stockDailyService;

StockDaily daily = new StockDaily();
daily.setStockCode("600519");
daily.setTradeDate(LocalDate.of(2024, 1, 2));
daily.setOpenPrice(new BigDecimal("1780.00"));
daily.setHighPrice(new BigDecimal("1800.00"));
daily.setLowPrice(new BigDecimal("1750.00"));
daily.setClosePrice(new BigDecimal("1790.00"));
daily.setMinuteData("[1780.0,1785.0,1790.0]");
stockDailyService.save(daily);
```

#### 查询
```java
// 查询某股票的历史数据
LambdaQueryWrapper<StockDaily> wrapper = new LambdaQueryWrapper<>();
wrapper.eq(StockDaily::getStockCode, "600519")
       .orderByDesc(StockDaily::getTradeDate);
List<StockDaily> history = stockDailyService.list(wrapper);

// 查询某天的所有股票
wrapper = new LambdaQueryWrapper<>();
wrapper.eq(StockDaily::getTradeDate, LocalDate.of(2024, 1, 2));
List<StockDaily> dailyData = stockDailyService.list(wrapper);

// 查询日期范围
wrapper = new LambdaQueryWrapper<>();
wrapper.eq(StockDaily::getStockCode, "600519")
       .between(StockDaily::getTradeDate, 
                LocalDate.of(2024, 1, 1), 
                LocalDate.of(2024, 12, 31))
       .orderByAsc(StockDaily::getTradeDate);
List<StockDaily> rangeData = stockDailyService.list(wrapper);
```

#### 更新（UPSERT）
```java
// 使用saveOrUpdate实现存在则更新，不存在则插入
StockDaily daily = new StockDaily();
daily.setStockCode("600519");
daily.setTradeDate(LocalDate.of(2024, 1, 2));
daily.setOpenPrice(new BigDecimal("1780.00"));
// ... 设置其他字段

LambdaQueryWrapper<StockDaily> wrapper = new LambdaQueryWrapper<>();
wrapper.eq(StockDaily::getStockCode, "600519")
       .eq(StockDaily::getTradeDate, LocalDate.of(2024, 1, 2));
stockDailyService.saveOrUpdate(daily, wrapper);
```

## 常用查询场景

### 1. 获取最新交易日的股票数据
```java
// 先查询最大日期
LambdaQueryWrapper<StockDaily> wrapper = new LambdaQueryWrapper<>();
wrapper.select(StockDaily::getTradeDate)
       .orderByDesc(StockDaily::getTradeDate)
       .last("LIMIT 1");
StockDaily latest = stockDailyService.getOne(wrapper);
LocalDate latestDate = latest.getTradeDate();

// 再查询该日期的所有数据
wrapper = new LambdaQueryWrapper<>();
wrapper.eq(StockDaily::getTradeDate, latestDate);
List<StockDaily> latestData = stockDailyService.list(wrapper);
```

### 2. 统计某股票的涨跌幅
```java
LambdaQueryWrapper<StockDaily> wrapper = new LambdaQueryWrapper<>();
wrapper.eq(StockDaily::getStockCode, "600519")
       .orderByAsc(StockDaily::getTradeDate);
List<StockDaily> history = stockDailyService.list(wrapper);

if (history.size() >= 2) {
    BigDecimal yesterdayClose = history.get(history.size() - 2).getClosePrice();
    BigDecimal todayClose = history.get(history.size() - 1).getClosePrice();
    BigDecimal change = todayClose.subtract(yesterdayClose)
                                  .divide(yesterdayClose, 4, RoundingMode.HALF_UP)
                                  .multiply(new BigDecimal("100"));
    System.out.println("涨跌幅: " + change + "%");
}
```

### 3. 查询主力资金净流入为正的的股票
```java
LambdaQueryWrapper<StockDaily> wrapper = new LambdaQueryWrapper<>();
wrapper.eq(StockDaily::getTradeDate, LocalDate.now())
       .gt(StockDaily::getMainNetInflow, BigDecimal.ZERO)
       .orderByDesc(StockDaily::getMainNetInflow);
List<StockDaily> positiveFlow = stockDailyService.list(wrapper);
```

## 注意事项

1. **数据库配置**: 确保在 `application.properties` 中正确配置MySQL连接信息
2. **建表**: 先执行 `sql/stock_tables.sql` 创建表结构
3. **时区**: 数据库URL中已设置 `serverTimezone=Asia/Shanghai`
4. **驼峰转换**: 已配置 `map-underscore-to-camel-case=true`，自动转换下划线到驼峰
5. **日志**: 已开启SQL日志输出，方便调试
