# DashScope AI 股票箱体分析测试脚本
# 使用方法: .\test-box-analysis.ps1

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "DashScope AI 股票箱体分析测试" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# 配置
$baseUrl = "http://localhost:8083"
$stockCode = "600519"  # 默认测试股票代码（贵州茅台）

Write-Host "测试股票代码: $stockCode" -ForegroundColor Yellow
Write-Host ""

# 测试1: 检查服务是否运行
Write-Host "[测试1] 检查服务是否运行..." -ForegroundColor Green
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/api/strategies/stocks" -Method Get -TimeoutSec 5
    Write-Host "✓ 服务正常运行，共 $($response.Count) 只股票" -ForegroundColor Green
} catch {
    Write-Host "✗ 服务未运行或无法访问" -ForegroundColor Red
    Write-Host "请先启动服务: mvn spring-boot:run" -ForegroundColor Yellow
    exit 1
}
Write-Host ""

# 测试2: 检查是否有历史数据
Write-Host "[测试2] 检查股票历史数据..." -ForegroundColor Green
try {
    $dailyCountResponse = Invoke-RestMethod -Uri "$baseUrl/api/strategies/daily-count" -Method Get -TimeoutSec 5
    Write-Host "✓ 日线数据数量: $dailyCountResponse" -ForegroundColor Green
} catch {
    Write-Host "✗ 无法获取数据统计" -ForegroundColor Red
}
Write-Host ""

# 测试3: 执行箱体分析
Write-Host "[测试3] 执行AI箱体分析..." -ForegroundColor Green
Write-Host "正在调用DashScope API进行分析，请稍候..." -ForegroundColor Yellow
Write-Host ""

try {
    $startTime = Get-Date
    $result = Invoke-RestMethod -Uri "$baseUrl/api/strategies/analyze-box/$stockCode" -Method Get -TimeoutSec 60
    $endTime = Get-Date
    $duration = ($endTime - $startTime).TotalSeconds
    
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host "分析结果 (耗时: ${duration}秒)" -ForegroundColor Cyan
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host ""
    Write-Host $result -ForegroundColor White
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host "分析完成" -ForegroundColor Green
    Write-Host "========================================" -ForegroundColor Cyan
    
} catch {
    Write-Host "✗ 分析失败" -ForegroundColor Red
    Write-Host "错误信息: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host ""
    Write-Host "可能的原因:" -ForegroundColor Yellow
    Write-Host "1. DASHSCOPE_API_KEY 未配置或配置错误" -ForegroundColor Yellow
    Write-Host "2. 该股票没有足够的历史数据（需要至少120天）" -ForegroundColor Yellow
    Write-Host "3. 网络连接问题" -ForegroundColor Yellow
    Write-Host "4. DashScope API 服务异常" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "提示: 可以修改脚本中的 `$stockCode 变量来测试其他股票" -ForegroundColor Gray
