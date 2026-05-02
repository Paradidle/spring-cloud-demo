package com.paradidle.crawler.api.model;

import lombok.Data;

@Data
public class XiaohongshuPost {
    private String noteId;
    private String title;
    private String content;  // 帖子正文内容
    private String author;
    private String authorId;
    private String coverImage;
    private Integer likes;
    private String url;
    private String xsecToken;
}
