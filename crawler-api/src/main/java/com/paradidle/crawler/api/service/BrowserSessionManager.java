package com.paradidle.crawler.api.service;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.stereotype.Component;

@Component
public class BrowserSessionManager {

    private final Map<String, BrowserSession> sessions = new ConcurrentHashMap<>();
    private static final long SESSION_MAX_AGE = 24 * 60 * 60 * 1000; // 24小时

    public BrowserSession createSession(String platform) {
        cleanupExpiredSessions();
        
        ChromeOptions options = new ChromeOptions();
        configureChromeOptions(options, platform);
        
        ChromeDriver driver = new ChromeDriver(options);
        BrowserSession session = new BrowserSession(platform, driver);
        
        sessions.put(session.getSessionId(), session);
        return session;
    }
    
    /**
     * 配置Chrome浏览器选项
     */
    private void configureChromeOptions(ChromeOptions options, String platform) {
        // 设置ChromeDriver路径（使用项目中的chromedriver）
        String driverPath = getChromeDriverPath();
        if (driverPath != null && !driverPath.isEmpty()) {
            System.setProperty("webdriver.chrome.driver", driverPath);
            System.out.println("会话[" + platform + "]使用项目中的ChromeDriver: " + driverPath);
        } else {
            // 如果项目中没有，使用WebDriverManager自动下载
            io.github.bonigarcia.wdm.WebDriverManager.chromedriver().setup();
            System.out.println("会话[" + platform + "]使用WebDriverManager管理的ChromeDriver");
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
            System.out.println("会话[" + platform + "]启用无头模式");
        } else {
            options.addArguments("--start-maximized");
        }
        
        // 用户数据目录
        options.addArguments("--user-data-dir=" + getUserDataDir(platform));
    }
    
    /**
     * 获取项目中ChromeDriver的路径
     */
    private String getChromeDriverPath() {
        try {
            String userDir = System.getProperty("user.dir");
            String driverFileName = isWindowsEnvironment() ? "chromedriver.exe" : "chromedriver";
            
            // 尝试多个可能的路径
            String[] possiblePaths = {
                userDir + "/src/main/resources/" + driverFileName,
                userDir + "/../crawler-api/src/main/resources/" + driverFileName,
                userDir + "/../../crawler-api/src/main/resources/" + driverFileName
            };
            
            for (String path : possiblePaths) {
                File driverFile = new File(path);
                if (driverFile.exists()) {
                    // 确保在Linux上有执行权限
                    if (!isWindowsEnvironment()) {
                        driverFile.setExecutable(true);
                    }
                    return driverFile.getAbsolutePath();
                }
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

    public BrowserSession getSession(String sessionId) {
        return sessions.get(sessionId);
    }

    public void closeSession(String sessionId) {
        BrowserSession session = sessions.remove(sessionId);
        if (session != null && session.getDriver() != null) {
            session.getDriver().quit();
        }
    }

    public void closeAllSessions() {
        sessions.values().forEach(session -> {
            if (session.getDriver() != null) {
                session.getDriver().quit();
            }
        });
        sessions.clear();
    }

    public int getActiveSessionCount() {
        return sessions.size();
    }

    private String getUserDataDir(String platform) {
        String userHome = System.getProperty("user.home");
        String userDataDir = userHome + "/.crawler-chrome-profile-" + platform;
        File dir = new File(userDataDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return userDataDir;
    }

    private void cleanupExpiredSessions() {
        sessions.entrySet().removeIf(entry -> {
            if (entry.getValue().isExpired(SESSION_MAX_AGE)) {
                if (entry.getValue().getDriver() != null) {
                    entry.getValue().getDriver().quit();
                }
                return true;
            }
            return false;
        });
    }
}
