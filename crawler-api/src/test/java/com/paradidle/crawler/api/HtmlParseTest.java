package com.paradidle.crawler.api;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import com.paradidle.crawler.api.model.XiaohongshuPost;

public class HtmlParseTest {

    public static void main(String[] args) throws Exception {
        String htmlPath = "D:\\gitproject\\spring-cloud-demo\\crawler-api\\src\\main\\resources\\debughtml\\9bde44e1-a5dd-435e-9f70-dce9f3487ad1_search_result.html";
        
        File htmlFile = new File(htmlPath);
        if (!htmlFile.exists()) {
            System.err.println("HTML文件不存在");
            return;
        }
        
        Document doc = Jsoup.parse(htmlFile, "UTF-8", "");
        
        Elements noteItems = doc.select("section.note-item");
        System.out.println("找到 " + noteItems.size() + " 个帖子\n");
        
        List<XiaohongshuPost> posts = new ArrayList<>();
        
        for (int i = 0; i < noteItems.size(); i++) {
            Element noteItem = noteItems.get(i);
            XiaohongshuPost post = parseNoteItem(noteItem);
            if (post != null && post.getTitle() != null && !post.getTitle().isEmpty()) {
                posts.add(post);
                System.out.println((i + 1) + ". " + post.getTitle());
                System.out.println("   作者: " + post.getAuthor());
                System.out.println("   点赞: " + post.getLikes());
                System.out.println("   ID: " + post.getNoteId());
                System.out.println("   xsecToken: " + post.getXsecToken());
                System.out.println("   链接: " + post.getUrl());
                System.out.println();
            }
        }
        
        System.out.println("\n成功解析 " + posts.size() + " 个帖子");
    }
    
    private static XiaohongshuPost parseNoteItem(Element noteItem) {
        XiaohongshuPost post = new XiaohongshuPost();
        
        Element titleLink = noteItem.selectFirst("a.title");
        if (titleLink != null) {
            String href = titleLink.attr("href");
            post.setUrl("https://www.xiaohongshu.com" + href);
            
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
            
            Element titleSpan = titleLink.selectFirst("span");
            if (titleSpan != null) {
                post.setTitle(titleSpan.text());
            }
        }
        
        Element authorName = noteItem.selectFirst("div.name");
        if (authorName != null) {
            post.setAuthor(authorName.text());
        }
        
        Element img = noteItem.selectFirst("img");
        if (img != null) {
            post.setCoverImage(img.attr("src"));
        }
        
        Element likesElement = noteItem.selectFirst("[class*='liked'], [class*='like-count'], span[class*='count']");
        if (likesElement != null) {
            String likesText = likesElement.text().trim();
            post.setLikes(parseLikes(likesText));
        }
        
        return post;
    }
    
    private static int parseLikes(String likesText) {
        if (likesText == null || likesText.isEmpty()) {
            return 0;
        }
        try {
            if (likesText.contains("w") || likesText.contains("万")) {
                String numStr = likesText.replaceAll("[^0-9.]", "");
                return (int)(Double.parseDouble(numStr) * 10000);
            }
            return Integer.parseInt(likesText.replaceAll("[^0-9]", ""));
        } catch (Exception e) {
            return 0;
        }
    }
}
