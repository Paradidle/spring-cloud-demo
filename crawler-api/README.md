# Crawler API

基于 Selenium 和 Spring AI 的通用爬虫框架，支持二维码登录和内容分析。

## 功能特性

- 通用的二维码登录方案
- 支持多平台扩展（小红书、Facebook等）
- 集成 Spring AI 进行内容分析
- 自动截图保存二维码到 resources 目录

## 项目结构

```
crawler-api/
├── src/main/java/com/paradidle/crawler/api/
│   ├── CrawlerApiApplication.java          # 主应用类
│   ├── controller/
│   │   └── CrawlerController.java          # REST API 控制器
│   └── service/
│       ├── BaseCrawlerService.java         # 通用爬虫基类
│       ├── XiaohongshuCrawlerService.java  # 小红书实现
│       └── FacebookCrawlerService.java     # Facebook 实现
├── src/main/resources/
│   ├── application.properties              # 配置文件
│   └── qrcode/                             # 二维码保存目录
└── src/test/java/
    └── QrCodeTest.java                     # 测试类
```

## 使用方法

### 1. 启动应用

```bash
mvn spring-boot:run
```

### 2. API 接口

#### 小红书登录
```bash
curl -X POST http://localhost:8081/api/crawler/xiaohongshu/login
```

#### Facebook 登录
```bash
curl -X POST http://localhost:8081/api/crawler/facebook/login
```

### 3. 运行测试

```bash
mvn test -Dtest=QrCodeTest
```

## 扩展新平台

继承 `BaseCrawlerService` 并实现具体平台的登录逻辑：

```java
@Service
public class YourPlatformCrawlerService extends BaseCrawlerService {
    
    public File loginWithQrCode() {
        initDriver();
        navigateTo("your-platform-url");
        
        File qrFile = captureQrCodeWithCustomLocator(
            driver -> driver.findElement(By.cssSelector("your-qr-selector")),
            outputPath
        );
        
        return qrFile;
    }
}
```

## 核心方法说明

### BaseCrawlerService

- `initDriver()`: 初始化 Chrome 浏览器驱动
- `navigateTo(String url)`: 导航到指定 URL
- `captureQrCodeWithCustomLocator(Function<WebDriver, WebElement> locator, String outputPath)`: 
  使用自定义定位器截取二维码
- `closeDriver()`: 关闭浏览器

## 注意事项

1. **ChromeDriver管理**: 
   - 项目自带 `chromedriver.exe` (Windows版本)
   - Linux部署时需要替换为Linux版本的ChromeDriver
   - 运行 `./prepare-chromedriver.sh` 自动下载匹配的ChromeDriver

2. **无头模式**:
   - Windows开发环境: 默认有界面模式
   - Linux服务器: 自动启用无头模式,或通过 `-Dchrome.headless=true` 手动启用

3. 二维码图片保存在 `src/main/resources/qrcode/` 目录
4. 需要手动扫描二维码完成登录
5. 确保网络连接正常

## Linux 部署

详细部署指南请查看 [LINUX_DEPLOYMENT.md](LINUX_DEPLOYMENT.md)

快速开始:
```bash
# 1. 安装Chrome浏览器
sudo yum install -y google-chrome-stable  # CentOS
# 或
sudo apt-get install -y google-chrome-stable  # Ubuntu

# 2. 准备ChromeDriver
chmod +x prepare-chromedriver.sh
./prepare-chromedriver.sh

# 3. 启动应用(无头模式)
java -Dchrome.headless=true -jar crawler-api-0.0.1-SNAPSHOT.jar
```
