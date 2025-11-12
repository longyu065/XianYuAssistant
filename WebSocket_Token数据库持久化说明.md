# WebSocket Token 数据库持久化说明

## 功能概述

将 WebSocket 的 accessToken 保存到数据库，避免频繁请求 Token API，减少触发滑块验证的概率。

## 数据库变更

### 新增字段

在 `xianyu_cookie` 表中添加两个字段：

| 字段名 | 类型 | 说明 |
|--------|------|------|
| `websocket_token` | TEXT | 存储 WebSocket 的 accessToken |
| `token_expire_time` | INTEGER | Token 过期时间戳（毫秒） |

### 迁移脚本

执行 `src/main/resources/sql/migration_add_websocket_token.sql`：

```sql
-- 添加字段
ALTER TABLE xianyu_cookie ADD COLUMN websocket_token TEXT;
ALTER TABLE xianyu_cookie ADD COLUMN token_expire_time INTEGER;

-- 添加索引
CREATE INDEX IF NOT EXISTS idx_token_expire_time ON xianyu_cookie(token_expire_time);
```

### 执行迁移

#### 方法1：手动执行（推荐）

```bash
# Windows
sqlite3 data/xianyu.db < src/main/resources/sql/migration_add_websocket_token.sql

# 或者在 SQLite 命令行中
sqlite3 data/xianyu.db
.read src/main/resources/sql/migration_add_websocket_token.sql
.quit
```

#### 方法2：使用数据库工具

1. 打开 DB Browser for SQLite
2. 打开 `data/xianyu.db`
3. 执行 SQL 标签
4. 粘贴并执行迁移脚本

#### 方法3：自动迁移（应用启动时）

系统会在启动时自动检测并执行迁移（如果配置了自动迁移）。

## 工作原理

### 1. Token 获取流程

```
用户请求连接
    ↓
检查数据库中的 Token
    ├─ 有效 → 直接使用 ✅
    │         （避免请求 API）
    │
    └─ 无效/不存在 → 请求 Token API
                      ↓
                   获取新 Token
                      ↓
                   保存到数据库
                      ↓
                   使用新 Token ✅
```

### 2. Token 有效期判断

```java
// 检查 Token 是否有效
if (tokenExpireTime > System.currentTimeMillis()) {
    // Token 有效，直接使用
    return websocketToken;
} else {
    // Token 过期，重新获取
    return getNewToken();
}
```

### 3. Token 保存

```java
// 计算过期时间（当前时间 + 20小时）
long expireTime = System.currentTimeMillis() + TOKEN_VALID_DURATION;

// 更新数据库
xianyuCookieMapper.update(null,
    new LambdaUpdateWrapper<XianyuCookie>()
        .eq(XianyuCookie::getXianyuAccountId, accountId)
        .set(XianyuCookie::getWebsocketToken, token)
        .set(XianyuCookie::getTokenExpireTime, expireTime)
);
```

## 代码变更

### 1. 实体类更新

`XianyuCookie.java` 添加字段：

```java
/**
 * WebSocket accessToken
 */
private String websocketToken;

/**
 * Token过期时间戳（毫秒）
 */
private Long tokenExpireTime;
```

### 2. Token 服务更新

`WebSocketTokenServiceImpl.java` 主要变更：

#### 变更前（内存缓存）
```java
// 使用 ConcurrentHashMap 缓存
private final Map<Long, TokenCache> tokenCache = new ConcurrentHashMap<>();

// 检查内存缓存
TokenCache cached = tokenCache.get(accountId);
if (cached != null && cached.isValid()) {
    return cached.token;
}
```

#### 变更后（数据库持久化）
```java
// 从数据库查询
XianyuCookie cookieEntity = xianyuCookieMapper.selectOne(...);

// 检查数据库中的 Token
if (cookieEntity.getTokenExpireTime() > System.currentTimeMillis()) {
    return cookieEntity.getWebsocketToken();
}
```

### 3. 新增方法

```java
/**
 * 保存 Token 到数据库
 */
private void saveTokenToDatabase(Long accountId, String token) {
    long expireTime = System.currentTimeMillis() + TOKEN_VALID_DURATION;
    
    xianyuCookieMapper.update(null,
        new LambdaUpdateWrapper<XianyuCookie>()
            .eq(XianyuCookie::getXianyuAccountId, accountId)
            .set(XianyuCookie::getWebsocketToken, token)
            .set(XianyuCookie::getTokenExpireTime, expireTime)
    );
}
```

## 优势

### vs 内存缓存

| 特性 | 内存缓存 | 数据库持久化 |
|------|---------|------------|
| 持久性 | ❌ 重启丢失 | ✅ 永久保存 |
| 多实例共享 | ❌ 不支持 | ✅ 支持 |
| 查询性能 | 极快 | 快 |
| 内存占用 | 低 | 无 |
| 适用场景 | 单实例 | 生产环境 |

### 减少 API 请求

**场景1：应用重启**
- 内存缓存：需要重新请求 Token
- 数据库持久化：直接使用保存的 Token ✅

**场景2：多次连接**
- 内存缓存：首次请求后缓存
- 数据库持久化：首次请求后保存，后续直接使用 ✅

**场景3：多实例部署**
- 内存缓存：每个实例独立缓存
- 数据库持久化：所有实例共享 Token ✅

## 使用示例

### 场景1：首次连接

```
1. 用户点击"启动监听"
2. 系统检查数据库：无 Token
3. 请求 Token API
4. 保存 Token 到数据库（有效期20小时）
5. 使用 Token 连接 WebSocket ✅
```

