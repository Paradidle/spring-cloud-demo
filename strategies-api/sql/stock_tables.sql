-- 股票数据库表结构设计
-- 创建时间: 2026-05-02

-- 使用股票数据库
USE `stock_db`;

-- 1. 股票基本信息表
CREATE TABLE IF NOT EXISTS `stock_basic` (
    `id` BIGINT UNSIGNED AUTO_INCREMENT COMMENT '主键ID',
    `stock_code` VARCHAR(10) NOT NULL COMMENT '股票代码',
    `market` VARCHAR(2) NOT NULL COMMENT '市场标识: sh-上海, sz-深圳',
    `stock_name` VARCHAR(50) NOT NULL COMMENT '股票名称',
    `is_index` TINYINT(1) DEFAULT 0 COMMENT '是否为大盘指数',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_stock_code` (`stock_code`),
    KEY `idx_market` (`market`),
    KEY `idx_stock_name` (`stock_name`),
    KEY `idx_is_index` (`is_index`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='股票基本信息表';

-- 2. 股票日线数据表
CREATE TABLE IF NOT EXISTS `stock_daily` (
    `id` BIGINT UNSIGNED AUTO_INCREMENT COMMENT '主键ID',
    `stock_code` VARCHAR(10) NOT NULL COMMENT '股票代码',
    `trade_date` DATE NOT NULL COMMENT '交易日期',
    `open_price` DECIMAL(10, 3) DEFAULT NULL COMMENT '开盘价',
    `high_price` DECIMAL(10, 3) DEFAULT NULL COMMENT '最高价',
    `low_price` DECIMAL(10, 3) DEFAULT NULL COMMENT '最低价',
    `close_price` DECIMAL(10, 3) DEFAULT NULL COMMENT '收盘价',
    `total_market_value` DECIMAL(20, 2) DEFAULT NULL COMMENT '总市值(元)',
    `net_inflow` DECIMAL(20, 2) DEFAULT NULL COMMENT '净流入(元)',
    `net_outflow` DECIMAL(20, 2) DEFAULT NULL COMMENT '净流出(元)',
    `main_net_inflow` DECIMAL(20, 2) DEFAULT NULL COMMENT '主力净流入(元)',
    `main_net_outflow` DECIMAL(20, 2) DEFAULT NULL COMMENT '主力净流出(元)',
    `minute_data` TEXT COMMENT '分时数据JSON数组，存储5分钟K线收盘价',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_stock_date` (`stock_code`, `trade_date`),
    KEY `idx_trade_date` (`trade_date`),
    KEY `idx_stock_code` (`stock_code`),
    KEY `idx_date_range` (`trade_date`, `stock_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='股票日线数据表';

-- 创建视图：获取最新交易日的所有股票数据
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
INNER JOIN stock_daily sd ON sb.stock_code = sd.stock_code
WHERE sd.trade_date = (
    SELECT MAX(trade_date) 
    FROM stock_daily
);

-- 索引说明：
-- 1. stock_basic表：
--    - uk_stock_code: 唯一索引，保证股票代码唯一性，加速根据代码查询
--    - idx_market: 加速按市场筛选（如只查沪市或深市）
--    - idx_stock_name: 加速按股票名称搜索
--
-- 2. stock_daily表：
--    - uk_stock_date: 联合唯一索引，保证同一股票同一天只有一条记录
--    - idx_trade_date: 加速按日期查询（如查询某天的所有股票）
--    - idx_stock_code: 加速按股票代码查询历史数据
--    - idx_date_range: 覆盖索引，优化日期范围查询性能
--
-- 使用示例：
-- 1. 查询某股票的历史数据：
--    SELECT * FROM stock_daily WHERE stock_code = '600519' ORDER BY trade_date DESC;
--
-- 2. 查询某天的所有股票：
--    SELECT * FROM stock_daily WHERE trade_date = '2024-01-02';
--
-- 3. 查询某股票在某日期范围内的数据：
--    SELECT * FROM stock_daily 
--    WHERE stock_code = '600519' 
--    AND trade_date BETWEEN '2024-01-01' AND '2024-12-31'
--    ORDER BY trade_date;
--
-- 4. 查询最新交易日的股票数据：
--    SELECT * FROM v_latest_stock_data;
--
-- 5. 插入股票基本信息：
--    INSERT INTO stock_basic (stock_code, market, stock_name) 
--    VALUES ('600519', 'sh', '贵州茅台')

-- 3. 财经新闻表（财联社加红栏目）
CREATE TABLE IF NOT EXISTS `stock_daily_news` (
    `id` BIGINT UNSIGNED AUTO_INCREMENT COMMENT '主键ID',
    `news_id` VARCHAR(50) DEFAULT NULL COMMENT '新闻ID（来源网站唯一标识，用于排重）',
    `title` VARCHAR(255) NOT NULL COMMENT '新闻标题',
    `summary` TEXT COMMENT '新闻内容摘要',
    `source` VARCHAR(50) DEFAULT NULL COMMENT '新闻来源',
    `news_type` VARCHAR(20) DEFAULT NULL COMMENT '新闻类型（快讯、头条、专题等）',
    `url` VARCHAR(500) DEFAULT NULL COMMENT '新闻URL',
    `publish_time` DATETIME DEFAULT NULL COMMENT '发布时间',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_news_id` (`news_id`),
    KEY `idx_publish_time` (`publish_time`),
    KEY `idx_news_type` (`news_type`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='财经新闻表';

-- 4. 行业概念涨幅表（每日收盘后更新）
CREATE TABLE IF NOT EXISTS `stock_daily_category` (
    `id` BIGINT UNSIGNED AUTO_INCREMENT COMMENT '主键ID',
    `data_date` DATE NOT NULL COMMENT '数据日期',
    `industry_top5` JSON COMMENT '行业涨幅前5数据',
    `concept_top5` JSON COMMENT '概念涨幅前5数据',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_data_date` (`data_date`),
    KEY `idx_data_date` (`data_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='行业概念涨幅表';
--    ON DUPLICATE KEY UPDATE stock_name = VALUES(stock_name);
--
-- 6. 插入股票日线数据：
--    INSERT INTO stock_daily (stock_code, trade_date, open_price, high_price, low_price, close_price, minute_data)
--    VALUES ('600519', '2024-01-02', 1780.00, 1800.00, 1750.00, 1790.00, '[1780.0,1785.0,1790.0]')
--    ON DUPLICATE KEY UPDATE 
--        open_price = VALUES(open_price),
--        high_price = VALUES(high_price),
--        low_price = VALUES(low_price),
--        close_price = VALUES(close_price),
--        minute_data = VALUES(minute_data);
