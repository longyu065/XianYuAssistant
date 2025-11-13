-- 添加 WebSocket Token 相关字段到 xianyu_cookie 表
-- 执行时间：2025-11-12

-- 添加 websocket_token 字段（存储 accessToken）
ALTER TABLE xianyu_cookie ADD COLUMN websocket_token TEXT;

-- 添加 token_expire_time 字段（存储 Token 过期时间）
ALTER TABLE xianyu_cookie ADD COLUMN token_expire_time INTEGER;

-- 添加索引以提高查询性能
CREATE INDEX IF NOT EXISTS idx_token_expire_time ON xianyu_cookie(token_expire_time);

-- 说明：
-- websocket_token: 存储 WebSocket 的 accessToken
-- token_expire_time: 存储 Token 的过期时间戳（毫秒），用于判断是否需要刷新
-- Token 有效期为 20 小时，过期后需要重新获取
