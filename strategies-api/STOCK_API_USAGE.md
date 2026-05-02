# 股票数据API使用说明

## API接口

### 1. 获取股票列表
**接口**: `GET /api/strategies/stocks`

**说明**: 获取沪深两市主板股票列表，已过滤ST股票、北交所和创业板股票

**返回示例**:
```json
["600000", "600001", "000001", "000002"]
```

### 2. 获取单只股票详情
**接口**: `GET /api/strategies/stock/{code}`

**参数**: 
- `code`: 股票代码，如 `600519`

**返回示例**:
```json
{
  "code": "600519",
  "name": "贵州茅台",
  "highPrice": 1800.00,
  "lowPrice": 1750.00,
  "openPrice": 1780.00,
  "trendData": [1780.0, 1785.0, 1790.0, ...]
}
```

### 3. 获取指定日期的股票详情
**接口**: `GET /api/strategies/stock/{code}/{date}`

**参数**: 
- `code`: 股票代码，如 `600519`
- `date`: 日期，格式 `YYYYMMDD`，如 `20240101`

**返回示例**:
```json
{
  "code": "600519",
  "name": "贵州茅台",
  "highPrice": 1800.00,
  "lowPrice": 1750.00,
  "openPrice": 1780.00,
  "trendData": []
}
```

**注意**: 历史数据不包含实时走势数据

### 4. 初始化历史数据
**接口**: `POST /api/strategies/init-history`

**说明**: 初始化近1年的所有股票详情数据，方便后续写入数据库

**返回**: `历史数据初始化完成`

## 数据源说明

使用东方财富网API获取股票数据：
- 股票列表接口：`push2.eastmoney.com`
- 个股详情接口：`push2.eastmoney.com`
- 分时数据接口：`push2.eastmoney.com`

## 注意事项

1. 请求频率控制：代码中已添加100ms延迟，避免请求过于频繁
2. 数据过滤：自动过滤ST股票、北交所(8开头)和创业板(300开头)股票
3. 数据库集成：`initHistoricalData()`方法中预留了数据库写入接口，可根据需要实现
