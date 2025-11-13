-- 自动发货功能数据库迁移脚本
-- 创建日期: 2025-11-13

-- 商品配置表
CREATE TABLE IF NOT EXISTS xianyu_goods_config (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    xianyu_account_id BIGINT NOT NULL,                -- 闲鱼账号ID
    xianyu_goods_id BIGINT,                           -- 本地闲鱼商品ID
    xy_goods_id VARCHAR(100) NOT NULL,                -- 闲鱼的商品ID
    xianyu_auto_delivery_on TINYINT DEFAULT 0,        -- 自动发货开关：1-开启，0-关闭，默认关闭
    xianyu_auto_reply_on TINYINT DEFAULT 0,           -- 自动回复开关：1-开启，0-关闭，默认关闭
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,   -- 创建时间
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP,   -- 更新时间
    FOREIGN KEY (xianyu_account_id) REFERENCES xianyu_account(id)
);

-- 创建商品配置表索引
CREATE INDEX IF NOT EXISTS idx_goods_config_account_id ON xianyu_goods_config(xianyu_account_id);
CREATE INDEX IF NOT EXISTS idx_goods_config_xy_goods_id ON xianyu_goods_config(xy_goods_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_goods_config_unique ON xianyu_goods_config(xianyu_account_id, xy_goods_id);

-- 创建商品配置表更新时间触发器
CREATE TRIGGER IF NOT EXISTS update_xianyu_goods_config_time
AFTER UPDATE ON xianyu_goods_config
BEGIN
    UPDATE xianyu_goods_config SET update_time = CURRENT_TIMESTAMP WHERE id = NEW.id;
END;

-- 商品自动发货配置表
CREATE TABLE IF NOT EXISTS xianyu_goods_auto_delivery_config (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    xianyu_account_id BIGINT NOT NULL,                -- 闲鱼账号ID
    xianyu_goods_id BIGINT,                           -- 本地闲鱼商品ID
    xy_goods_id VARCHAR(100) NOT NULL,                -- 闲鱼的商品ID
    type TINYINT DEFAULT 1,                           -- 发货类型（1-文本，2-自定义）
    auto_delivery_content TEXT,                       -- 自动发货的文本内容
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,   -- 创建时间
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP,   -- 更新时间
    FOREIGN KEY (xianyu_account_id) REFERENCES xianyu_account(id)
);

-- 创建自动发货配置表索引
CREATE INDEX IF NOT EXISTS idx_auto_delivery_config_account_id ON xianyu_goods_auto_delivery_config(xianyu_account_id);
CREATE INDEX IF NOT EXISTS idx_auto_delivery_config_xy_goods_id ON xianyu_goods_auto_delivery_config(xy_goods_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_auto_delivery_config_unique ON xianyu_goods_auto_delivery_config(xianyu_account_id, xy_goods_id);

-- 创建自动发货配置表更新时间触发器
CREATE TRIGGER IF NOT EXISTS update_xianyu_goods_auto_delivery_config_time
AFTER UPDATE ON xianyu_goods_auto_delivery_config
BEGIN
    UPDATE xianyu_goods_auto_delivery_config SET update_time = CURRENT_TIMESTAMP WHERE id = NEW.id;
END;

-- 商品自动发货记录表
CREATE TABLE IF NOT EXISTS xianyu_goods_auto_delivery_record (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    xianyu_account_id BIGINT NOT NULL,                -- 闲鱼账号ID
    xianyu_goods_id BIGINT,                           -- 本地闲鱼商品ID
    xy_goods_id VARCHAR(100) NOT NULL,                -- 闲鱼的商品ID
    content TEXT,                                     -- 发货消息内容
    state TINYINT DEFAULT 0,                          -- 状态是否成功1-成功，0-失败
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,   -- 创建时间
    FOREIGN KEY (xianyu_account_id) REFERENCES xianyu_account(id)
);

-- 创建自动发货记录表索引
CREATE INDEX IF NOT EXISTS idx_auto_delivery_record_account_id ON xianyu_goods_auto_delivery_record(xianyu_account_id);
CREATE INDEX IF NOT EXISTS idx_auto_delivery_record_xy_goods_id ON xianyu_goods_auto_delivery_record(xy_goods_id);
CREATE INDEX IF NOT EXISTS idx_auto_delivery_record_state ON xianyu_goods_auto_delivery_record(state);
CREATE INDEX IF NOT EXISTS idx_auto_delivery_record_create_time ON xianyu_goods_auto_delivery_record(create_time);

-- 商品自动回复配置表
CREATE TABLE IF NOT EXISTS xianyu_goods_auto_reply_config (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    xianyu_account_id BIGINT NOT NULL,                -- 闲鱼账号ID
    xianyu_goods_id BIGINT,                           -- 本地闲鱼商品ID
    xy_goods_id VARCHAR(100) NOT NULL,                -- 闲鱼的商品ID
    keyword TEXT,                                     -- 关键词（支持多个，用逗号分隔）
    reply_content TEXT,                               -- 回复内容
    match_type TINYINT DEFAULT 1,                     -- 匹配类型（1-包含，2-完全匹配，3-正则）
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,   -- 创建时间
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP,   -- 更新时间
    FOREIGN KEY (xianyu_account_id) REFERENCES xianyu_account(id)
);

-- 创建自动回复配置表索引
CREATE INDEX IF NOT EXISTS idx_auto_reply_config_account_id ON xianyu_goods_auto_reply_config(xianyu_account_id);
CREATE INDEX IF NOT EXISTS idx_auto_reply_config_xy_goods_id ON xianyu_goods_auto_reply_config(xy_goods_id);

-- 创建自动回复配置表更新时间触发器
CREATE TRIGGER IF NOT EXISTS update_xianyu_goods_auto_reply_config_time
AFTER UPDATE ON xianyu_goods_auto_reply_config
BEGIN
    UPDATE xianyu_goods_auto_reply_config SET update_time = CURRENT_TIMESTAMP WHERE id = NEW.id;
END;

-- 商品自动回复记录表
CREATE TABLE IF NOT EXISTS xianyu_goods_auto_reply_record (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    xianyu_account_id BIGINT NOT NULL,                -- 闲鱼账号ID
    xianyu_goods_id BIGINT,                           -- 本地闲鱼商品ID
    xy_goods_id VARCHAR(100) NOT NULL,                -- 闲鱼的商品ID
    buyer_message TEXT,                               -- 买家消息内容
    reply_content TEXT,                               -- 回复消息内容
    matched_keyword VARCHAR(200),                     -- 匹配的关键词
    state TINYINT DEFAULT 0,                          -- 状态是否成功1-成功，0-失败
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,   -- 创建时间
    FOREIGN KEY (xianyu_account_id) REFERENCES xianyu_account(id)
);

-- 创建自动回复记录表索引
CREATE INDEX IF NOT EXISTS idx_auto_reply_record_account_id ON xianyu_goods_auto_reply_record(xianyu_account_id);
CREATE INDEX IF NOT EXISTS idx_auto_reply_record_xy_goods_id ON xianyu_goods_auto_reply_record(xy_goods_id);
CREATE INDEX IF NOT EXISTS idx_auto_reply_record_state ON xianyu_goods_auto_reply_record(state);
CREATE INDEX IF NOT EXISTS idx_auto_reply_record_create_time ON xianyu_goods_auto_reply_record(create_time);
