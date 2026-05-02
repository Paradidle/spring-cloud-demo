# Linux 服务器部署指南

## 📋 前置要求

### 1. 安装 Chrome 浏览器

```bash
# CentOS/RHEL
sudo yum install -y wget
wget https://dl.google.com/linux/direct/google-chrome-stable_current_x86_64.rpm
sudo yum install -y ./google-chrome-stable_current_x86_64.rpm

# Ubuntu/Debian
wget https://dl.google.com/linux/direct/google-chrome-stable_current_amd64.deb
sudo dpkg -i google-chrome-stable_current_amd64.deb
sudo apt-get install -f -y
```

验证安装:
```bash
google-chrome --version
```

### 2. 确认 ChromeDriver 版本匹配

项目已包含 `chromedriver.exe` (Windows版本),需要在Linux上替换为Linux版本:

```bash
# 下载与Chrome版本匹配的ChromeDriver
CHROME_VERSION=$(google-chrome --version | awk '{print $3}' | cut -d. -f1)
wget https://chromedriver.storage.googleapis.com/LATEST_RELEASE_${CHROME_VERSION}
LATEST_VERSION=$(cat LATEST_RELEASE_${CHROME_VERSION})
wget https://chromedriver.storage.googleapis.com/${LATEST_VERSION}/chromedriver_linux64.zip
unzip chromedriver_linux64.zip
chmod +x chromedriver

# 替换项目中的ChromeDriver
cp chromedriver crawler-api/src/main/resources/chromedriver
rm chromedriver chromedriver_linux64.zip LATEST_RELEASE_*
```

## 🚀 部署步骤

### 1. 上传项目到服务器

```bash
# 方式1: Git克隆
git clone <your-repo-url>
cd spring-cloud-demo/crawler-api

# 方式2: 直接上传jar包
scp target/crawler-api-0.0.1-SNAPSHOT.jar user@server:/opt/crawler-api/
```

### 2. 配置无头模式

创建或修改配置文件 `application-prod.yml`:

```yaml
crawler:
  chrome:
    headless: true          # 启用无头模式
    disable-gpu: true       # 禁用GPU加速
    window-size: 1920,1080  # 设置窗口大小
```

或者在启动时通过JVM参数设置:

```bash
java -Dchrome.headless=true -jar crawler-api-0.0.1-SNAPSHOT.jar
```

### 3. 设置文件权限

```bash
# 确保ChromeDriver有执行权限
chmod +x crawler-api/src/main/resources/chromedriver

# 确保数据目录可写
mkdir -p ~/.crawler-chrome-profile-xiaohongshu
chmod 755 ~/.crawler-chrome-profile-xiaohongshu
```

### 4. 启动应用

```bash
# 开发环境（有界面模式）
java -jar crawler-api-0.0.1-SNAPSHOT.jar

# 生产环境（无头模式）
java -Dchrome.headless=true -jar crawler-api-0.0.1-SNAPSHOT.jar

# 后台运行
nohup java -Dchrome.headless=true -jar crawler-api-0.0.1-SNAPSHOT.jar > crawler.log 2>&1 &
```

### 5. 使用 systemd 管理（推荐）

创建服务文件 `/etc/systemd/system/crawler-api.service`:

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

# 环境变量
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

查看日志:

```bash
sudo journalctl -u crawler-api -f
```

## 🔧 常见问题

### 1. ChromeDriver 版本不匹配

**错误信息**: `session not created: This version of ChromeDriver only supports Chrome version XX`

**解决方案**:
```bash
# 检查Chrome版本
google-chrome --version

# 下载对应版本的ChromeDriver
# 访问 https://chromedriver.chromium.org/downloads 下载匹配版本
```

### 2. 缺少依赖库

**错误信息**: `error while loading shared libraries: libXXX.so`

**解决方案** (CentOS):
```bash
sudo yum install -y atk cups-libs gtk3 libXcomposite alsa-lib \
  libXcursor libXdamage libXext libXi libXrandr libXScrnSaver \
  libXtst pango at-spi2-atk libdrm libgbm libxcb libxkbcommon
```

**解决方案** (Ubuntu):
```bash
sudo apt-get install -y libatk1.0-0 libcups2 libgtk-3-0 libxcomposite1 \
  libasound2 libxcursor1 libxdamage1 libxext6 libxi6 libxrandr2 \
  libxss1 libxtst6 pango1.0 libdrm2 libgbm1 libxcb1 libxkbcommon0
```

### 3. 沙箱权限问题

**错误信息**: `Running as root without --no-sandbox is not supported`

**解决方案**: 代码中已添加 `--no-sandbox` 参数,无需额外配置。

### 4. 共享内存不足

**错误信息**: `DevToolsActivePort file doesn't exist`

**解决方案**: 代码中已添加 `--disable-dev-shm-usage` 参数。

### 5. 二维码无法扫描

在无头模式下,二维码会保存到文件中:

```bash
# 查看二维码位置
ls -lh crawler-api/src/main/resources/qrcode/

# 复制二维码到本地扫描
scp user@server:/opt/crawler-api/src/main/resources/qrcode/*.png ./
```

## 📊 性能优化

### 1. JVM 参数调优

```bash
java -Dchrome.headless=true \
     -Xms256m \
     -Xmx512m \
     -XX:+UseG1GC \
     -XX:MaxGCPauseMillis=200 \
     -jar crawler-api-0.0.1-SNAPSHOT.jar
```

### 2. Chrome 参数优化

已在代码中配置:
- `--no-sandbox`: 禁用沙箱(服务器必需)
- `--disable-dev-shm-usage`: 避免共享内存问题
- `--disable-gpu`: 无头模式禁用GPU
- `--headless=new`: 使用新版无头模式

### 3. 会话管理

- 会话自动清理: 24小时过期
- 建议定期重启服务释放资源

## 🔍 监控和调试

### 1. 查看Chrome进程

```bash
ps aux | grep chrome
ps aux | grep chromedriver
```

### 2. 启用调试日志

```bash
java -Dchrome.headless=true \
     -Dlogging.level.com.paradidle.crawler.api=DEBUG \
     -jar crawler-api-0.0.1-SNAPSHOT.jar
```

### 3. 检查HTML调试文件

```bash
ls -lh crawler-api/src/main/resources/debughtml/
```

## ⚠️ 注意事项

1. **ChromeDriver版本必须与Chrome浏览器版本匹配**
2. **Linux服务器必须安装Chrome浏览器**(不仅仅是ChromeDriver)
3. **无头模式下无法手动操作浏览器**,所有交互必须通过代码完成
4. **定期清理会话和数据目录**,避免磁盘空间占满
5. **生产环境建议使用systemd管理服务**,确保自动重启

## 📞 技术支持

如遇到问题,请检查:
1. Chrome和ChromeDriver版本是否匹配
2. 是否有足够的系统权限
3. 系统依赖库是否完整
4. 日志文件中的详细错误信息

```bash
# 查看完整日志
tail -f /var/log/crawler-api.log
# 或
sudo journalctl -u crawler-api -n 100 --no-pager
```
