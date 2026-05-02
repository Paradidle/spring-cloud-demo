-- 创建股票数据库
-- 创建时间: 2026-05-02

-- 如果数据库已存在则删除（可选，谨慎使用）
-- DROP DATABASE IF EXISTS stock_db;

-- 创建数据库
CREATE DATABASE IF NOT EXISTS `stock_db` 
CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

-- 使用数据库
USE `stock_db`;

-- 显示创建成功信息
SELECT '数据库 stock_db 创建成功！' AS message;
