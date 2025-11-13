# Token自动刷新和重连机制说明

## 功能概述

实现了WebSocket连接的Token自动刷新和重连机制。当Token失效（返回401错误）时，系统会自动：
1. 检测到401错误
2. 停止当前连接
3. 重新获取Token
4. 自动重新连接

## 实现原理

### 1. 401错误检测

在`XianyuWebSocketClient.handleMessage`方法中，检测服务器返回的错误码：

```java
// 检查是否是401错误（Token失效）
if (code != null && (code.equals(401) || "401".equals(code.toString()))) {
    log.error("【账号{}】❌ Token失效(401)，需要重新获取Token并重连", accountId);
    
    // 触发Token失效回调
    if (onTokenExpired != null) {
        onTokenExpired.run();
    }
    return;
}
```

### 2. 自动重连回调

在`WebSocketServiceImpl.connectWebSocket`方法中，设置Token失效回调：

```java
// 设置Token失效回调（自动重连）
client.setOnTokenExpired(() -> {
    log.warn("【账号{}】Token失效，开始自动重连流程...", accountId);
    
    // 停止当前连接
    stopWebSocket(accountId);
    
    // 等待1秒
    Thread.sleep(1000);
    
    // 重新启动连接（会自动刷新Token）
    boolean success = startWebSocket(accountId);
    
    if (success) {
        log.info("【账号{}】✅ 自动重连成功", accountId);
    } else {
        log.error("【账号{}】❌ 自动重连失败", accountId);
    }
});
```

### 3. Token刷新逻辑

在`WebSocketTokenServiceImpl.getAccessToken`方法中：

```java
// 1. 先检查数据库中的Token是否有效
if (cookieEntity != null && cookieEntity.getWebsocketToken() != null) {
    long now = System.currentTimeMillis();
    if (cookieEntity.getTokenExpireTime() > now) {
        // Token有效，直接使用
        return cookieEntity.getWebsocketToken();
    } else {
        // Token过期，需要重新获取
        log.info("【账号{}】数据库中的Token已过期，需要重新获取", accountId);
    }
}

// 2. 调用API获取新Token
String newToken = refreshTokenFromApi(accountId, cookiesStr, deviceId);

// 3. 保存到数据库
saveToken(accountId, newToken);

return newToken;
```

## 工作流程

### 正常流程

```
1. 启动WebSocket连接
   ↓
2. 检查数据库中的Token
   ↓
3. Token有效 → 使用现有Token
   ↓
4. 连接成功，开始接收消息
```

### Token失效流程

```
1. 接收到401错误
   ↓
2. 触发Token失效回调
   ↓
3. 停止当前连接
   ↓
4. 等待1秒
   ↓
5. 调用startWebSocket重新连接
   ↓
6. 自动调用getAccessToken刷新Token
   ↓
7. 使用新Token重新连接
   ↓
8. 连接成功，继续接收消息
```

## 日志示例

### 检测到Token失效

```
【账号1】消息#1 [响应(code=401)]: {"code":401,"body":{"reason":"token is not found"}}
【账号1】❌ Token失效(401)，需要重新获取Token并重连
【账号1】触发Token失效回调，准备重新获取Token...
【账号1】Token失效，开始自动重连流程...
```

### 自动重连过程

```
【账号1】停止WebSocket连接: accountId=1
【账号1】WebSocket连接已关闭
【账号1】心跳任务已停止
【账号1】重新启动WebSocket连接（自动刷新Token）
【账号1】启动WebSocket连接: accountId=1
【账号1】数据库中的Token已过期，需要重新获取
【账号1】开始获取新的accessToken...
【账号1】调用Token API...
【账号1】Token API响应成功
【账号1】✅ 获取新Token成功，有效期20小时
【账号1】Token已保存到数据库
【账号1】accessToken获取成功: accountId=1, token长度=XXX
```

### 重连成功

```
【账号1】WebSocket连接建立成功
【账号1】开始WebSocket初始化流程...
【账号1】已发送注册消息
【账号1】消息#1 [响应(code=200)]: {"code":200,"headers":{"reg-sid":"..."}}
【账号1】✅ 注册成功
【账号1】✅ 自动重连成功
```

## 适用场景

### 1. 手动输入Token后过期

**场景**：用户手动输入了accessToken，但Token在使用过程中过期了。