### 场景2：再次连接（Token 有效）

```
1. 用户点击"启动监听"
2. 系统检查数据库：有 Token，剩余18小时
3. 直接使用数据库中的 Token ✅
4. 连接 WebSocket
```

**优势：** 跳过了 Token API 请求，避免触发滑块验证！

### 场景3：Token 过期

```
1. 用户点击"启动监听"
2. 系统检查数据库：Token 已过期
3. 请求 Token API 获取新 Token
4. 更新数据库中的 Token
5. 使用新 Token 连接 WebSocket ✅
```

### 场景4：应用重启

```
1. 应用重启
2. 用户点击"启动监听"
3. 系统检查数据库：有 Token，剩余15小时
4. 直接使用数据库中的 Token ✅
5. 连接 WebSocket
```

**优势：** 重启后无需重新获取 Token！

## 日志示例

### 使用数据库中的 Token

```
【账号1】使用数据库中的accessToken（剩余有效期: 18小时）
【账号1】WebSocket连接成功
```

### 获取新 Token 并保存

```
【账号1】开始获取新的accessToken...
【账号1】accessToken获取成功并已保存到数据库
【账号1】Token已保存到数据库，过期时间: 2025-11-13 13:00:00
【账号1】WebSocket连接成功
```

### Token 过期，重新获取

```
【账号1】数据库中的Token已过期，需要重新获取
【账号1】开始获取新的accessToken...
【账号1】accessToken获取成功并已保存到数据库
【账号1】WebSocket连接成功
```

## 性能优化

### 1. 索引优化

已为 `token_expire_time` 字段创建索引：

```sql
CREATE INDEX idx_token_expire_time ON xianyu_cookie(token_expire_time);
```

查询性能：O(log n)

### 2. 查询优化

使用 MyBatis-Plus 的 Lambda 查询：

```java
xianyuCookieMapper.selectOne(
    new LambdaQueryWrapper<XianyuCookie>()
        .eq(XianyuCookie::getXianyuAccountId, accountId)
);
```

只查询需要的记录，避免全表扫描。

### 3. 更新优化

使用 Lambda 更新，只更新必要的字段：

```java
xianyuCookieMapper.update(null,
    new LambdaUpdateWrapper<XianyuCookie>()
        .eq(XianyuCookie::getXianyuAccountId, accountId)
        .set(XianyuCookie::getWebsocketToken, token)
        .set(XianyuCookie::getTokenExpireTime, expireTime)
);
```

## 数据维护

### 查看 Token 信息

```sql
SELECT 
    xianyu_account_id,
    websocket_token,
    datetime(token_expire_time/1000, 'unixepoch', 'localtime') as expire_time,
    CASE 
        WHEN token_expire_time > strftime('%s', 'now') * 1000 THEN '有效'
        ELSE '已过期'
    END as status
FROM xianyu_cookie
WHERE websocket_token IS NOT NULL;
```

### 清理过期 Token

```sql
-- 清理已过期的 Token
UPDATE xianyu_cookie
SET websocket_token = NULL,
    token_expire_time = NULL
WHERE token_expire_time < strftime('%s', 'now') * 1000;
```

### 手动设置 Token

```sql
-- 手动设置 Token（用于测试）
UPDATE xianyu_cookie
SET websocket_token = 'YOUR_TOKEN_HERE',
    token_expire_time = strftime('%s', 'now') * 1000 + 72000000  -- 20小时后过期
WHERE xianyu_account_id = 1;
```

## 故障排查

### 问题1：Token 未保存

**症状：** 日志显示"Token保存失败，未找到对应的Cookie记录"

**原因：** 数据库中没有对应账号的 Cookie 记录

**解决：**
1. 先通过扫码登录创建 Cookie 记录
2. 再尝试连接 WebSocket

### 问题2：Token 一直过期

**症状：** 每次都需要重新获取 Token

**原因：** 
- 系统时间不正确
- Token 过期时间计算错误

**解决：**
1. 检查系统时间
2. 查看数据库中的 `token_expire_time` 是否正确

### 问题3：数据库字段不存在

**症状：** 启动时报错"no such column: websocket_token"

**原因：** 未执行数据库迁移

**解决：**
执行迁移脚本：
```bash
sqlite3 data/xianyu.db < src/main/resources/sql/migration_add_websocket_token.sql
```

## 相关文件

- `src/main/resources/sql/migration_add_websocket_token.sql` - 数据库迁移脚本
- `src/main/java/com/feijimiao/xianyuassistant/entity/XianyuCookie.java` - 实体类
- `src/main/java/com/feijimiao/xianyuassistant/service/impl/WebSocketTokenServiceImpl.java` - Token 服务

## 测试步骤

1. **执行数据库迁移**
   ```bash
   sqlite3 data/xianyu.db < src/main/resources/sql/migration_add_websocket_token.sql
   ```

2. **重启应用**
   ```bash
   mvn spring-boot:run
   ```

3. **首次连接**
   - 访问 WebSocket 页面
   - 点击"启动监听"
   - 查看日志：应该显示"Token已保存到数据库"

4. **再次连接**
   - 停止连接
   - 再次点击"启动监听"
   - 查看日志：应该显示"使用数据库中的accessToken"

5. **验证持久化**
   - 重启应用
   - 点击"启动监听"
   - 查看日志：应该仍然显示"使用数据库中的accessToken"

## 总结

通过将 Token 保存到数据库：
- ✅ 减少 API 请求频率
- ✅ 降低触发滑块验证的概率
- ✅ 支持应用重启后继续使用
- ✅ 支持多实例部署
- ✅ 提高系统稳定性
