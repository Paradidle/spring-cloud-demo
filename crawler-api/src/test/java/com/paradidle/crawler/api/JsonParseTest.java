package com.paradidle.crawler.api;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paradidle.crawler.api.model.XiaohongshuPost;

public class JsonParseTest {

    public static void main(String[] args) throws Exception {
        String htmlPath = "D:\\gitproject\\spring-cloud-demo\\crawler-api\\src\\main\\resources\\debughtml\\9bde44e1-a5dd-435e-9f70-dce9f3487ad1_search_result.html";
        
        File htmlFile = new File(htmlPath);
        if (!htmlFile.exists()) {
            System.err.println("HTML文件不存在: " + htmlPath);
            return;
        }
        
        String pageSource = new String(Files.readAllBytes(htmlFile.toPath()), "UTF-8");
        
        int startIndex = pageSource.indexOf("window.__INITIAL_STATE__=");
        if (startIndex == -1) {
            System.err.println("未找到页面数据");
            return;
        }
        
        int endIndex = pageSource.indexOf("</script>", startIndex);
        if (endIndex == -1) {
            System.err.println("未找到脚本结束标记");
            return;
        }
        
        String jsonStr = pageSource.substring(startIndex + 25, endIndex);
        System.out.println("JSON字符串长度: " + jsonStr.length());
        
        // 替换 JavaScript undefined 为 JSON null
        jsonStr = jsonStr.replaceAll(":\\s*undefined([,}\\]])", ":null$1");
        
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(jsonStr);
        JsonNode searchNode = root.path("search").path("feeds");
        
        System.out.println("搜索到的帖子数量: " + (searchNode.isArray() ? searchNode.size() : 0));
        
        if (searchNode.isArray()) {
            List<XiaohongshuPost> posts = new ArrayList<>();
            
            for (int i = 0; i < Math.min(5, searchNode.size()); i++) {
                JsonNode feedNode = searchNode.get(i);
                XiaohongshuPost post = parsePostFromJson(feedNode);
                
                if (post != null) {
                    posts.add(post);
                    System.out.println("\n=== 帖子 " + (i + 1) + " ===");
                    System.out.println("ID: " + post.getNoteId());
                    System.out.println("标题: " + post.getTitle());
                    System.out.println("作者: " + post.getAuthor());
                    System.out.println("点赞: " + post.getLikes());
                    System.out.println("xsecToken: " + post.getXsecToken());
                    System.out.println("URL: " + post.getUrl());
                }
            }
            
            System.out.println("\n\n总共解析到 " + posts.size() + " 个帖子");
        }
    }
    
    private static XiaohongshuPost parsePostFromJson(JsonNode feedNode) {
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
    
    private static int parseLikes(String likesText) {
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
