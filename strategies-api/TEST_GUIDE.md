# 批量股票分析功能测试指南

## 快速测试步骤

### 1. 启动服务

确保后端服务已启动（默认端口8083）

### 2. 测试单个股票分析（可选）

```bash
GET http://localhost:8083/api/strategies/analyze-box/600000
```

### 3. 启动批量分析

```bash
POST http://localhost:8083/api/strategies/analyze-all-stocks
```

响应：
```
批量分析任务已启动，请稍后检查结果
```

### 4. 查看进度

观察控制台日志输出：
```
进度: [10/465] 2.15% - 股票 600000 (浦发银行) 分析完成
进度: [20/465] 4.30% - 股票 600004 (白云机场) 分析完成
...
临时Excel已生成: stock_analysis_results/stock_box_analysis_temp_50_of_465_2026-05-12.xlsx (50个股票)
```

### 5. 手动导出Excel（随时可用）

如果想查看当前已完成的结果，调用：

```bash
POST http://localhost:8083/api/strategies/export-excel
```

响应示例：
```
Excel文件已生成: stock_analysis_results/stock_box_analysis_2026-05-12.xlsx, 共 150 个股票
```

### 6. 查看结果文件

打开目录：`stock_analysis_results/`

你会看到：
- ✅ `stock_box_analysis_2026-05-12.xlsx` - 最终Excel文件
- ✅ `stock_box_analysis_temp_50_of_465_2026-05-12.xlsx` - 临时Excel（每50个股票生成）
- ✅ `600000.json`, `600004.json` ... - 单个股票的JSON结果
- ✅ `analysis_progress.txt` - 进度记录文件

## 中断恢复测试

### 场景1：分析过程中停止服务

1. 启动批量分析
2. 等待分析约100只股票后，停止服务（Ctrl+C）
3. 重新启动服务
4. 再次调用 `/analyze-all-stocks`
5. 系统会从第101只股票继续，不会重复分析前100只

### 场景2：只想查看部分结果

1. 启动批量分析
2. 等待分析约50只股票
3. 调用 `/export-excel` 接口
4. 查看生成的Excel文件，包含已完成的50只股票

## 验证数据完整性

### 方法1：检查JSON文件数量

```bash
# Windows PowerShell
(Get-ChildItem stock_analysis_results\*.json).Count

# Linux/Mac
ls stock_analysis_results/*.json | wc -l
```

应该与进度文件中记录的已完成股票数量一致。

### 方法2：检查Excel行数

打开Excel文件，数据行数应该等于已分析的股票数量。

### 方法3：查看进度文件

```bash
cat stock_analysis_results/analysis_progress.txt
```

每一行代表一个已完成的股票：
```
COMPLETED:600000 at 2026-05-12 10:05:30
COMPLETED:600004 at 2026-05-12 10:06:15
...
```

## 性能测试建议

### 小批量测试（推荐先测试）

修改代码中的线程池大小和股票过滤条件：

```java
// 在 DashScopeAnalysisServiceImpl.java 中
private static final int THREAD_POOL_SIZE = 2; // 减小线程数

// 在 analyzeAllStocksAndExportToExcel() 方法中
wrapper.eq(StockBasic::getIsIndex, false)
       .last("LIMIT 10")  // 只分析前10只股票
       .select(StockBasic::getStockCode, StockBasic::getStockName);
```

### 完整测试

使用默认配置分析所有465只股票，预计时间：
- 快速模式（qwen-turbo）：约30-60分钟
- 取决于API响应速度和网络状况

## 故障排查

### 问题1：找不到Excel文件

**检查位置**：确保在 `stock_analysis_results/` 目录下查找，而不是项目根目录

**解决方案**：
```bash
# 查看目录内容
ls -la stock_analysis_results/

# 如果目录为空，调用手动导出接口
POST http://localhost:8083/api/strategies/export-excel
```

### 问题2：JSON文件存在但Excel为空

**原因**：可能是JSON文件格式错误

**解决方案**：
1. 检查某个JSON文件是否可读：
   ```bash
   cat stock_analysis_results/600000.json
   ```
2. 查看日志中的错误信息
3. 删除有问题的JSON文件后重新导出

### 问题3：断点续传不生效

**原因**：进度文件损坏或格式错误

**解决方案**：
```bash
# 删除进度文件，重新开始
rm stock_analysis_results/analysis_progress.txt

# 或者删除整个目录
rm -rf stock_analysis_results/
```

## API接口汇总

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/strategies/analyze-box/{code}` | GET | 分析单个股票 |
| `/api/strategies/analyze-all-stocks` | POST | 启动批量分析（异步） |
| `/api/strategies/export-excel` | POST | 从已有JSON导出Excel（同步） |

## 预期文件结构

```
spring-cloud-demo/
└── stock_analysis_results/
    ├── stock_box_analysis_2026-05-12.xlsx          # 最终Excel
    ├── stock_box_analysis_temp_50_of_465_2026-05-12.xlsx
    ├── stock_box_analysis_temp_100_of_465_2026-05-12.xlsx
    ├── analysis_progress.txt                        # 进度记录
    ├── 600000.json                                  # 浦发银行
    ├── 600004.json                                  # 白云机场
    ├── 600005.json                                  # 东风汽车
    └── ...                                          # 其他股票
```

## 成功标志

✅ 控制台显示进度日志  
✅ `stock_analysis_results/` 目录中有JSON文件  
✅ 调用 `/export-excel` 返回成功消息  
✅ Excel文件可以正常打开并包含数据  
✅ 中断后重启可以继续分析  

祝测试顺利！🎉
