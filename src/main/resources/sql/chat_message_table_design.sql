-- 闲鱼聊天消息表设计
-- 用于保存WebSocket接收到的所有聊天消息

-- 聊天消息表
CREATE TABLE IF NOT EXISTS xianyu_chat_message (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    
    -- 关联信息
    xianyu_account_id BIGINT NOT NULL,            -- 关联的闲鱼账号ID
    
    -- WebSocket消息字段
    lwp VARCHAR(50),                              -- websocket消息类型，比如："/s/para"
    pnm_id VARCHAR(100) NOT NULL,                 -- 对应的消息pnmid，比如："3813496236127.PNM"（字段1.3）
    s_id VARCHAR(100),                            -- 消息聊天框id，比如："55435931514@goofish"（字段1.2）
    
    -- 消息内容
    content_type INTEGER,                         -- 消息类别，contentType=1用户消息，32系统消息（字段1.6.3.5中的contentType）
    msg_content TEXT,                             -- 消息内容，对应1.10.reminderContent
    
    -- 发送者信息
    sender_user_name VARCHAR(200),                -- 发送者用户名称，对应1.10.reminderTitle
    sender_user_id VARCHAR(100),                  -- 发送者用户id，对应1.10.senderUserId
    sender_app_v VARCHAR(50),                     -- 发送者app版本，对应1.10._appVersion
    sender_os_type VARCHAR(20),                   -- 发送者系统版本，对应1.10._platform
    
    -- 消息链接
    reminder_url TEXT,                            -- 消息链接，对应1.10.reminderUrl
    
    -- 完整消息体
    complete_msg TEXT NOT NULL,                   -- 完整的消息体JSON
    
    -- 时间信息
    message_time BIGINT,                          -- 消息时间戳（毫秒，字段1.5）
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,  -- 创建时间
    
    -- 外键约束
    FOREIGN KEY (xianyu_account_id) REFERENCES xianyu_account(id)
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_chat_message_account_id ON xianyu_chat_message(xianyu_account_id);
CREATE INDEX IF NOT EXISTS idx_chat_message_pnm_id ON xianyu_chat_message(pnm_id);
CREATE INDEX IF NOT EXISTS idx_chat_message_s_id ON xianyu_chat_message(s_id);
CREATE INDEX IF NOT EXISTS idx_chat_message_sender_user_id ON xianyu_chat_message(sender_user_id);
CREATE INDEX IF NOT EXISTS idx_chat_message_content_type ON xianyu_chat_message(content_type);
CREATE INDEX IF NOT EXISTS idx_chat_message_time ON xianyu_chat_message(message_time);

-- 创建唯一索引，防止重复消息
CREATE UNIQUE INDEX IF NOT EXISTS idx_chat_message_unique 
ON xianyu_chat_message(xianyu_account_id, pnm_id);

-- 表说明
-- 1. pnm_id: 闲鱼消息的唯一ID（字段1.3），确保唯一性
-- 2. s_id: 聊天框ID（字段1.2），用于区分不同的对话
-- 3. content_type: 消息类别，1=用户消息，32=系统消息
-- 4. msg_content: 消息文本内容，方便查询
-- 5. complete_msg: 保存完整的消息JSON，方便后续分析
-- 6. message_time: 闲鱼的时间戳（毫秒）
-- 7. sender_user_id: 发送者ID，方便查询对话

-- 字段映射说明
-- lwp: websocket消息路径（如果有的话）
-- pnm_id: 字段1.3
-- s_id: 字段1.2
-- content_type: 字段1.6.3.5中的contentType
-- msg_content: 字段1.10.reminderContent
-- sender_user_name: 字段1.10.reminderTitle
-- sender_user_id: 字段1.10.senderUserId
-- reminder_url: 字段1.10.reminderUrl
-- sender_app_v: 字段1.10._appVersion
-- sender_os_type: 字段1.10._platform
-- message_time: 字段1.5

-- 查询示例
-- 1. 查询某个账号的所有消息
-- SELECT * FROM xianyu_chat_message WHERE xianyu_account_id = 1 ORDER BY message_time DESC;

-- 2. 查询某个会话的消息
-- SELECT * FROM xianyu_chat_message WHERE s_id = '55435931514@goofish' ORDER BY message_time ASC;

-- 3. 查询用户消息（排除系统消息）
-- SELECT * FROM xianyu_chat_message WHERE xianyu_account_id = 1 AND content_type = 1;

-- 4. 查询系统消息
-- SELECT * FROM xianyu_chat_message WHERE xianyu_account_id = 1 AND content_type = 32;

-- 5. 查询与某个用户的对话
-- SELECT * FROM xianyu_chat_message WHERE sender_user_id = '3553532632' ORDER BY message_time ASC;

-- 6. 统计每天的消息数量
-- SELECT DATE(datetime(message_time/1000, 'unixepoch', 'localtime')) as date, 
--        COUNT(*) as count
-- FROM xianyu_chat_message 
-- WHERE xianyu_account_id = 1
-- GROUP BY date
-- ORDER BY date DESC;
