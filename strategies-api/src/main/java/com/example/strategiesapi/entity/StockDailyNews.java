package com.example.strategiesapi.entity;

import java.time.LocalDateTime;
import lombok.Data;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 财经新闻实体类
 * 记录财联社网站加红栏目的信息
 *
 * @author system
 */
@Data
@TableName("stock_daily_news")
public class StockDailyNews {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 新闻ID（来源网站的唯一标识，用于排重）
     */
    private String newsId;

    /**
     * 新闻标题
     */
    private String title;

    /**
     * 新闻内容摘要
     */
    private String summary;

    /**
     * 新闻来源
     */
    private String source;

    /**
     * 新闻类型（快讯、头条、专题等）
     */
    private String newsType;

    /**
     * 新闻URL
     */
    private String url;

    /**
     * 发布时间
     */
    private LocalDateTime publishTime;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
