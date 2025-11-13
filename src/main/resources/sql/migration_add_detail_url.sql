-- 为 xianyu_goods_info 表添加 detail_url 字段
-- 执行时间：2024-11-12

-- 检查字段是否存在，如果不存在则添加
-- SQLite 不支持 IF NOT EXISTS 语法，所以需要手动检查

-- 添加 detail_url 字段
ALTER TABLE xianyu_goods_info ADD COLUMN detail_url TEXT;

-- 说明：
-- 1. 此字段用于存储商品详情页的URL
-- 2. 在刷新商品列表时会自动保存
-- 3. 可用于直接访问商品详情页