**处理**：
- 系统检测到401错误
- 自动停止连接
- 重新调用API获取新Token
- 自动重连

### 2. 数据库Token过期

**场景**：数据库中保存的Token已经过期（超过20小时）。

**处理**：
- `getAccessToken`方法检查Token有效期
- 发现过期，自动调用API刷新
- 使用新Token连接

### 3. 长时间运行后Token失效

**场景**：WebSocket连接已经运行了很长时间，Token自然过期。

**处理**：
- 服务器返回401错误
- 触发自动重连机制
- 刷新Token并重连

## 配置参数

### Token有效期

```java
// 20小时（参考Python的TOKEN_REFRESH_INTERVAL）
private static final long TOKEN_VALID_DURATION = 20 * 60 * 60 * 1000;
```

### 重连等待时间

```java
// 等待1秒后重连
Thread.sleep(1000);
```

### 心跳间隔

```java
// 每15秒发送一次心跳
scheduleAtFixedRate(heartbeatTask, 15, 15, TimeUnit.SECONDS);
```

## 注意事项

### 1. 避免频繁重连

如果Token刷新失败，不会无限重试，避免对服务器造成压力。

### 2. Cookie有效性

Token刷新依赖于Cookie的有效性。如果Cookie失效，需要重新扫码登录。

### 3. 并发控制

重连过程中会先停止旧连接，避免同一账号有多个连接。

### 4. 线程安全

使用`ConcurrentHashMap`存储WebSocket客户端，保证线程安全。

## 错误处理

### Token刷新失败

```java
if (accessToken == null || accessToken.isEmpty()) {
    log.error("【账号{}】获取accessToken失败", accountId);
    log.error("【账号{}】无法继续WebSocket连接，请检查Cookie是否有效", accountId);
    return false;
}
```

**可能原因**：
- Cookie失效
- 网络问题
- API限流

**解决方法**：
- 重新扫码登录
- 检查网络连接
- 等待一段时间后重试

### 重连失败

```java
if (success) {
    log.info("【账号{}】✅ 自动重连成功", accountId);
} else {
    log.error("【账号{}】❌ 自动重连失败", accountId);
}
```

**可能原因**：
- Token刷新失败
- 网络问题
- 服务器问题

**解决方法**：
- 查看详细日志
- 手动重新启动连接
- 检查Cookie和网络

## 测试方法

### 1. 模拟Token过期

```sql
-- 将数据库中的Token过期时间设置为过去
UPDATE xianyu_cookie 
SET token_expire_time = 0 
WHERE xianyu_account_id = 1;
```

然后启动WebSocket连接，观察是否自动刷新Token。

### 2. 使用过期Token

在Web界面手动输入一个已经过期的Token，观察是否触发自动重连。

### 3. 长时间运行测试

让WebSocket连接运行超过20小时，观察Token过期后是否自动刷新。

## 与Python版本对比

### Python版本

```python
async def refresh_token(self):
    """刷新Token"""
    try:
        # 调用API获取新Token
        token = await self._get_token_from_api()
        
        # 保存Token
        self.access_token = token
        
        return token
    except Exception as e:
        logger.error(f"刷新Token失败: {e}")
        return None
```

### Java版本

```java
public String getAccessToken(Long accountId, String cookiesStr, String deviceId) {
    // 1. 检查数据库中的Token
    if (isTokenValid(accountId)) {
        return getCachedToken(accountId);
    }
    
    // 2. 调用API获取新Token
    String newToken = refreshTokenFromApi(accountId, cookiesStr, deviceId);
    
    // 3. 保存到数据库
    saveToken(accountId, newToken);
    
    return newToken;
}
```

**主要区别**：
- Java版本增加了数据库缓存
- Java版本增加了自动重连机制
- Java版本增加了Token有效期检查

## 优势

1. **自动化**：无需手动干预，系统自动处理Token过期
2. **可靠性**：多重检查机制，确保Token始终有效
3. **用户友好**：用户无感知，连接自动恢复
4. **日志完善**：详细的日志记录，方便排查问题

## 未来改进

1. **重试策略**：增加指数退避重试机制
2. **通知机制**：Token刷新失败时通知用户
3. **监控告警**：集成监控系统，及时发现问题
4. **性能优化**：减少不必要的Token刷新
