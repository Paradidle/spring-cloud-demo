# 股票数据系统 - 发布说明

## 📦 版本信息
- **版本**: v1.0.0
- **发布日期**: 2026-05-02
- **JDK版本**: 1.8+
- **Spring Boot**: 2.2.7.RELEASE
- **MyBatis-Plus**: 3.5.3.1

## ✨ 核心功能

### 1. 股票数据采集
- ✅ 自动获取沪深主板股票列表（过滤ST、创业板、北交所）
- ✅ 实时获取股票基本信息（代码、名称、市场）
- ✅ 获取股票实时行情（开盘价、最高价、最低价）
- ✅ 获取5分钟K线分时数据

### 2. 数据存储
- ✅ MySQL数据库持久化存储
- ✅ 智能去重（同一天同一股票只存一条记录）
- ✅ 支持增量更新和全量初始化

### 3. 定时任务
- ✅ 交易时段自动更新（周一至周五 9:00-15:00）
- ✅ 每5分钟拉取最新分时数据
- ✅ 可手动触发数据更新

### 4. RESTful API
- ✅ 股票列表查询
- ✅ 股票详情查询（含分时数据）
- ✅ 历史数据查询
- ✅ 数据初始化接口

## 📂 项目结构

```
strategies-api/
├── sql/
│   └── stock_tables.sql              # 数据库建表脚本
├── src/main/
│   ├── java/com/example/strategiesapi/
│   │   ├── controller/
│   │   │   └── StrategiesController.java    # REST控制器
│   │   ├── entity/
│   │   │   ├── StockBasic.java              # 股票基本信息实体
│   │   │   └── StockDaily.java              # 股票日线数据实体
│   │   ├── mapper/
│   │   │   ├── StockBasicMapper.java        # 基本信息Mapper
│   │   │   └── StockDailyMapper.java        # 日线数据Mapper
│   │   ├── service/
│   │   │   ├── StockService.java            # 股票服务接口
│   │   │   ├── IStockScheduleService.java   # 定时任务接口
│   │   │   └── impl/
│   │   │       ├── StockServiceImpl.java           # 股票服务实现
│   │   │       └── StockScheduleServiceImpl.java   # 定时任务实现
│   │   └── StrategiesApiApplication.java    # 启动类
│   └── resources/
│       ├── mapper/
│       │   ├── StockBasicMapper.xml         # Mapper XML
│       │   └── StockDailyMapper.xml
│       └── application.properties           # 配置文件
├── pom.xml                                  # Maven配置
├── RELEASE_CHECKLIST.md                     # 发布检查清单
├── STOCK_DATA_USAGE.md                      # 使用文档
└── MYBATIS_PLUS_USAGE.md                    # MyBatis-Plus使用指南
```

## 🚀 快速开始

### 1. 数据库准备
```bash
# 方法1: 使用提供的脚本（推荐）
# 创建数据库
mysql -u root -p < sql/create_database.sql

# 创建表结构
mysql -u root -p < sql/stock_tables.sql
```

或手动执行：
```bash
# 创建数据库和表
mysql -u root -p -e "CREATE DATABASE stock_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
mysql -u root -p stock_db < sql/stock_tables.sql
```

### 2. 配置数据库
编辑 `src/main/resources/application.properties`:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/stock_db?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
spring.datasource.username=root
spring.datasource.password=你的密码
```

### 3. 编译打包
```bash
mvn clean package -DskipTests
```

### 4. 启动应用
```bash
java -jar target/strategies-api-0.0.1-SNAPSHOT.jar
```

### 5. 初始化数据
```bash
curl -X POST http://localhost:8080/api/strategies/init-history
```

## 🔌 API接口

### 股票数据接口

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/strategies/stocks` | GET | 获取股票列表 |
| `/api/strategies/stock/{code}` | GET | 获取股票详情（含分时） |
| `/api/strategies/stock/{code}/{date}` | GET | 获取指定日期详情 |
| `/api/strategies/init-history` | POST | 初始化股票数据 |
| `/api/strategies/fetch-minute-data` | POST | 手动触发分时更新 |

### Python Skill接口

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/strategies/execute` | POST | 执行Python Skill |

## 📊 数据库表结构

### stock_basic - 股票基本信息表
- `stock_code`: 股票代码（唯一索引）
- `market`: 市场标识（sh/sz）
- `stock_name`: 股票名称

### stock_daily - 股票日线数据表
- `stock_code`: 股票代码
- `trade_date`: 交易日期
- `open_price/high_price/low_price/close_price`: 价格数据
- `minute_data`: 5分钟K线JSON数据
- 联合唯一索引：`(stock_code, trade_date)`

## ⚙️ 定时任务配置

**默认配置**: 每5分钟执行一次（交易时段）
```
cron = "0 */5 9-15 * * MON-FRI"
```

**修改方法**: 编辑 `StockScheduleServiceImpl.java` 第39行

## 📝 技术栈

- **后端框架**: Spring Boot 2.2.7
- **ORM框架**: MyBatis-Plus 3.5.3.1
- **数据库**: MySQL 5.7+
- **HTTP客户端**: Hutool Http
- **JSON处理**: Fastjson 1.2.83
- **工具库**: Lombok, Commons-Lang3

## ⚠️ 注意事项

1. **网络要求**: 服务器需能访问新浪API（外网）
2. **数据库**: 确保MySQL已启动且配置正确
3. **首次使用**: 必须先执行SQL脚本并调用初始化接口
4. **异步执行**: 初始化和手动更新接口为异步执行，立即返回
5. **日志监控**: 查看控制台日志了解数据处理进度

## 🐛 已知问题

无

## 📞 技术支持

如有问题，请查看：
- `RELEASE_CHECKLIST.md` - 发布检查清单
- `STOCK_DATA_USAGE.md` - 详细使用文档
- `MYBATIS_PLUS_USAGE.md` - MyBatis-Plus使用指南

## 🎯 后续规划

- [ ] 添加更多技术指标计算
- [ ] 实现股票预警功能
- [ ] 添加数据可视化接口
- [ ] 支持更多数据源
- [ ] 性能优化（缓存、分批处理）

---

**发布完成，可以开始使用了！** 🎉
