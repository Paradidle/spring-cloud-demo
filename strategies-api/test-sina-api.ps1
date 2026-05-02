# 测试新浪股票API

Write-Host "=== 测试1: 获取股票列表 ===" -ForegroundColor Green
$response1 = Invoke-RestMethod -Uri "http://vip.stock.finance.sina.com.cn/quotes_service/api/json_v2.php/Market_Center.getHQNodeData?page=1&num=3&sort=symbol&asc=1&node=sh_a&symbol=" -Method Get
Write-Host "返回数据:" $response1
Write-Host ""

Write-Host "=== 测试2: 获取股票详情 (600519) ===" -ForegroundColor Green
$response2 = Invoke-RestMethod -Uri "http://hq.sinajs.cn/list=sh600519" -Method Get
Write-Host "返回数据:" $response2
Write-Host ""

Write-Host "=== 测试3: 获取历史数据 (600519, 2024-01-02) ===" -ForegroundColor Green
$response3 = Invoke-RestMethod -Uri "http://money.finance.sina.com.cn/quotes_service/api/json_v2.php/CN_MarketData.getKLineData?symbol=sh600519&scale=240&ma=no&datalen=1&d1=2024-01-02" -Method Get
Write-Host "返回数据:" $response3
Write-Host ""

Write-Host "测试完成!" -ForegroundColor Cyan
