# 股票数据系统 - 发布检查清单与快速开始

## ✅ 发布前检查清单

### 1. 数据库准备
- [ ] MySQL已安装并启动
- [ ] 创建数据库: `CREATE DATABASE stock_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;`
- [ ] 执行SQL脚本: `sql/stock_tables.sql`
- [ ] 验证表创建成功: `SHOW TABLES;` 应显示 `stock_basic`, `stock_daily`, `v_latest_stock_data`

### 2. 配置文件检查
- [ ] 修改 `application.properties` 中的数据库连接信息
  - `spring.datasource.url`: 数据库地址
  - `spring.datasource.username`: 用户名
  - `spring.datasource.password`: 密码

### 3. 依赖检查
- [ ] 执行 `mvn clean compile` 确保编译成功
- [ ] 确认MyBatis-Plus、MySQL驱动等依赖已正确引入

### 4. 代码检查
- [x] Controller接口正常
- [x] Service层逻辑完整
- [x] Mapper XML文件存在
- [x] Entity实体类有完整注释
- [x] 定时任务已启用 (@EnableScheduling)
- [x] 异步执行耗时操作（避免阻塞HTTP响应）

## 🚀 快速开始

### 步骤1: 准备数据库

```sql
-- 方法1: 使用提供的脚本（推荐）
-- 1. 创建数据库
mysql -u root -p < sql/create_database.sql

-- 2. 创建表结构
mysql -u root -p < sql/stock_tables.sql
```

或手动执行：
```sql
-- 1. 创建数据库
CREATE DATABASE IF NOT EXISTS stock_db 
CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

-- 2. 使用数据库
USE stock_db;

-- 3. 执行建表脚本
-- 在MySQL客户端中执行 sql/stock_tables.sql 文件
```

或在命令行执行：
```bash
# 创建数据库
mysql -u root -p < sql/create_database.sql

# 创建表
mysql -u root -p stock_db < sql/stock_tables.sql
```

验证表创建成功：
```sql
SHOW TABLES;
-- 应显示: stock_basic, stock_daily, v_latest_stock_data
```

### 步骤2: 配置数据库连接

编辑 `src/main/resources/application.properties`:

```properties
# 修改为你的数据库配置
spring.datasource.url=jdbc:mysql://localhost:3306/stock_db?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
spring.datasource.username=你的用户名
spring.datasource.password=你的密码
```

### 步骤3: 编译打包

```bash
cd strategies-api
mvn clean package -DskipTests
```

### 步骤4: 启动应用

```bash
java -jar target/strategies-api-0.0.1-SNAPSHOT.jar
```

或使用Maven：
```bash
mvn spring-boot:run
```

### 步骤5: 初始化数据

应用启动后，调用初始化接口：

```bash
curl -X POST http://localhost:8080/api/strategies/init-history
```

或在浏览器访问：
```
POST http://localhost:8080/api/strategies/init-history
```

**注意**: 该接口会异步执行，立即返回"数据初始化任务已启动"，实际处理在后台进行。

### 步骤6: 查看日志

观察控制台输出，会看到类似以下日志：

```
开始初始化股票数据
共需要处理XXX只股票
已处理: 基本信息50条, 日线数据50条
已处理: 基本信息100条, 日线数据100条
...
数据初始化完成: 基本信息XXX条, 日线数据XXX条
```

### 步骤7: 验证数据

在MySQL中查询：

```sql
-- 查看股票基本信息
SELECT COUNT(*) FROM stock_basic;

-- 查看今日日线数据
SELECT COUNT(*) FROM stock_daily WHERE trade_date = CURDATE();

-- 查看某只股票的详细信息
SELECT * FROM stock_basic WHERE stock_code = '600519';
SELECT * FROM stock_daily WHERE stock_code = '600519' ORDER BY trade_date DESC LIMIT 5;
```

### 步骤8: 体验功能

#### 8.1 获取股票列表
```bash
GET http://localhost:8080/api/strategies/stocks
```

#### 8.2 获取单只股票详情（包含分时数据）
```bash
GET http://localhost:8080/api/strategies/stock/600519
```

#### 8.3 获取指定日期的股票详情
```bash
GET http://localhost:8080/api/strategies/stock/600519/2024-01-02
```

#### 8.4 手动触发分时数据更新
```bash
POST http://localhost:8080/api/strategies/fetch-minute-data
```

#### 8.5 定时任务自动运行
- 交易时段（周一至周五 9:00-15:00）每5分钟自动更新分时数据
- 无需手动干预

## 📊 API接口总览

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/strategies/execute` | POST | 执行Python Skill |
| `/api/strategies/stocks` | GET | 获取股票列表 |
| `/api/strategies/stock/{code}` | GET | 获取股票详情（含分时） |
| `/api/strategies/stock/{code}/{date}` | GET | 获取指定日期股票详情 |
| `/api/strategies/init-history` | POST | 初始化股票数据 |
| `/api/strategies/fetch-minute-data` | POST | 手动触发分时数据拉取 |

## ⚠️ 注意事项

### 1. 网络要求
- 服务器必须能访问新浪API（外网）
- 防火墙不要阻止HTTP请求

### 2. 性能考虑
- 初始化数据可能需要较长时间（取决于股票数量）
- 建议首次初始化后，依靠定时任务增量更新
- 如需更快的初始化速度，可调整线程池配置

### 3. 定时任务
- 默认配置：每5分钟执行一次（仅交易时段）
- 如需修改频率，编辑 `StockScheduleServiceImpl.java` 中的 `@Scheduled` 注解

### 4. 数据存储
- `minute_data` 字段存储JSON格式的5分钟K线数据
- 同一股票同一天只会有一条记录（重复调用会更新）

### 5. 错误处理
- 单个股票失败不影响其他股票处理
- 查看日志了解详细的成功/失败统计

## 🔧 常见问题

### Q1: 编译失败？
A: 检查Maven版本（建议3.6+），执行 `mvn clean compile` 查看详细错误

### Q2: 数据库连接失败？
A: 检查 `application.properties` 中的数据库配置是否正确，MySQL是否启动

### Q3: 初始化数据很慢？
A: 正常现象，需要逐个股票拉取数据。可以查看日志了解进度。

### Q4: 定时任务没有执行？
A: 检查当前时间是否在交易时段（周一至周五 9:00-15:00）

### Q5: 如何修改定时任务频率？
A: 修改 `StockScheduleServiceImpl.java` 第39行的cron表达式

## 📝 下一步

1. 根据需要调整定时任务频率
2. 添加更多业务逻辑（如数据分析、预警等）
3. 优化性能（分批处理、缓存等）
4. 添加监控和告警

---

**祝使用愉快！** 🎉
