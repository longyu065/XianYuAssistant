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
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,  -- 创建时间
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP,  -- 更新时间
    FOREIGN KEY (xianyu_account_id) REFERENCES xianyu_account(id)
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_account_unb ON xianyu_account(unb);
CREATE INDEX IF NOT EXISTS idx_cookie_account_id ON xianyu_cookie(xianyu_account_id);
CREATE INDEX IF NOT EXISTS idx_cookie_status ON xianyu_cookie(cookie_status);

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
    title VARCHAR(500),                           -- 商品标题
    cover_pic TEXT,                               -- 封面图片URL
    info_pic TEXT,                                -- 商品详情图片（JSON数组）
    detail_info TEXT,                             -- 商品详情信息（预留字段）
    sold_price VARCHAR(50),                       -- 商品价格
    status TINYINT DEFAULT 0,                     -- 商品状态 0:在售 1:已下架 2:已售出
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,  -- 创建时间
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP   -- 更新时间
);

-- 创建商品表索引
CREATE UNIQUE INDEX IF NOT EXISTS idx_goods_xy_good_id ON xianyu_goods_info(xy_good_id);
CREATE INDEX IF NOT EXISTS idx_goods_status ON xianyu_goods_info(status);

-- 创建商品表更新时间触发器
CREATE TRIGGER IF NOT EXISTS update_xianyu_goods_info_time 
AFTER UPDATE ON xianyu_goods_info
BEGIN
    UPDATE xianyu_goods_info SET updated_time = CURRENT_TIMESTAMP WHERE id = NEW.id;
END
$
