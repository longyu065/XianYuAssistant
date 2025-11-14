-- 添加 xy_goods_id 字段
ALTER TABLE xianyu_chat_message ADD COLUMN xy_goods_id VARCHAR(100);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_chat_message_goods_id ON xianyu_chat_message(xy_goods_id);
