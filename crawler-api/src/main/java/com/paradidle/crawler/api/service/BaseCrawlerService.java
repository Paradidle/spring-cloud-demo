package com.paradidle.crawler.api.service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.function.Function;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public abstract class BaseCrawlerService {

    protected WebDriver driver;
    protected WebDriverWait wait;

    public void initDriver() {
        ChromeOptions options = new ChromeOptions();
        
        // 配置Chrome选项
        configureChromeOptions(options);
        
        this.driver = new ChromeDriver(options);
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(300));
    }
    
    /**
     * 配置Chrome浏览器选项
     */
    protected void configureChromeOptions(ChromeOptions options) {
        // 设置ChromeDriver路径（使用项目中的chromedriver）
        String driverPath = getChromeDriverPath();
        if (driverPath != null && !driverPath.isEmpty()) {
            System.setProperty("webdriver.chrome.driver", driverPath);
            System.out.println("使用项目中的ChromeDriver: " + driverPath);
        } else {
            // 如果项目中没有，使用WebDriverManager自动下载
            io.github.bonigarcia.wdm.WebDriverManager.chromedriver().setup();
            System.out.println("使用WebDriverManager管理的ChromeDriver");
        }
        
        // 基本配置
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        
        // 无头模式配置（Linux服务器部署时需要）
        boolean headless = Boolean.parseBoolean(System.getProperty("chrome.headless", "false"));
        if (headless || isLinuxEnvironment()) {
            options.addArguments("--headless=new");
            options.addArguments("--window-size=1920,1080");
            options.addArguments("--disable-gpu");
            System.out.println("启用无头模式");
        } else {
            options.addArguments("--start-maximized");
        }
        
        // 用户数据目录
        options.addArguments("--user-data-dir=" + getUserDataDir());
    }
    
    /**
     * 获取项目中ChromeDriver的路径
     */
    private String getChromeDriverPath() {
        try {
            String projectRoot = findProjectRoot();
            String driverFileName = isWindowsEnvironment() ? "chromedriver.exe" : "chromedriver";
            File driverFile = new File(projectRoot, "src/main/resources/" + driverFileName);
            
            if (driverFile.exists()) {
                // 确保在Linux上有执行权限
                if (!isWindowsEnvironment()) {
                    driverFile.setExecutable(true);
                }
                return driverFile.getAbsolutePath();
            }
        } catch (Exception e) {
            System.err.println("获取ChromeDriver路径失败: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * 判断是否为Linux环境
     */
    private boolean isLinuxEnvironment() {
        String os = System.getProperty("os.name").toLowerCase();
        return os.contains("linux") || os.contains("nix") || os.contains("nux");
    }
    
    /**
     * 判断是否为Windows环境
     */
    private boolean isWindowsEnvironment() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }

    private String getUserDataDir() {
        String userHome = System.getProperty("user.home");
        String userDataDir = userHome + "/.crawler-chrome-profile";
        File dir = new File(userDataDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return userDataDir;
    }

    public void closeDriver() {
        if (driver != null) {
            driver.quit();
        }
    }

    public void navigateTo(String url) {
        driver.get(url);
    }

    public File captureQrCodeWithCustomLocator(Function<WebDriver, WebElement> locator, String outputPath) {
        try {
            WebElement qrElement = wait.until(driver -> locator.apply(driver));
            
            String base64Src = qrElement.getAttribute("src");
            if (base64Src != null && base64Src.startsWith("data:image")) {
                String base64Data = base64Src.split(",")[1];
                byte[] imageBytes = java.util.Base64.getDecoder().decode(base64Data);
                
                File qrFile = new File(outputPath);
                File parentDir = qrFile.getParentFile();
                if (parentDir != null && !parentDir.exists()) {
                    parentDir.mkdirs();
                }
                
                java.nio.file.Files.write(qrFile.toPath(), imageBytes);
                System.out.println("二维码已保存到: " + qrFile.getAbsolutePath());
                
                return qrFile;
            } else {
                throw new RuntimeException("未找到有效的二维码图片数据");
            }
        } catch (Exception e) {
            throw new RuntimeException("截取二维码失败: " + e.getMessage(), e);
        }
    }

    public String getModuleResourcePath(String fileName) {
        String projectRoot = findProjectRoot();
        File resourceDir = new File(projectRoot, "src/main/resources/qrcode");
        if (!resourceDir.exists()) {
            resourceDir.mkdirs();
        }
        return new File(resourceDir, fileName).getAbsolutePath();
    }

    public void saveDebugHtml(String fileName, String sessionId) {
        try {
            String projectRoot = findProjectRoot();
            File debugDir = new File(projectRoot, "src/main/resources/debughtml");
            if (!debugDir.exists()) {
                debugDir.mkdirs();
            }
            
            String uniqueFileName = sessionId + "_" + fileName;
            File htmlFile = new File(debugDir, uniqueFileName);
            
            String pageSource = driver.getPageSource();
            try (FileWriter writer = new FileWriter(htmlFile)) {
                writer.write(pageSource);
            }
            
            System.out.println("调试HTML已保存到: " + htmlFile.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("保存调试HTML失败: " + e.getMessage());
        }
    }

    private String findProjectRoot() {
        String currentDir = System.getProperty("user.dir");
        File dir = new File(currentDir);
        
        while (dir != null) {
            if (new File(dir, "crawler-api").exists()) {
                return new File(dir, "crawler-api").getAbsolutePath();
            }
            dir = dir.getParentFile();
        }
        
        return currentDir;
    }

    protected WebElement waitForElement(By locator, String value) {
        return wait.until(ExpectedConditions.presenceOfElementLocated(locator));
    }

    protected WebElement waitForElementClickable(By locator, String value) {
        return wait.until(ExpectedConditions.elementToBeClickable(locator));
    }

    protected List<WebElement> findElements(By locator) {
        return driver.findElements(locator);
    }

    protected void clickElement(By locator, String value) {
        WebElement element = waitForElementClickable(locator, value);
        element.click();
    }

    protected void inputText(By locator, String value, String text) {
        WebElement element = waitForElement(locator, value);
        element.clear();
        element.sendKeys(text);
    }

    protected String getPageSource() {
        return driver.getPageSource();
    }

    protected String getCurrentUrl() {
        return driver.getCurrentUrl();
    }
}
