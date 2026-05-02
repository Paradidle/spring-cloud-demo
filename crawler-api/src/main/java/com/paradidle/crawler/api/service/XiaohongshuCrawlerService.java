package com.paradidle.crawler.api.service;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paradidle.crawler.api.model.XiaohongshuPost;

@Service
public class XiaohongshuCrawlerService extends BaseCrawlerService {

    private static final String BASE_URL = "https://www.xiaohongshu.com/explore";
    private static final String HOME_URL = "https://www.xiaohongshu.com";
    private static final String PLATFORM = "xiaohongshu";

    public String loginWithQrCode(BrowserSessionManager sessionManager) {
        BrowserSession session = sessionManager.createSession(PLATFORM);
        this.driver = session.getDriver();
        this.wait = session.getWait();
        
        try {
            navigateTo(HOME_URL);
            Thread.sleep(3000);
            
            WebElement searchInput = waitForElement(By.cssSelector("input#search-input"), "搜索输入框");
            String placeholder = searchInput.getAttribute("placeholder");
            
            if (placeholder != null && !placeholder.contains("登录")) {
                System.out.println("检测到已登录状态，会话ID: " + session.getSessionId());
                saveDebugHtml("already_logged_in.html", session.getSessionId());
                return session.getSessionId();
            }
            
            saveDebugHtml("login_before.html", session.getSessionId());
            
            File qrFile = captureQrCodeWithCustomLocator(
                driver -> driver.findElement(By.cssSelector("img.qrcode-img")),
                getModuleResourcePath(session.getSessionId() + "_xiaohongshu_qr.png")
            );
            
            System.out.println("请扫描二维码登录小红书，会话ID: " + session.getSessionId());
            
            return session.getSessionId();
        } catch (Exception e) {
            sessionManager.closeSession(session.getSessionId());
            throw new RuntimeException("小红书登录失败: " + e.getMessage(), e);
        }
    }

    private void waitForLoginSuccess() {
        try {
            wait.until(driver -> {
                String currentUrl = driver.getCurrentUrl();
                return !currentUrl.contains("login") && !currentUrl.equals(BASE_URL);
            });
            System.out.println("登录成功！");
        } catch (Exception e) {
            System.out.println("等待登录超时，请手动检查");
        }
    }

    public List<XiaohongshuPost> searchPosts(String keyword, BrowserSessionManager sessionManager, String sessionId) {
        BrowserSession session = sessionManager.getSession(sessionId);
        if (session == null) {
            throw new RuntimeException("会话不存在或已过期，请重新登录");
        }
        
        this.driver = session.getDriver();
        this.wait = session.getWait();
        
        try {
            navigateTo(BASE_URL);
            Thread.sleep(2000);
            
            saveDebugHtml("search_before.html", sessionId);
            
            WebElement searchInput = waitForElement(By.cssSelector("input#search-input"), "搜索输入框");
            String placeholder = searchInput.getAttribute("placeholder");
            
            if (placeholder != null && placeholder.contains("登录")) {
                throw new RuntimeException("未登录或登录已过期，请重新扫码登录");
            }
            
            searchInput.clear();
            searchInput.sendKeys(keyword);
            Thread.sleep(500);
            
            try {
                WebElement searchButton = driver.findElement(By.cssSelector("div.input-button div.search-icon"));
                searchButton.click();
            } catch (Exception e) {
                System.out.println("未找到搜索按钮，尝试使用回车键");
                searchInput.sendKeys(org.openqa.selenium.Keys.ENTER);
            }
            
            Thread.sleep(5000);
            
            for (int i = 0; i < 3; i++) {
                ((org.openqa.selenium.JavascriptExecutor) driver).executeScript("window.scrollBy(0, 500)");
                Thread.sleep(1000);
            }
            
            saveDebugHtml("search_result.html", sessionId);
            
            List<XiaohongshuPost> posts = parsePostsFromDOM();
            System.out.println("搜索到 " + posts.size() + " 个帖子");
            return posts;
        } catch (Exception e) {
            throw new RuntimeException("搜索失败: " + e.getMessage(), e);
        }
    }

    public XiaohongshuPost getPostDetail(String noteId, String xsecToken, BrowserSessionManager sessionManager, String sessionId) {
        BrowserSession session = sessionManager.getSession(sessionId);
        if (session == null) {
            throw new RuntimeException("会话不存在或已过期，请重新登录");
        }
        
        this.driver = session.getDriver();
        this.wait = session.getWait();
        
        try {
            String postUrl = "https://www.xiaohongshu.com/explore/" + noteId + "?xsec_token=" + xsecToken;
            navigateTo(postUrl);
            Thread.sleep(3000);
            
            saveDebugHtml("post_detail_" + noteId + ".html", sessionId);
            
            XiaohongshuPost post = new XiaohongshuPost();
            post.setNoteId(noteId);
            post.setXsecToken(xsecToken);
            post.setUrl(postUrl);
            
            // 使用DOM解析提取标题和内容
            parsePostDetailFromDOM(post);
            
            return post;
        } catch (Exception e) {
            throw new RuntimeException("获取帖子详情失败: " + e.getMessage(), e);
        }
    }

