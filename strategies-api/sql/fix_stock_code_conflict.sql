-- 修复股票代码冲突问题：为stock_daily表添加market字段
-- 执行时间: 2026-05-11

USE `stock_db`;

-- 1. 为stock_daily表添加market字段
ALTER TABLE `stock_daily` 
ADD COLUMN `market` VARCHAR(2) NOT NULL DEFAULT 'sh' COMMENT '市场标识: sh-上海, sz-深圳' AFTER `stock_code`;

-- 2. 删除原有的唯一索引
ALTER TABLE `stock_daily` DROP INDEX `uk_stock_date`;

-- 3. 创建新的唯一索引，包含market字段
ALTER TABLE `stock_daily` 
ADD UNIQUE KEY `uk_market_stock_date` (`market`, `stock_code`, `trade_date`);

-- 4. 为market字段添加索引以提高查询性能
ALTER TABLE `stock_daily` 
ADD KEY `idx_market` (`market`);

-- 5. 更新现有数据的market字段（根据stock_code推断）
UPDATE `stock_daily` 
SET `market` = CASE 
    WHEN `stock_code` REGEXP '^(6|9)' THEN 'sh'
    WHEN `stock_code` REGEXP '^(0|1|2|3)' THEN 'sz'
    ELSE 'sh'
END;

-- 6. 同样更新stock_basic表的market字段（如果还没有正确设置）
UPDATE `stock_basic` 
SET `market` = CASE 
    WHEN `stock_code` REGEXP '^(6|9)' THEN 'sh'
    WHEN `stock_code` REGEXP '^(0|1|2|3)' THEN 'sz'
    ELSE 'sh'
END
WHERE `market` IS NULL OR `market` = '';

-- 7. 更新视图以包含market信息
DROP VIEW IF EXISTS `v_latest_stock_data`;
CREATE OR REPLACE VIEW `v_latest_stock_data` AS
SELECT 
    sb.stock_code,
    sb.market,
    sb.stock_name,
    sd.trade_date,
    sd.open_price,
    sd.high_price,
    sd.low_price,
    sd.close_price,
    sd.total_market_value,
    sd.net_inflow,
    sd.net_outflow,
    sd.main_net_inflow,
    sd.main_net_outflow
FROM stock_basic sb
INNER JOIN stock_daily sd ON sb.stock_code = sd.stock_code AND sb.market = sd.market
WHERE sd.trade_date = (
    SELECT MAX(trade_date) 
    FROM stock_daily
);

-- 8. 验证数据完整性
SELECT 
    market,
    COUNT(*) as record_count,
    COUNT(DISTINCT stock_code) as unique_stocks
FROM stock_daily 
GROUP BY market;

SELECT 
    market,
    COUNT(*) as record_count,
    COUNT(DISTINCT stock_code) as unique_stocks
FROM stock_basic 
GROUP BY market;