package com.paradidle.crawler.api.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.paradidle.crawler.api.model.XiaohongshuPost;
import com.paradidle.crawler.api.service.BrowserSessionManager;
import com.paradidle.crawler.api.service.FacebookCrawlerService;
import com.paradidle.crawler.api.service.XiaohongshuCrawlerService;

@RestController
@RequestMapping("/api/crawler")
public class CrawlerController {

    @Autowired
    private XiaohongshuCrawlerService xiaohongshuCrawlerService;

    @Autowired
    private FacebookCrawlerService facebookCrawlerService;

    @Autowired
    private BrowserSessionManager sessionManager;

    @PostMapping("/xiaohongshu/login")
    public ResponseEntity<Map<String, Object>> xiaohongshuLogin() {
        Map<String, Object> result = new HashMap<>();
        try {
            String sessionId = xiaohongshuCrawlerService.loginWithQrCode(sessionManager);
            
            result.put("success", true);
            result.put("message", "二维码已生成，请扫码登录");
            result.put("sessionId", sessionId);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "登录失败: " + e.getMessage());
            return ResponseEntity.status(500).body(result);
        }
    }

    @DeleteMapping("/session/{sessionId}")
    public ResponseEntity<Map<String, Object>> closeSession(@PathVariable String sessionId) {
        Map<String, Object> result = new HashMap<>();
        try {
            sessionManager.closeSession(sessionId);
            
            result.put("success", true);
            result.put("message", "会话已关闭");
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "关闭会话失败: " + e.getMessage());
            return ResponseEntity.status(500).body(result);
        }
    }

    @GetMapping("/sessions")
    public ResponseEntity<Map<String, Object>> getActiveSessions() {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("count", sessionManager.getActiveSessionCount());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/facebook/login")
    public ResponseEntity<Map<String, Object>> facebookLogin() {
        Map<String, Object> result = new HashMap<>();
        try {
            String sessionId = facebookCrawlerService.loginWithQrCode(sessionManager);
            
            result.put("success", true);
            result.put("message", "二维码已生成，请扫码登录");
            result.put("sessionId", sessionId);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "登录失败: " + e.getMessage());
            return ResponseEntity.status(500).body(result);
        }
    }

    @GetMapping("/xiaohongshu/search")
    public ResponseEntity<Map<String, Object>> searchXiaohongshuPosts(
            @RequestParam String keyword,
            @RequestParam String sessionId) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<XiaohongshuPost> posts = xiaohongshuCrawlerService.searchPosts(keyword, sessionManager, sessionId);
            
            result.put("success", true);
            result.put("count", posts.size());
            result.put("posts", posts);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "搜索失败: " + e.getMessage());
            return ResponseEntity.status(500).body(result);
        }
    }

    @GetMapping("/xiaohongshu/post/{noteId}")
    public ResponseEntity<Map<String, Object>> getPostDetail(
            @PathVariable String noteId,
            @RequestParam String xsecToken,
            @RequestParam String sessionId) {
        Map<String, Object> result = new HashMap<>();
        try {
            XiaohongshuPost post = xiaohongshuCrawlerService.getPostDetail(noteId, xsecToken, sessionManager, sessionId);
            
            result.put("success", true);
            result.put("post", post);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "获取帖子详情失败: " + e.getMessage());
            return ResponseEntity.status(500).body(result);
        }
    }
}
