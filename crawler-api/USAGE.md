# Crawler API 使用指南

## 快速开始

### 1. 编译项目

```bash
cd D:\gitproject\spring-cloud-demo
mvn clean package -pl crawler-api -am -DskipTests
```

### 2. 运行测试

有两种方式测试二维码截取功能：

#### 方式一：使用 CommandLineRunner（推荐）

```bash
cd D:\gitproject\spring-cloud-demo\crawler-api
mvn spring-boot:run
```

应用启动后会自动：
1. 打开小红书网页
2. 等待页面加载
3. 截取二维码并保存到 `src/main/resources/qrcode/xiaohongshu_test.png`
4. 输出文件路径和状态信息
5. 10秒后自动关闭浏览器

#### 方式二：使用单元测试

```bash
cd D:\gitproject\spring-cloud-demo\crawler-api
mvn test -Dtest=QrCodeTest
```

### 3. 验证结果

测试完成后，检查以下位置是否有二维码图片：

```
D:\gitproject\spring-cloud-demo\crawler-api\src\main\resources\qrcode\xiaohongshu_test.png
```

如果文件存在且大小大于 0，说明截取成功！

## API 接口使用

启动应用后（注释掉 CrawlerTestRunner），可以调用以下接口：

### 小红书登录

```bash
curl -X POST http://localhost:8081/api/crawler/xiaohongshu/login
```

返回示例：
```json
{
  "success": true,
  "message": "二维码已生成",
  "qrPath": "D:\\...\\xiaohongshu_qr.png",
  "qrExists": true
}
```

### Facebook 登录

```bash
curl -X POST http://localhost:8081/api/crawler/facebook/login
```

## 扩展新平台

创建新的 Service 类继承 `BaseCrawlerService`：

```java
@Service
public class YourPlatformCrawlerService extends BaseCrawlerService {
    
    private static final String BASE_URL = "https://your-platform.com";
    
    public File loginWithQrCode() {
        initDriver();
        
        try {
            navigateTo(BASE_URL);
            Thread.sleep(3000);
            
            // 根据实际页面结构调整定位器
            File qrFile = captureQrCodeWithCustomLocator(
                driver -> {
                    // 尝试多种定位方式
                    try {
                        return driver.findElement(By.cssSelector("canvas.qrcode"));
                    } catch (Exception e) {
                        return driver.findElement(By.xpath("//img[contains(@class, 'qr')]"));
                    }
                },
                getOutputPath()
            );
            
            System.out.println("请扫描二维码登录");
            waitForLoginSuccess();
            
            return qrFile;
        } catch (Exception e) {
            closeDriver();
            throw new RuntimeException("登录失败: " + e.getMessage(), e);
        }
    }
    
    private void waitForLoginSuccess() {
        wait.until(driver -> !driver.getCurrentUrl().contains("login"));
    }
    
    private String getOutputPath() {
        File dir = new File("src/main/resources/qrcode");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return Paths.get("src/main/resources/qrcode/your_platform_qr.png")
                   .toAbsolutePath().toString();
    }
}
```

在 Controller 中添加对应接口：

```java
@Autowired
private YourPlatformCrawlerService yourPlatformService;

@PostMapping("/your-platform/login")
public ResponseEntity<Map<String, Object>> yourPlatformLogin() {
    Map<String, Object> result = new HashMap<>();
    try {
        File qrFile = yourPlatformService.loginWithQrCode();
        result.put("success", true);
        result.put("qrPath", qrFile.getAbsolutePath());
        return ResponseEntity.ok(result);
    } catch (Exception e) {
        result.put("success", false);
        result.put("message", e.getMessage());
        return ResponseEntity.status(500).body(result);
    } finally {
        yourPlatformService.closeDriver();
    }
}
```

## 核心方法说明

### BaseCrawlerService 提供的方法

| 方法 | 说明 |
|------|------|
| `initDriver()` | 初始化 Chrome 浏览器 |
| `navigateTo(String url)` | 访问指定 URL |
| `captureQrCodeWithCustomLocator(Function, String)` | 截取二维码（推荐使用） |
| `waitForElement(By, String)` | 等待元素出现 |
| `clickElement(By, String)` | 点击元素 |
| `inputText(By, String, String)` | 输入文本 |
| `closeDriver()` | 关闭浏览器 |

### 二维码截取方法

`captureQrCodeWithCustomLocator` 接受两个参数：

1. **locator**: 函数式接口，用于定位二维码元素
   ```java
   driver -> driver.findElement(By.cssSelector("canvas.qrcode"))
   ```

2. **outputPath**: 二维码保存的绝对路径

方法会自动：
- 等待二维码元素出现
- 截取整个页面
- 计算二维码元素的位置和大小
- 裁剪出二维码部分
- 保存为 PNG 图片

## 常见问题

### 1. ChromeDriver 下载失败

确保网络畅通，WebDriverManager 会自动下载合适的 ChromeDriver。

### 2. 找不到二维码元素

检查页面的 HTML 结构，调整 CSS Selector 或 XPath：
- 打开浏览器开发者工具
- 找到二维码元素
- 复制 CSS Selector 或 XPath
- 更新 locator 函数

### 3. 截图为空或损坏

增加等待时间，确保页面完全加载：
```java
Thread.sleep(5000); // 增加等待时间
```

### 4. 端口冲突

修改 `application.properties`：
```properties
server.port=8082
```

## 下一步

完成二维码登录测试后，可以继续开发：
1. 实现内容爬取功能
2. 集成 Spring AI 分析文章内容
3. 添加更多平台支持
