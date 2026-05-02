#!/bin/bash

# Linux环境下准备ChromeDriver的脚本
# 使用方法: chmod +x prepare-chromedriver.sh && ./prepare-chromedriver.sh

set -e

echo "========================================="
echo "  ChromeDriver 自动配置脚本"
echo "========================================="
echo ""

# 检测操作系统
if [ "$(uname)" != "Linux" ]; then
    echo "⚠️  警告: 此脚本仅适用于Linux系统"
    echo "当前系统: $(uname)"
    read -p "是否继续? (y/n): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
fi

# 检查Chrome是否安装
echo "📌 检查Chrome浏览器..."
if command -v google-chrome &> /dev/null; then
    CHROME_VERSION=$(google-chrome --version | awk '{print $3}')
    echo "✓ Chrome已安装,版本: $CHROME_VERSION"
elif command -v google-chrome-stable &> /dev/null; then
    CHROME_VERSION=$(google-chrome-stable --version | awk '{print $3}')
    echo "✓ Chrome已安装,版本: $CHROME_VERSION"
else
    echo "✗ Chrome未安装!"
    echo ""
    echo "请先安装Chrome浏览器:"
    echo "  CentOS/RHEL: sudo yum install -y google-chrome-stable"
    echo "  Ubuntu/Debian: sudo apt-get install -y google-chrome-stable"
    exit 1
fi

# 获取Chrome主版本号
CHROME_MAJOR_VERSION=$(echo $CHROME_VERSION | cut -d. -f1)
echo "  主版本号: $CHROME_MAJOR_VERSION"
echo ""

# 检查项目目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$SCRIPT_DIR"

# 如果在scripts目录中,则回到项目根目录
if [[ "$PROJECT_ROOT" == *"/scripts" ]]; then
    PROJECT_ROOT="$(dirname "$PROJECT_ROOT")"
fi

RESOURCES_DIR="$PROJECT_ROOT/src/main/resources"
echo "📌 项目资源目录: $RESOURCES_DIR"

if [ ! -d "$RESOURCES_DIR" ]; then
    echo "✗ 错误: 找不到资源目录 $RESOURCES_DIR"
    exit 1
fi

echo ""

# 下载ChromeDriver
echo "📥 下载ChromeDriver..."
TEMP_DIR=$(mktemp -d)
cd "$TEMP_DIR"

# 尝试从官方源下载
echo "  尝试从Chrome for Testing下载..."
if command -v curl &> /dev/null; then
    DOWNLOAD_CMD="curl -sL"
elif command -v wget &> /dev/null; then
    DOWNLOAD_CMD="wget -q -O-"
else
    echo "✗ 错误: 需要安装curl或wget"
    exit 1
fi

# 获取最新的ChromeDriver版本信息
LATEST_RELEASE_URL="https://googlechromelabs.github.io/chrome-for-testing/latest-patch-versions-per-build.json"
CHROMEDRIVER_URL=""

# 对于较新版本的Chrome (115+),使用新的下载方式
if [ "$CHROME_MAJOR_VERSION" -ge 115 ]; then
    echo "  Chrome版本 >= 115,使用新的下载方式"
    
    # 获取特定版本的ChromeDriver
    VERSIONS_JSON=$($DOWNLOAD_CMD "https://googlechromelabs.github.io/chrome-for-testing/known-good-versions-with-downloads.json")
    
    # 查找匹配的ChromeDriver版本
    CHROMEDRIVER_INFO=$(echo "$VERSIONS_JSON" | python3 -c "
import sys, json
data = json.load(sys.stdin)
target_version = '$CHROME_MAJOR_VERSION'
for version in reversed(data['versions']):
    v_num = version['version'].split('.')[0]
    if v_num == target_version and 'chromedriver' in version.get('downloads', {}):
        downloads = version['downloads']['chromedriver']
        for d in downloads:
            if d['platform'] == 'linux64':
                print(d['url'])
                sys.exit(0)
" 2>/dev/null || echo "")
    
    if [ -n "$CHROMEDRIVER_INFO" ]; then
        CHROMEDRIVER_URL="$CHROMEDRIVER_INFO"
        echo "  找到ChromeDriver: $CHROMEDRIVER_URL"
    else
        echo "  ⚠️  未找到精确匹配的版本,尝试使用最新版本"
    fi
fi

# 如果还没找到,使用旧的方式
if [ -z "$CHROMEDRIVER_URL" ]; then
    echo "  尝试从旧版存储库下载..."
    LATEST_VERSION=$($DOWNLOAD_CMD "https://chromedriver.storage.googleapis.com/LATEST_RELEASE_${CHROME_MAJOR_VERSION}" 2>/dev/null || echo "")
    
    if [ -n "$LATEST_VERSION" ]; then
        CHROMEDRIVER_URL="https://chromedriver.storage.googleapis.com/${LATEST_VERSION}/chromedriver_linux64.zip"
        echo "  ChromeDriver版本: $LATEST_VERSION"
    else
        echo "✗ 无法找到匹配的ChromeDriver版本"
        exit 1
    fi
fi

# 下载ChromeDriver
echo "  下载地址: $CHROMEDRIVER_URL"
if command -v curl &> /dev/null; then
    curl -sL "$CHROMEDRIVER_URL" -o chromedriver.zip
elif command -v wget &> /dev/null; then
    wget -q "$CHROMEDRIVER_URL" -O chromedriver.zip
fi

if [ ! -f "chromedriver.zip" ]; then
    echo "✗ 下载失败"
    exit 1
fi

# 解压
echo "  解压文件..."
unzip -q chromedriver.zip
chmod +x chromedriver

# 备份旧的ChromeDriver
if [ -f "$RESOURCES_DIR/chromedriver" ]; then
    echo "  备份旧的ChromeDriver..."
    mv "$RESOURCES_DIR/chromedriver" "$RESOURCES_DIR/chromedriver.bak.$(date +%Y%m%d%H%M%S)"
fi

# 复制新的ChromeDriver
echo "  复制到项目目录..."
cp chromedriver "$RESOURCES_DIR/chromedriver"

# 清理临时文件
cd - > /dev/null
rm -rf "$TEMP_DIR"

echo ""
echo "✅ ChromeDriver配置完成!"
echo ""
echo "📍 位置: $RESOURCES_DIR/chromedriver"
echo "📋 版本信息:"
"$RESOURCES_DIR/chromedriver" --version
echo ""
echo "🎯 下一步:"
echo "  1. 确保ChromeDriver有执行权限: chmod +x $RESOURCES_DIR/chromedriver"
echo "  2. 启动应用时使用无头模式: java -Dchrome.headless=true -jar crawler-api.jar"
echo ""
