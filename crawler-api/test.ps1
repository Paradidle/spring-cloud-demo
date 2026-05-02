# Crawler API 测试脚本

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Crawler API 二维码截取测试" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$projectRoot = "D:\gitproject\spring-cloud-demo\crawler-api"
$qrcodeDir = Join-Path $projectRoot "src\main\resources\qrcode"

# 创建二维码保存目录
if (-not (Test-Path $qrcodeDir)) {
    Write-Host "创建二维码保存目录..." -ForegroundColor Yellow
    New-Item -ItemType Directory -Path $qrcodeDir -Force | Out-Null
}

Write-Host "开始编译项目..." -ForegroundColor Green
Set-Location "D:\gitproject\spring-cloud-demo"
mvn clean compile -pl crawler-api -am -q

if ($LASTEXITCODE -eq 0) {
    Write-Host "编译成功！" -ForegroundColor Green
    Write-Host ""
    Write-Host "请选择测试方式：" -ForegroundColor Cyan
    Write-Host "1. 运行 CommandLineRunner 测试（自动打开浏览器）" -ForegroundColor White
    Write-Host "2. 运行单元测试" -ForegroundColor White
    Write-Host "3. 启动 Web 服务（手动调用 API）" -ForegroundColor White
    Write-Host ""
    
    $choice = Read-Host "请输入选项 (1/2/3)"
    
    switch ($choice) {
        "1" {
            Write-Host ""
            Write-Host "启动应用进行测试..." -ForegroundColor Green
            Write-Host "应用将自动打开小红书网页并截取二维码" -ForegroundColor Yellow
            Set-Location $projectRoot
            mvn spring-boot:run
        }
        "2" {
            Write-Host ""
            Write-Host "运行单元测试..." -ForegroundColor Green
            Set-Location $projectRoot
            mvn test -Dtest=QrCodeTest
        }
        "3" {
            Write-Host ""
            Write-Host "启动 Web 服务..." -ForegroundColor Green
            Write-Host "服务启动后，使用以下命令测试：" -ForegroundColor Yellow
            Write-Host "curl -X POST http://localhost:8081/api/crawler/xiaohongshu/login" -ForegroundColor White
            Set-Location $projectRoot
            mvn spring-boot:run
        }
        default {
            Write-Host "无效的选项" -ForegroundColor Red
        }
    }
} else {
    Write-Host "编译失败！" -ForegroundColor Red
}

Write-Host ""
Write-Host "测试完成后，检查以下目录是否有二维码图片：" -ForegroundColor Cyan
Write-Host $qrcodeDir -ForegroundColor White
