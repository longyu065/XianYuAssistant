-- 闲鱼账号表
CREATE TABLE IF NOT EXISTS xianyu_account (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    account_note VARCHAR(100),                    -- 闲鱼账号备注
    unb VARCHAR(100),                             -- UNB标识
    status TINYINT DEFAULT 1,                     -- 账号状态 1:正常 -1:需要手机号验证
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,  -- 创建时间
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP   -- 更新时间
);

-- 闲鱼Cookie表
CREATE TABLE IF NOT EXISTS xianyu_cookie (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    xianyu_account_id BIGINT NOT NULL,            -- 关联的闲鱼账号ID
    cookie_text TEXT,                             -- 完整的Cookie字符串
    m_h5_tk VARCHAR(500),                         -- _m_h5_tk token（用于API签名）
    cookie_status TINYINT DEFAULT 1,              -- Cookie状态 1:有效 2:过期 3:失效
    expire_time DATETIME,                         -- 过期时间
    websocket_token TEXT,                         -- WebSocket accessToken
    token_expire_time INTEGER,                    -- Token过期时间戳（毫秒）
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,  -- 创建时间
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP,  -- 更新时间
    FOREIGN KEY (xianyu_account_id) REFERENCES xianyu_account(id)
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_account_unb ON xianyu_account(unb);
CREATE INDEX IF NOT EXISTS idx_cookie_account_id ON xianyu_cookie(xianyu_account_id);
CREATE INDEX IF NOT EXISTS idx_cookie_status ON xianyu_cookie(cookie_status);
CREATE INDEX IF NOT EXISTS idx_token_expire_time ON xianyu_cookie(token_expire_time);

-- 创建更新时间触发器（SQLite不支持ON UPDATE CURRENT_TIMESTAMP，需要用触发器）
-- 注意：触发器使用特殊分隔符 $$
CREATE TRIGGER IF NOT EXISTS update_xianyu_account_time 
AFTER UPDATE ON xianyu_account
BEGIN
    UPDATE xianyu_account SET updated_time = CURRENT_TIMESTAMP WHERE id = NEW.id;
END
$$

CREATE TRIGGER IF NOT EXISTS update_xianyu_cookie_time 
AFTER UPDATE ON xianyu_cookie
BEGIN
    UPDATE xianyu_cookie SET updated_time = CURRENT_TIMESTAMP WHERE id = NEW.id;
END
$$

-- 闲鱼商品信息表
CREATE TABLE IF NOT EXISTS xianyu_goods_info (
    id BIGINT PRIMARY KEY,                        -- 表ID（使用雪花ID）
    xy_good_id VARCHAR(100) NOT NULL,             -- 闲鱼商品ID
    xianyu_account_id BIGINT,                     -- 关联的闲鱼账号ID
    title VARCHAR(500),                           -- 商品标题
    cover_pic TEXT,                               -- 封面图片URL
    info_pic TEXT,                                -- 商品详情图片（JSON数组）
    detail_info TEXT,                             -- 商品详情信息（预留字段）
    detail_url TEXT,                              -- 商品详情页URL
    sold_price VARCHAR(50),                       -- 商品价格
    status TINYINT DEFAULT 0,                     -- 商品状态 0:在售 1:已下架 2:已售出
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,  -- 创建时间
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP,  -- 更新时间
    FOREIGN KEY (xianyu_account_id) REFERENCES xianyu_account(id)
);

-- 创建商品表索引
CREATE UNIQUE INDEX IF NOT EXISTS idx_goods_xy_good_id ON xianyu_goods_info(xy_good_id);
CREATE INDEX IF NOT EXISTS idx_goods_status ON xianyu_goods_info(status);
CREATE INDEX IF NOT EXISTS idx_goods_account_id ON xianyu_goods_info(xianyu_account_id);

-- 创建商品表更新时间触发器
CREATE TRIGGER IF NOT EXISTS update_xianyu_goods_info_time 
AFTER UPDATE ON xianyu_goods_info
BEGIN
    UPDATE xianyu_goods_info SET updated_time = CURRENT_TIMESTAMP WHERE id = NEW.id;
END
$

-- 闲鱼聊天消息表
CREATE TABLE IF NOT EXISTS xianyu_chat_message (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    xianyu_account_id BIGINT NOT NULL,            -- 关联的闲鱼账号ID
    session_id VARCHAR(100),                      -- 会话ID（对话ID）
    message_id VARCHAR(100) NOT NULL,             -- 消息ID（闲鱼的消息ID）
    message_type INTEGER DEFAULT 1,               -- 消息类型：1=文本 2=图片 3=语音 4=商品卡片 5=系统消息
    direction TINYINT NOT NULL,                   -- 消息方向：1=发送 2=接收
    sender_user_id VARCHAR(100),                  -- 发送者用户ID
    sender_nickname VARCHAR(200),                 -- 发送者昵称
    receiver_user_id VARCHAR(100),                -- 接收者用户ID
    receiver_nickname VARCHAR(200),               -- 接收者昵称
    content_type INTEGER DEFAULT 1,               -- 内容类型：1=文本 2=图片 3=语音 101=文本消息 26 系统消息
    content_text TEXT,                            -- 文本内容
    content_json TEXT,                            -- 完整的消息内容JSON
    item_id VARCHAR(100),                         -- 商品ID
    item_title VARCHAR(500),                      -- 商品标题
    is_read TINYINT DEFAULT 0,                    -- 是否已读：0=未读 1=已读
    read_time DATETIME,                           -- 阅读时间
    raw_data TEXT,                                -- 原始解密后的完整JSON数据
    extra_info TEXT,                              -- 额外信息JSON
    message_time BIGINT,                          -- 消息时间戳（毫秒）
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,  -- 记录创建时间
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP,  -- 记录更新时间
    FOREIGN KEY (xianyu_account_id) REFERENCES xianyu_account(id)
);

-- 创建聊天消息表索引
CREATE INDEX IF NOT EXISTS idx_chat_message_account_id ON xianyu_chat_message(xianyu_account_id);
CREATE INDEX IF NOT EXISTS idx_chat_message_session_id ON xianyu_chat_message(session_id);
CREATE INDEX IF NOT EXISTS idx_chat_message_message_id ON xianyu_chat_message(message_id);
CREATE INDEX IF NOT EXISTS idx_chat_message_direction ON xianyu_chat_message(direction);
CREATE INDEX IF NOT EXISTS idx_chat_message_time ON xianyu_chat_message(message_time);
CREATE INDEX IF NOT EXISTS idx_chat_message_sender ON xianyu_chat_message(sender_user_id);
CREATE INDEX IF NOT EXISTS idx_chat_message_receiver ON xianyu_chat_message(receiver_user_id);
CREATE INDEX IF NOT EXISTS idx_chat_message_item_id ON xianyu_chat_message(item_id);
CREATE INDEX IF NOT EXISTS idx_chat_message_is_read ON xianyu_chat_message(is_read);
CREATE UNIQUE INDEX IF NOT EXISTS idx_chat_message_unique ON xianyu_chat_message(xianyu_account_id, message_id);

-- 创建聊天消息表更新时间触发器
CREATE TRIGGER IF NOT EXISTS update_xianyu_chat_message_time 
AFTER UPDATE ON xianyu_chat_message
BEGIN
    UPDATE xianyu_chat_message SET updated_time = CURRENT_TIMESTAMP WHERE id = NEW.id;
END
$
