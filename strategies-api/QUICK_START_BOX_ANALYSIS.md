# 快速开始 - DashScope AI 股票箱体分析

## 1. 配置DashScope API Key

### Windows PowerShell
```powershell
$env:DASHSCOPE_API_KEY="your-api-key-here"
```

### Windows CMD
```cmd
set DASHSCOPE_API_KEY=your-api-key-here
```

### Linux/Mac
```bash
export DASHSCOPE_API_KEY=your-api-key-here
```

> **注意**: 请将 `your-api-key-here` 替换为你在阿里云DashScope控制台获取的真实API Key

## 2. 确保数据库中有历史数据

如果还没有初始化历史数据，先执行：

```bash
curl -X POST http://localhost:8083/api/strategies/init-history
```

或者使用快速初始化（只初始化基本信息）：

```bash
curl -X POST http://localhost:8083/api/strategies/init-basic
```

## 3. 启动服务

在项目根目录执行：

```bash
cd strategies-api
mvn spring-boot:run
```

或者直接运行jar包：

```bash
java -jar target/strategies-api-0.0.1-SNAPSHOT.jar
```

## 4. 测试功能

### 方法1: 使用浏览器
访问以下地址（将600519替换为你想分析的股票代码）：
```
http://localhost:8083/api/strategies/analyze-box/600519
```

### 方法2: 使用curl
```bash
curl http://localhost:8083/api/strategies/analyze-box/600519
```

### 方法3: 使用PowerShell测试脚本
```powershell
.\test-box-analysis.ps1
```

## 5. 查看结果

AI会返回类似以下的分析结果：

```
根据对股票600519近120个交易日的数据分析：

【箱体识别结果】
- 箱顶价格：1850.00元
- 箱底价格：1650.00元
- 箱体振幅：12.12%

【当前位置判断】
- 最新收盘价：1820.00元
- 相对位置R值：0.85
- 位置判断：顶部附近

【操作建议】
当前股价接近箱体顶部，建议谨慎追高。如持有可考虑部分止盈，等待回调至箱体中部或底部再考虑加仓。若有效突破箱顶并站稳，可重新评估上涨空间。
```

## 常见问题

### Q1: 提示"未找到股票XXX的历史数据"
**A**: 需要先初始化该股票的历史数据，执行步骤2中的初始化命令

### Q2: 提示API调用失败
**A**: 检查以下几点：
- DASHSCOPE_API_KEY是否正确设置
- 网络连接是否正常
- application.yml中chat.client.enabled是否为true

### Q3: 分析时间很长
**A**: 正常现象，AI分析通常需要5-15秒，取决于数据量和网络状况

### Q4: 如何分析其他股票？
**A**: 修改URL中的股票代码即可，例如：
```
http://localhost:8083/api/strategies/analyze-box/000001
```

## 下一步

- 查看详细文档：[DASHSCOPE_ANALYSIS_README.md](DASHSCOPE_ANALYSIS_README.md)
- 查看箱体识别规则：[strategies.md](strategies.md)
- 查看项目整体说明：[README_RELEASE.md](README_RELEASE.md)