    private List<XiaohongshuPost> parsePostsFromDOM() {
        List<XiaohongshuPost> posts = new ArrayList<>();
        
        try {
            List<WebElement> noteItems = driver.findElements(By.cssSelector("div.note-item"));
            System.out.println("找到 " + noteItems.size() + " 个帖子元素");
            
            for (WebElement noteItem : noteItems) {
                try {
                    XiaohongshuPost post = parseNoteItem(noteItem);
                    if (post != null && post.getNoteId() != null) {
                        posts.add(post);
                    }
                } catch (Exception e) {
                    System.err.println("解析单个帖子失败: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("DOM解析失败: " + e.getMessage());
            e.printStackTrace();
        }
        
        return posts;
    }

    private XiaohongshuPost parseNoteItem(WebElement noteItem) {
        XiaohongshuPost post = new XiaohongshuPost();
        
        try {
            WebElement titleLink = noteItem.findElement(By.cssSelector("a.title"));
            String href = titleLink.getAttribute("href");
            if (href != null && !href.startsWith("http")) {
                post.setUrl("https://www.xiaohongshu.com" + href);
            } else {
                post.setUrl(href);
            }
            
            String[] urlParts = href.split("\\?")[0].split("/");
            for (String part : urlParts) {
                if (part.length() == 24 && part.matches("^[0-9a-f]+$")) {
                    post.setNoteId(part);
                    break;
                }
            }
            
            if (href.contains("xsec_token=")) {
                String queryPart = href.split("\\?")[1];
                String[] params = queryPart.split("&");
                for (String param : params) {
                    if (param.startsWith("xsec_token=")) {
                        post.setXsecToken(param.substring(11));
                    }
                }
            }
            
            try {
                WebElement titleSpan = titleLink.findElement(By.tagName("span"));
                post.setTitle(titleSpan.getText());
            } catch (Exception e) {
                post.setTitle("");
            }
            
            try {
                WebElement authorElement = noteItem.findElement(By.cssSelector("div.name"));
                post.setAuthor(authorElement.getText());
            } catch (Exception e) {
                post.setAuthor("");
            }
            
            try {
                WebElement imgElement = noteItem.findElement(By.cssSelector("img"));
                post.setCoverImage(imgElement.getAttribute("src"));
            } catch (Exception e) {
                post.setCoverImage("");
            }
            
            return post;
        } catch (Exception e) {
            return null;
        }
    }

    private List<XiaohongshuPost> parsePostsFromJson() {
        List<XiaohongshuPost> posts = new ArrayList<>();
        
        try {
            String pageSource = driver.getPageSource();
            int startIndex = pageSource.indexOf("window.__INITIAL_STATE__=");
            if (startIndex == -1) {
                System.err.println("未找到页面数据");
                return posts;
            }
            
            int endIndex = pageSource.indexOf("</script>", startIndex);
            if (endIndex == -1) {
                System.err.println("未找到脚本结束标记");
                return posts;
            }
            
            String jsonStr = pageSource.substring(startIndex + 25, endIndex);
            
            // 替换 JavaScript undefined 为 JSON null
            jsonStr = jsonStr.replaceAll(":\\s*undefined([,}\\]])", ":null$1");
            
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(jsonStr);
            JsonNode searchNode = root.path("search").path("feeds");
            
            if (searchNode.isArray()) {
                for (JsonNode feedNode : searchNode) {
                    try {
                        XiaohongshuPost post = parsePostFromJson(feedNode);
                        if (post != null) {
                            posts.add(post);
                        }
                    } catch (Exception e) {
                        System.err.println("解析单个帖子失败: " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("JSON解析失败: " + e.getMessage());
            e.printStackTrace();
        }
        
        return posts;
    }

    private XiaohongshuPost parsePostFromJson(JsonNode feedNode) {
        JsonNode noteCard = feedNode.path("noteCard");
        if (noteCard.isMissingNode()) {
            return null;
        }
        
        XiaohongshuPost post = new XiaohongshuPost();
        
        post.setNoteId(feedNode.path("id").asText());
        post.setXsecToken(feedNode.path("xsecToken").asText());
        
        JsonNode userNode = noteCard.path("user");
        post.setAuthor(userNode.path("nickname").asText());
        post.setAuthorId(userNode.path("userId").asText());
        
        post.setTitle(noteCard.path("displayTitle").asText());
        
        JsonNode coverNode = noteCard.path("cover");
        post.setCoverImage(coverNode.path("urlDefault").asText());
        
        JsonNode interactNode = noteCard.path("interactInfo");
        String likesStr = interactNode.path("likedCount").asText();
        post.setLikes(parseLikes(likesStr));
        
        post.setUrl("https://www.xiaohongshu.com/explore/" + post.getNoteId() + "?xsec_token=" + post.getXsecToken());
        
        return post;
    }

    private XiaohongshuPost parsePostElement(WebElement element) {
        XiaohongshuPost post = new XiaohongshuPost();
        
        try {
            WebElement linkElement = element.findElement(By.tagName("a"));
            String href = linkElement.getAttribute("href");
            post.setUrl(href);
            
            if (href != null && href.contains("noteId=")) {
                String[] params = href.split("[?&]");
                for (String param : params) {
                    if (param.startsWith("noteId=")) {
                        post.setNoteId(param.substring(7));
                    }
                    if (param.startsWith("xsec_token=")) {
                        post.setXsecToken(param.substring(11));
                    }
                }
            }
            
            try {
                WebElement titleElement = element.findElement(By.cssSelector("[class*='title']"));
                post.setTitle(titleElement.getText());
            } catch (Exception e) {
                post.setTitle("");
            }
            
            try {
                WebElement authorElement = element.findElement(By.cssSelector("[class*='author'], [class*='nickname']"));
                post.setAuthor(authorElement.getText());
            } catch (Exception e) {
                post.setAuthor("");
            }
            
            try {
                WebElement imageElement = element.findElement(By.cssSelector("img"));
                post.setCoverImage(imageElement.getAttribute("src"));
            } catch (Exception e) {
                post.setCoverImage("");
            }
            
            try {
                WebElement likesElement = element.findElement(By.cssSelector("[class*='liked'], [class*='like']"));
                String likesText = likesElement.getText().trim();
                post.setLikes(parseLikes(likesText));
            } catch (Exception e) {
                post.setLikes(0);
            }
            
            return post;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 从DOM中解析帖子详情（标题和内容）
     */
    private void parsePostDetailFromDOM(XiaohongshuPost post) {
        try {
            // 尝试多种可能的选择器来获取标题
            String title = extractTextBySelectors(
                "div.note-content div.title",
                "section.note-item div.title",
                "[class*='title']"
            );
            
            if (title != null && !title.isEmpty()) {
                post.setTitle(title);
                System.out.println("成功提取标题: " + title);
            } else {
                // 如果DOM中没有找到，尝试从meta标签获取
                title = extractMetaContent("og:title");
                if (title != null && !title.isEmpty()) {
                    post.setTitle(title);
                    System.out.println("从meta标签提取标题: " + title);
                }
            }
            
            // 尝试多种可能的选择器来获取内容
            String content = extractTextBySelectors(
                "div.note-content div.desc",
                "section.note-item div.desc",
                "[class*='desc']"
            );
            
            if (content != null && !content.isEmpty()) {
                post.setContent(content);
                System.out.println("成功提取内容，长度: " + content.length());
            } else {
                // 如果DOM中没有找到，尝试从meta标签获取
                content = extractMetaContent("og:description");
                if (content != null && !content.isEmpty()) {
                    post.setContent(content);
                    System.out.println("从meta标签提取内容，长度: " + content.length());
                }
            }
            
        } catch (Exception e) {
            System.err.println("解析帖子详情失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 通过多个CSS选择器尝试提取文本
     */
    private String extractTextBySelectors(String... selectors) {
        for (String selector : selectors) {
            try {
                WebElement element = driver.findElement(By.cssSelector(selector));
                if (element != null) {
                    String text = element.getText().trim();
                    if (text != null && !text.isEmpty()) {
                        return text;
                    }
                }
            } catch (Exception e) {
                // 继续尝试下一个选择器
            }
        }
        return null;
    }
    
    /**
     * 从meta标签中提取内容
     */
    private String extractMetaContent(String metaName) {
        try {
            WebElement metaElement = driver.findElement(
                By.cssSelector("meta[name='" + metaName + "'], meta[property='" + metaName + "']")
            );
            if (metaElement != null) {
                return metaElement.getAttribute("content");
            }
        } catch (Exception e) {
            // 忽略异常
        }
        return null;
    }

    private int parseLikes(String likesText) {
        if (likesText == null || likesText.isEmpty()) {
            return 0;
        }
        try {
            if (likesText.contains("w")) {
                return (int)(Double.parseDouble(likesText.replace("w", "")) * 10000);
            }
            return Integer.parseInt(likesText.replaceAll("[^0-9]", ""));
        } catch (Exception e) {
            return 0;
        }
    }
}
