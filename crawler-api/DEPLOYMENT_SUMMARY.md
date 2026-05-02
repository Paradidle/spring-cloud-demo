# Linux部署配置说明

## ✅ 已完成的修改

### 1. ChromeDriver管理优化

**问题**: 原项目使用WebDriverManager自动下载ChromeDriver,会依赖本地Chrome浏览器版本,不适合Linux服务器部署。

**解决方案**:
- ✅ 优先使用项目中的ChromeDriver (`src/main/resources/chromedriver`)
- ✅ 自动检测操作系统(Windows/Linux)
- ✅ Windows使用`chromedriver.exe`,Linux使用`chromedriver`
- ✅ 如果项目中没有ChromeDriver,降级使用WebDriverManager

**修改文件**:
- `BaseCrawlerService.java`: 添加`configureChromeOptions()`、`getChromeDriverPath()`等方法
- `BrowserSessionManager.java`: 添加相同的配置逻辑

### 2. 无头模式支持

**问题**: Linux服务器通常没有图形界面,需要无头模式运行Chrome。

**解决方案**:
- ✅ 自动检测Linux环境,自动启用无头模式
- ✅ 支持通过JVM参数 `-Dchrome.headless=true` 手动启用
- ✅ 配置必要的Chrome参数:
  - `--headless=new`: 使用新版无头模式
  - `--no-sandbox`: 禁用沙箱(Linux必需)
  - `--disable-dev-shm-usage`: 避免共享内存问题
  - `--disable-gpu`: 禁用GPU加速
  - `--window-size=1920,1080`: 设置窗口大小

### 3. 配置文件

创建了 `application.yml`:
```yaml
crawler:
  chrome:
    headless: false          # 默认开发环境有界面
    disable-gpu: true
    window-size: 1920,1080
    user-data-dir: ${user.home}/.crawler-chrome-profile
    driver-cache-dir: ${user.home}/.cache/selenium
```

### 4. 部署脚本和文档

- ✅ `prepare-chromedriver.sh`: 自动下载匹配Chrome版本的Linux ChromeDriver
- ✅ `LINUX_DEPLOYMENT.md`: 详细的Linux部署指南
- ✅ 更新 `README.md`: 添加Linux部署快速开始

## 📋 Linux部署步骤

### 前置要求

1. **安装Java 17+**
```bash
sudo yum install java-17-openjdk-devel  # CentOS
sudo apt-get install openjdk-17-jdk     # Ubuntu
```

2. **安装Chrome浏览器**
```bash
# CentOS/RHEL
wget https://dl.google.com/linux/direct/google-chrome-stable_current_x86_64.rpm
sudo yum install -y ./google-chrome-stable_current_x86_64.rpm

# Ubuntu/Debian  
wget https://dl.google.com/linux/direct/google-chrome-stable_current_amd64.deb
sudo dpkg -i google-chrome-stable_current_amd64.deb
sudo apt-get install -f -y
```

3. **安装系统依赖**
```bash
# CentOS
sudo yum install -y atk cups-libs gtk3 libXcomposite alsa-lib \
  libXcursor libXdamage libXext libXi libXrandr libXScrnSaver \
  libXtst pango at-spi2-atk libdrm libgbm libxcb libxkbcommon

# Ubuntu
sudo apt-get install -y libatk1.0-0 libcups2 libgtk-3-0 libxcomposite1 \
  libasound2 libxcursor1 libxdamage1 libxext6 libxi6 libxrandr2 \
  libxss1 libxtst6 pango1.0 libdrm2 libgbm1 libxcb1 libxkbcommon0
```

### 部署流程

1. **上传项目到服务器**
```bash
# 方式1: Git克隆
git clone <your-repo-url>
cd spring-cloud-demo/crawler-api

# 方式2: 上传jar包
scp target/crawler-api-0.0.1-SNAPSHOT.jar user@server:/opt/crawler-api/
```

2. **准备ChromeDriver**
```bash
chmod +x prepare-chromedriver.sh
./prepare-chromedriver.sh
```

