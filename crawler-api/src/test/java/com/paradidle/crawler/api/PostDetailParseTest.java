package com.paradidle.crawler.api;

import java.io.File;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import com.paradidle.crawler.api.model.XiaohongshuPost;

/**
 * 测试小红书帖子详情页的HTML解析
 */
public class PostDetailParseTest {

    public static void main(String[] args) throws Exception {
        String htmlPath = "D:\\gitproject\\spring-cloud-demo\\crawler-api\\src\\main\\resources\\debughtml\\detail.html";
        
        File htmlFile = new File(htmlPath);
        if (!htmlFile.exists()) {
            System.err.println("HTML文件不存在: " + htmlPath);
            return;
        }
        
        System.out.println("开始解析帖子详情HTML...\n");
        
        Document doc = Jsoup.parse(htmlFile, "UTF-8", "");
        
        XiaohongshuPost post = new XiaohongshuPost();
        
        // 方法1: 尝试从DOM中提取标题和内容
        parseFromDOM(doc, post);
        
        // 方法2: 如果DOM中没有，从meta标签提取
        if (post.getTitle() == null || post.getTitle().isEmpty()) {
            parseFromMetaTags(doc, post);
        }
        
        // 输出结果
        System.out.println("========== 解析结果 ==========");
        System.out.println("标题: " + (post.getTitle() != null ? post.getTitle() : "未找到"));
        System.out.println("\n内容: " + (post.getContent() != null ? post.getContent() : "未找到"));
        System.out.println("\nURL: " + (post.getUrl() != null ? post.getUrl() : "未找到"));
        System.out.println("==============================\n");
    }
    
    /**
     * 从DOM结构中解析帖子详情
     */
    private static void parseFromDOM(Document doc, XiaohongshuPost post) {
        System.out.println("尝试从DOM结构解析...");
        
        // 尝试多种选择器获取标题
        String[] titleSelectors = {
            "div.note-content div.title",
            "section.note-item div.title",
            "[class*='title']"
        };
        
        for (String selector : titleSelectors) {
            Element titleElement = doc.selectFirst(selector);
            if (titleElement != null) {
                String title = titleElement.text().trim();
                if (!title.isEmpty()) {
                    post.setTitle(title);
                    System.out.println("✓ 通过选择器 '" + selector + "' 找到标题");
                    break;
                }
            }
        }
        
        // 尝试多种选择器获取内容
        String[] contentSelectors = {
            "div.note-content div.desc",
            "section.note-item div.desc",
            "[class*='desc']"
        };
        
        for (String selector : contentSelectors) {
            Element contentElement = doc.selectFirst(selector);
            if (contentElement != null) {
                String content = contentElement.text().trim();
                if (!content.isEmpty()) {
                    post.setContent(content);
                    System.out.println("✓ 通过选择器 '" + selector + "' 找到内容 (长度: " + content.length() + ")");
                    break;
                }
            }
        }
    }
    
    /**
     * 从meta标签中解析帖子信息（备用方案）
     */
    private static void parseFromMetaTags(Document doc, XiaohongshuPost post) {
        System.out.println("\n尝试从meta标签解析...");
        
        // 从og:title获取标题
        Element ogTitle = doc.selectFirst("meta[property='og:title']");
        if (ogTitle != null) {
            String title = ogTitle.attr("content");
            if (title != null && !title.isEmpty()) {
                post.setTitle(title);
                System.out.println("✓ 从 og:title 找到标题: " + title);
            }
        }
        
        // 从og:description获取内容
        Element ogDesc = doc.selectFirst("meta[property='og:description']");
        if (ogDesc != null) {
            String desc = ogDesc.attr("content");
            if (desc != null && !desc.isEmpty()) {
                post.setContent(desc);
                System.out.println("✓ 从 og:description 找到内容 (长度: " + desc.length() + ")");
            }
        }
        
        // 从og:url获取URL
        Element ogUrl = doc.selectFirst("meta[property='og:url']");
        if (ogUrl != null) {
            String url = ogUrl.attr("content");
            if (url != null && !url.isEmpty()) {
                post.setUrl(url);
                System.out.println("✓ 从 og:url 找到URL");
            }
        }
    }
}
