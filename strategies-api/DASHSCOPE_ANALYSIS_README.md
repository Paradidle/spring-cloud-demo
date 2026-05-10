# DashScope AI 股票箱体分析功能

## 功能说明

本功能使用阿里云DashScope大模型API，根据近120个交易日的股票K线数据（开盘价、最高价、最低价、收盘价），自动识别股票价格的箱体区间，并判断当前价格处于箱体的什么位置。

## 箱体识别规则

基于 `strategies.md` 文档中的技术分析规则：

1. **局部极值识别**
   - 局部高点：High[i] > High[i-1] 且 High[i] > High[i+1]
   - 局部低点：Low[i] < Low[i-1] 且 Low[i] < Low[i+1]

2. **价格聚类**
   - 对局部高点价格进行聚类（相邻价格差≤0.5%归为同一簇）
   - 取最大簇的平均值作为箱顶
   - 对局部低点同样处理得到箱底

3. **有效性检查**
   - 箱顶 > 箱底
   - 振幅 ≥ 2%

4. **位置判断**
   - 计算相对位置 R = (currentPrice - boxBottom) / (boxTop - boxBottom)
   - R > 0.85: 顶部附近
   - R < 0.15: 底部附近
   - 0.4 ≤ R ≤ 0.6: 中部
   - R > 1.0: 向上突破
   - R < 0: 向下突破

## API接口

### 接口地址
```
GET /api/strategies/analyze-box/{code}
```

### 参数说明
- `code`: 股票代码（例如：600519）

### 返回结果
返回AI分析的文本结果，包含：
1. 箱顶价格
2. 箱底价格
3. 当前价格在箱体中的位置
4. 相对位置R值
5. 操作建议

## 使用示例

### curl命令
```bash
curl http://localhost:8083/api/strategies/analyze-box/600519
```

### 浏览器访问
```
http://localhost:8083/api/strategies/analyze-box/600519
```

### Java代码调用
```java
@RestController
public class TestController {
    
    @Autowired
    private DashScopeAnalysisService analysisService;
    
    @GetMapping("/test")
    public String test() {
        return analysisService.analyzeBoxPosition("600519");
    }
}
```

## 配置要求

### 1. 环境变量配置
确保已设置以下环境变量：

```bash
# Windows PowerShell
$env:DASHSCOPE_API_KEY="your-api-key-here"

# Linux/Mac
export DASHSCOPE_API_KEY=your-api-key-here
```

### 2. application.yml配置
```yaml
spring:
  ai:
    dashscope:
      api-key: ${DASHSCOPE_API_KEY}
      read-timeout: 30000
    chat:
      client:
        enabled: true  # 必须设置为true
```

### 3. Maven依赖
项目已包含Spring AI Alibaba依赖：
```xml
<dependency>
    <groupId>com.alibaba.cloud.ai</groupId>
    <artifactId>spring-ai-alibaba-starter</artifactId>
    <version>1.0.0-M6.1</version>
</dependency>
```

## 数据要求

- **数据量**: 至少需要120个交易日的历史数据
- **必需字段**: 开盘价(openPrice)、最高价(highPrice)、最低价(lowPrice)、收盘价(closePrice)
- **数据来源**: 从stock_daily表查询

## 注意事项

1. **首次使用前**，请确保已经初始化了股票历史数据：
   ```bash
   curl -X POST http://localhost:8083/api/strategies/init-history
   ```

2. **API Key配置**: 需要在阿里云DashScope控制台获取API Key并配置到环境变量

3. **响应时间**: AI分析可能需要几秒到十几秒，取决于数据量和网络状况

4. **错误处理**: 如果未找到股票数据或API调用失败，会返回相应的错误信息

## 技术实现

### 核心类
- `DashScopeAnalysisService`: 服务接口
- `DashScopeAnalysisServiceImpl`: 服务实现类
- `StrategiesController`: REST控制器

### 工作流程
1. 接收股票代码参数
2. 从数据库查询近120天的K线数据
3. 构建包含箱体识别规则的提示词
4. 调用DashScope Chat API进行分析
5. 返回AI分析结果

## 扩展建议

1. **批量分析**: 可以扩展为支持批量分析多只股票
2. **缓存结果**: 对相同股票的分析结果进行缓存，避免重复调用API
3. **异步处理**: 对于大量股票分析，可以使用异步任务
4. **结果持久化**: 将分析结果保存到数据库，便于历史追溯
5. **可视化展示**: 在前端添加箱体图形化展示

## 故障排查

### 问题1: 返回"未找到股票XXX的历史数据"
**解决**: 检查是否已初始化该股票的历史数据

### 问题2: API调用失败
**解决**: 
- 检查DASHSCOPE_API_KEY是否正确配置
- 检查网络连接
- 查看日志文件了解详细错误信息

### 问题3: 分析结果为空或异常
**解决**:
- 检查stock_daily表中是否有足够的数据（至少120天）
- 确认chat client已启用（enabled: true）
- 查看应用日志

## 日志查看

分析过程会记录日志，可以通过以下方式查看：
```bash
# 查看实时日志
tail -f app.log

# 搜索特定股票的日志
grep "600519" app.log
```
