# Crawler API 模块说明

## 概述

`crawler-api` 是一个基于 Selenium 和 Spring AI 的通用爬虫框架，主要用于：
1. 模拟浏览器操作进行网页爬取
2. 支持二维码登录等多种认证方式
3. 集成 Spring AI 对爬取的内容进行智能分析

## 已实现功能

### 1. 通用二维码登录方案

**核心类：** `BaseCrawlerService`

提供了通用的二维码截取方法 `captureQrCodeWithCustomLocator`，支持：
- 自定义元素定位器（CSS Selector、XPath 等）
- 自动裁剪二维码区域
- 保存到指定路径

### 2. 平台实现

#### 小红书 (XiaohongshuCrawlerService)
- 网址：https://www.xiaohongshu.com/explore
- 二维码保存位置：`src/main/resources/qrcode/xiaohongshu_qr.png`
- 支持多种二维码元素定位方式

#### Facebook (FacebookCrawlerService)
- 网址：https://www.facebook.com/login
- 二维码保存位置：`src/main/resources/qrcode/facebook_qr.png`
- 支持点击 QR 按钮后截取

### 3. REST API 接口

提供两个登录接口：
- `POST /api/crawler/xiaohongshu/login` - 小红书登录
- `POST /api/crawler/facebook/login` - Facebook 登录

返回 JSON 格式结果，包含二维码文件路径和状态。

### 4. 测试工具

- **CommandLineRunner**: 启动时自动执行测试
- **单元测试**: JUnit 5 测试用例
- **PowerShell 脚本**: 交互式测试脚本 (`test.ps1`)

## 项目结构

```
crawler-api/
├── src/
│   ├── main/
│   │   ├── java/com/paradidle/crawler/api/
│   │   │   ├── CrawlerApiApplication.java          # Spring Boot 主类
│   │   │   ├── CrawlerTestRunner.java              # 命令行测试运行器
│   │   │   ├── controller/
│   │   │   │   └── CrawlerController.java          # REST API 控制器
│   │   │   └── service/
│   │   │       ├── BaseCrawlerService.java         # 通用爬虫基类
│   │   │       ├── XiaohongshuCrawlerService.java  # 小红书实现
│   │   │       └── FacebookCrawlerService.java     # Facebook 实现
│   │   └── resources/
│   │       ├── application.properties              # 配置文件
│   │       └── qrcode/                             # 二维码保存目录
│   └── test/
│       └── java/com/paradidle/crawler/api/
│           ├── QrCodeTest.java                     # 二维码测试
│           └── XiaohongshuCrawlerTest.java         # 小红书测试
├── pom.xml                                         # Maven 配置
├── README.md                                       # 项目说明
├── USAGE.md                                        # 使用指南
├── test.ps1                                        # 测试脚本
└── .gitignore
```

## 技术栈

- **Spring Boot 3.5.0** - 应用框架
- **Selenium 4.18.1** - 浏览器自动化
- **WebDriverManager 5.6.3** - 自动管理浏览器驱动
- **Spring AI Alibaba 1.0.0-M6.1** - AI 集成（待使用）
- **JUnit 5** - 单元测试
- **Lombok** - 代码简化

## 快速开始

### 方式一：使用测试脚本（推荐）

```powershell
cd D:\gitproject\spring-cloud-demo\crawler-api
.\test.ps1
```

### 方式二：手动测试

1. 编译项目：
```bash
cd D:\gitproject\spring-cloud-demo
mvn clean package -pl crawler-api -am -DskipTests
```

2. 运行测试：
```bash
cd crawler-api
mvn spring-boot:run
```

3. 检查结果：
查看 `src/main/resources/qrcode/` 目录下是否有二维码图片

## 扩展新平台

只需 3 步即可添加新平台支持：

1. 创建 Service 类继承 `BaseCrawlerService`
2. 实现 `loginWithQrCode()` 方法，定义二维码定位逻辑
3. 在 `CrawlerController` 中添加对应接口

详细示例见 [USAGE.md](USAGE.md)

## 下一步计划

1. ✅ 完成通用二维码登录方案
2. ✅ 实现小红书和 Facebook 示例
3. ⏳ 实现内容爬取功能
4. ⏳ 集成 Spring AI 分析文章内容
5. ⏳ 添加更多平台支持（微博、知乎等）
6. ⏳ 实现分布式爬虫调度
7. ⏳ 添加数据持久化

## 注意事项

1. **首次运行**会自动下载 ChromeDriver，需要网络连接
2. **二维码定位**需要根据实际页面结构调整 CSS Selector 或 XPath
3. **等待时间**可能需要根据网络情况调整 `Thread.sleep()` 的时长
4. **端口配置**默认使用 8081，可在 `application.properties` 中修改
5. **测试开关**通过 `app.run-test-on-startup` 控制是否自动运行测试

## 常见问题

**Q: 找不到二维码元素？**  
A: 打开浏览器开发者工具，检查二维码元素的 HTML 结构，更新定位器。

**Q: 截图为空或损坏？**  
A: 增加页面加载等待时间，确保二维码完全渲染。

**Q: ChromeDriver 版本不匹配？**  
A: WebDriverManager 会自动匹配，如仍有问题可手动指定版本。

**Q: 如何调试定位器？**  
A: 在 locator 函数中添加日志，打印找到的元素信息。

## 联系方式

如有问题或建议，请提交 Issue 或联系开发团队。
