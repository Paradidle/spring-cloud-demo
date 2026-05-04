# 股票数据系统项目

## 项目概述

本项目包含两部分：
1. **前端页面** - 基于《智选》风格的深色专业金融界面
2. **后端服务** - Spring Boot 股票数据服务

## 技术栈

### 前端
- Vite 7 + TypeScript
- Tailwind CSS (深色主题)
- 原生 JavaScript

### 后端
- Spring Boot 2.2.7
- MyBatis-Plus 3.5.3.1
- MySQL 8.0
- Java 17

## 目录结构

```
/workspace/projects/                          # 前端项目
├── index.html                                 # 入口页面
├── vite.config.ts                            # Vite配置（含API代理）
└── src/                                      # 源码目录

/workspace/projects/spring-cloud-demo/          # 后端项目
└── strategies-api/                            # 股票API模块
    ├── src/main/java/.../
    │   └── service/impl/StockServiceImpl.java  # 核心服务逻辑
    ├── src/main/resources/
    │   └── application.properties             # 数据库配置
    └── sql/
        ├── create_database.sql                # 创建数据库
        └── stock_tables.sql                   # 建表脚本
```

## 服务端口

| 服务 | 端口 | 说明 |
|------|------|------|
| 前端页面 | 5000 | 股票数据系统Web界面 |
| 后端API | 8083 | strategies-api 服务 |

## API 接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/strategies/stocks` | 获取股票代码列表 |
| GET | `/api/strategies/stocks/detail` | 获取股票列表(带详细信息) |
| GET | `/api/strategies/stock/{code}` | 获取股票详情（含分时数据） |
| GET | `/api/strategies/stock/{code}/{date}` | 获取指定日期股票数据 |
| POST | `/api/strategies/init-history` | 初始化历史数据 |
| POST | `/api/strategies/fetch-minute-data` | 刷新分时数据 |

## 数据库

- **数据库名**: stock_db
- **字符集**: utf8mb4

### 主要表

| 表名 | 说明 |
|------|------|
| stock_basic | 股票基本信息（代码、名称、市场） |
| stock_daily | 股票日线数据（含5分钟分时） |

## 数据状态

- **股票基本信息**: 465 只
- **日线数据**: 114,524 条
- **分时数据**: 465 只股票有分时数据
- **时间范围**: 近250个交易日（约一年）

## 常用命令

### 前端
```bash
cd /workspace/projects
pnpm dev          # 启动开发服务器（端口5000）
```

### 后端
```bash
cd /workspace/projects/spring-cloud-demo/strategies-api
mvn clean package -DskipTests    # 打包
java -jar target/strategies-api-0.0.1-SNAPSHOT.jar  # 启动
```

### 数据库
```bash
service mysql start                              # 启动MySQL
mysql -u root -p123456                          # 连接数据库
```

## 数据源

使用东方财富 API 获取全量A股数据：
- 上海A股 (m:1+t:23)
- 深圳主板 (m:0+t:6)
- 创业板 (m:0+t:80)
- 科创板 (m:1+t:8)
- 北交所 (m:0+t:81)

请求间隔设置为300ms，避免被限流。

## 注意事项

1. 后端服务需要先启动才能访问API
2. 前端页面访问地址: http://localhost:5000
3. API基础地址: http://localhost:8083/api/strategies
4. 前端通过 Vite 代理访问后端API，解决跨域问题
5. 初始化任务在后台运行，查看日志: `tail -f /workspace/projects/spring-cloud-demo/strategies-api/app.log`
