-- 为 xianyu_goods 表添加 xianyu_account_id 字段
-- 执行时间：2024-11-12

-- 添加 xianyu_account_id 字段
ALTER TABLE xianyu_goods ADD COLUMN xianyu_account_id BIGINT;

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_goods_account_id ON xianyu_goods(xianyu_account_id);

-- 说明：
-- 1. 此字段用于关联闲鱼账号
-- 2. 在刷新商品列表时会自动填充
-- 3. 可用于按账号查询商品
