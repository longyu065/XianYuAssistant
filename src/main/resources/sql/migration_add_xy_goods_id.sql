-- 为xianyu_chat_message表添加xy_goods_id字段
-- 该字段用于存储从reminder_url中解析出的商品ID

-- 添加xy_goods_id字段
ALTER TABLE xianyu_chat_message ADD COLUMN xy_goods_id VARCHAR(100);

-- 创建索引以提高查询性能
CREATE INDEX IF NOT EXISTS idx_chat_message_goods_id ON xianyu_chat_message(xy_goods_id);

-- 更新现有数据，从reminder_url中提取itemId
-- 注意：SQLite的字符串处理能力有限，建议通过应用程序代码更新现有数据
-- 如果需要批量更新，可以使用以下SQL（需要SQLite 3.8.3+支持instr和substr函数）
UPDATE xianyu_chat_message 
SET xy_goods_id = CASE 
    WHEN reminder_url LIKE '%itemId=%' THEN
        CASE 
            WHEN instr(substr(reminder_url, instr(reminder_url, 'itemId=') + 7), '&') > 0 THEN
                substr(
                    reminder_url, 
                    instr(reminder_url, 'itemId=') + 7,
                    instr(substr(reminder_url, instr(reminder_url, 'itemId=') + 7), '&') - 1
                )
            ELSE
                substr(reminder_url, instr(reminder_url, 'itemId=') + 7)
        END
    ELSE NULL
END
WHERE reminder_url IS NOT NULL AND reminder_url LIKE '%itemId=%';