3. **启动应用**
```bash
# 前台运行(测试用)
java -Dchrome.headless=true -jar crawler-api-0.0.1-SNAPSHOT.jar

# 后台运行
nohup java -Dchrome.headless=true -jar crawler-api-0.0.1-SNAPSHOT.jar > crawler.log 2>&1 &
```

4. **使用systemd管理(推荐)**

创建 `/etc/systemd/system/crawler-api.service`:
```ini
[Unit]
Description=Crawler API Service
After=network.target

[Service]
Type=simple
User=crawler
WorkingDirectory=/opt/crawler-api
ExecStart=/usr/bin/java -Dchrome.headless=true -jar /opt/crawler-api/crawler-api-0.0.1-SNAPSHOT.jar
Restart=on-failure
RestartSec=10
Environment="JAVA_OPTS=-Xmx512m -Xms256m"

[Install]
WantedBy=multi-user.target
```

启动服务:
```bash
sudo systemctl daemon-reload
sudo systemctl enable crawler-api
sudo systemctl start crawler-api
sudo systemctl status crawler-api
```

## 🔍 验证部署

### 1. 检查ChromeDriver
```bash
ls -lh src/main/resources/chromedriver
./src/main/resources/chromedriver --version
```

### 2. 检查Chrome
```bash
google-chrome --version
```

### 3. 查看应用日志
```bash
# 查看启动日志
tail -f crawler.log

# 或查看systemd日志
sudo journalctl -u crawler-api -f
```

### 4. 测试API
```bash
# 假设服务运行在8081端口
curl http://localhost:8081/actuator/health
```

## ⚠️ 常见问题

### Q1: ChromeDriver版本不匹配
**错误**: `session not created: This version of ChromeDriver only supports Chrome version XX`

**解决**: 
```bash
# 重新运行脚本下载匹配的ChromeDriver
./prepare-chromedriver.sh
```

### Q2: 缺少共享库
**错误**: `error while loading shared libraries: libXXX.so`

**解决**: 安装对应的系统依赖包(见上文"安装系统依赖")

### Q3: 权限问题
**错误**: `Running as root without --no-sandbox is not supported`

**解决**: 代码中已添加`--no-sandbox`参数,无需额外配置。建议不要以root用户运行。

### Q4: 二维码无法扫描
在无头模式下,二维码会保存到文件:
```bash
ls -lh src/main/resources/qrcode/
# 复制二维码到本地
scp user@server:/opt/crawler-api/src/main/resources/qrcode/*.png ./
```

## 📊 性能调优

### JVM参数
```bash
java -Dchrome.headless=true \
     -Xms256m \
     -Xmx512m \
     -XX:+UseG1GC \
     -XX:MaxGCPauseMillis=200 \
     -jar crawler-api-0.0.1-SNAPSHOT.jar
```

### Chrome参数
已在代码中优化:
- `--no-sandbox`: 禁用沙箱
- `--disable-dev-shm-usage`: 避免共享内存问题
- `--disable-gpu`: 无头模式禁用GPU
- `--headless=new`: 使用新版无头模式

## 🎯 关键改进点

| 项目 | 修改前 | 修改后 |
|------|--------|--------|
| ChromeDriver来源 | WebDriverManager自动下载 | 优先使用项目自带,降级到WebDriverManager |
| 环境适配 | 仅支持Windows | 自动检测Windows/Linux |
| 无头模式 | 不支持 | 自动启用(Linux)或手动启用 |
| 部署难度 | 需要手动配置 | 提供自动化脚本和详细文档 |
| 版本匹配 | 依赖本地Chrome | 脚本自动下载匹配版本 |

## 📝 下一步建议

1. **添加健康检查接口**: 监控Chrome进程状态
2. **会话监控**: 定期清理过期会话
3. **资源限制**: 限制最大并发会话数
4. **日志轮转**: 配置logback防止日志文件过大
5. **监控告警**: 集成Prometheus/Grafana监控

## 🔗 相关文档

- [LINUX_DEPLOYMENT.md](LINUX_DEPLOYMENT.md): 详细部署指南
- [README.md](README.md): 项目说明
- [USAGE.md](USAGE.md): 使用指南
