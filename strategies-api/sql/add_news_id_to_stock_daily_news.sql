-- 为 stock_daily_news 表添加 news_id 字段
-- 执行时间：2026-05-09

-- 添加 news_id 字段
ALTER TABLE `stock_daily_news` 
ADD COLUMN `news_id` VARCHAR(50) DEFAULT NULL COMMENT '新闻ID（来源网站唯一标识，用于排重）' AFTER `id`;

-- 添加唯一索引
ALTER TABLE `stock_daily_news` 
ADD UNIQUE KEY `uk_news_id` (`news_id`);

-- 说明：
-- 1. news_id 字段用于存储财联社新闻的唯一ID
-- 2. 使用唯一索引确保不会重复插入相同ID的新闻
-- 3. 相比标题排重，ID排重更准确可靠
