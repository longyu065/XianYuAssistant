# WebSocket注册频率限制说明

## 问题描述

闲鱼WebSocket服务器对注册请求有频率限制。如果短时间内频繁注册（重连），可能会被服务器限制，导致：

1. **注册失败**：返回错误码（如429 Too Many Requests）
2. **连接被拒绝**：服务器直接拒绝连接
3. **账号被临时封禁**：严重情况下可能导致账号被临时限制

## 触发场景

### 高风险场景

1. **频繁重启应用**
   - 短时间内多次启动/停止应用
   - 每次启动都会触发WebSocket注册

2. **频繁手动重连**
   - 用户在测试页面频繁点击"启动连接"按钮
   - 短时间内多次调用 `/api/websocket/start` 接口

3. **自动重连循环**
   - Token失效后自动重连
   - 如果Token一直无效，会陷入重连循环

4. **多账号同时注册**
   - 同时启动多个账号的WebSocket连接
   - 可能触发IP级别的频率限制

### 低风险场景

1. **正常使用**
   - 应用启动一次，保持长连接
   - 偶尔的网络断线重连

2. **Token自然过期**
   - Token过期后重新获取并重连
   - 通常间隔较长（几小时或几天）

## 当前实现的保护机制

### 1. Token缓存机制

```java
// WebSocketTokenService 会缓存Token到数据库
// 避免每次连接都重新获取Token
String accessToken = tokenService.getAccessToken(accountId, cookieStr, deviceId);
```

**优点**：
- 减少Token刷新频率
- 降低注册请求频率

### 2. 连接状态检查

```java
// 启动前检查是否已连接
if (webSocketClients.containsKey(accountId)) {
    XianyuWebSocketClient existingClient = webSocketClients.get(accountId);
    if (existingClient.isConnected()) {
        log.info("WebSocket已连接: accountId={}", accountId);
        return true; // 直接返回，不重新连接
    }
}
```

**优点**：
- 避免重复连接
- 防止误操作导致的频繁重连

### 3. 自动重连延迟

```java
// Token失效时的自动重连
client.setOnTokenExpired(() -> {
    // 停止当前连接
    stopWebSocket(accountId);
    
    // 等待1秒再重连
    Thread.sleep(1000);
    
    // 重新启动连接
    startWebSocket(accountId);
});
```

**优点**：
- 避免立即重连
- 给服务器缓冲时间

## 建议的优化方案

### 1. 增加重连间隔（推荐）

在自动重连时增加更长的延迟：

```java
// 建议延迟3-5秒
Thread.sleep(3000); // 3秒延迟
```

### 2. 实现指数退避策略

```java
private int reconnectAttempts = 0;
private static final int MAX_RECONNECT_ATTEMPTS = 5;

client.setOnTokenExpired(() -> {
    if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
        log.error("【账号{}】重连次数超过限制，停止重连", accountId);
        return;
    }
    
    // 指数退避：1秒、2秒、4秒、8秒、16秒
    long delay = (long) Math.pow(2, reconnectAttempts) * 1000;
    log.info("【账号{}】等待{}秒后重连（第{}次尝试）", accountId, delay/1000, reconnectAttempts + 1);
    
    Thread.sleep(delay);
    
    reconnectAttempts++;
    boolean success = startWebSocket(accountId);
    
    if (success) {
        reconnectAttempts = 0; // 重置计数器
    }
});
```

### 3. 添加重连限流器

```java
// 使用Guava的RateLimiter
private final RateLimiter reconnectLimiter = RateLimiter.create(0.1); // 每10秒最多1次

public boolean startWebSocket(Long accountId) {
    // 获取许可，如果超过频率则等待
    reconnectLimiter.acquire();
    
    // 继续连接逻辑...
}
```

### 4. 记录重连历史

```java
// 记录最近的重连时间
private final Map<Long, Long> lastReconnectTime = new ConcurrentHashMap<>();

public boolean startWebSocket(Long accountId) {
    Long lastTime = lastReconnectTime.get(accountId);
    if (lastTime != null) {
        long elapsed = System.currentTimeMillis() - lastTime;
        if (elapsed < 5000) { // 5秒内不允许重连
            log.warn("【账号{}】重连过于频繁，请等待{}秒", accountId, (5000 - elapsed) / 1000);
            return false;
        }
    }
    
    lastReconnectTime.put(accountId, System.currentTimeMillis());
    
    // 继续连接逻辑...
}
```

## 最佳实践

### 开发测试阶段

1. **使用Token缓存**
   - 第一次获取Token后保存到数据库
   - 后续测试使用缓存的Token
   - 避免频繁调用Token刷新接口

2. **限制测试频率**
   - 测试重连功能时，间隔至少5秒
   - 使用日志观察连接状态，而不是频繁重启

3. **使用单账号测试**
   - 先用一个账号测试功能
   - 确认稳定后再测试多账号

### 生产环境

1. **保持长连接**
   - 应用启动后保持WebSocket连接
   - 不要频繁重启应用

2. **优雅处理断线**
   - 网络断线时等待一段时间再重连
   - 使用指数退避策略

3. **监控连接状态**
   - 记录重连次数和频率
   - 设置告警阈值

4. **分批启动多账号**
   - 如果有多个账号，不要同时启动
   - 每个账号间隔5-10秒启动

## 错误处理

### 识别频率限制错误

```java
@Override
public void onMessage(String message) {
    Map<String, Object> messageData = objectMapper.readValue(message, Map.class);
    Object code = messageData.get("code");
    
    // 检查是否是频率限制错误
    if (code != null && (code.equals(429) || "429".equals(code.toString()))) {
        log.error("【账号{}】❌ 注册频率过高(429)，请稍后再试", accountId);
        
        // 停止自动重连
        stopAutoReconnect();
        
        // 通知用户
        notifyUser("注册频率过高，请等待5分钟后再试");
    }
}
```

### 用户提示

在前端页面添加提示：

```html
<div class="warning-box">
    <h4>⚠️ 注意事项</h4>
    <ul>
        <li>请勿频繁点击"启动连接"按钮</li>
        <li>建议间隔至少5秒再重试</li>
        <li>如果提示"注册频率过高"，请等待5分钟</li>
    </ul>
</div>
```

## 监控指标

建议监控以下指标：

1. **重连次数**：每小时/每天的重连次数
2. **重连间隔**：两次重连之间的时间间隔
3. **失败率**：注册失败的比例
4. **429错误**：频率限制错误的次数

## 总结

### 关键要点

1. ✅ **使用Token缓存**：避免频繁刷新Token
2. ✅ **检查连接状态**：避免重复连接
3. ✅ **增加重连延迟**：至少3-5秒
4. ✅ **实现退避策略**：失败后逐渐增加延迟
5. ✅ **限制重连次数**：避免无限重连循环

### 安全的重连频率

- **正常情况**：每次连接保持数小时或数天
- **异常重连**：间隔至少5秒
- **连续失败**：使用指数退避，最长等待几分钟
- **多账号**：每个账号间隔5-10秒启动

### 风险等级

| 场景 | 风险等级 | 建议间隔 |
|------|---------|---------|
| 正常使用 | 低 | 无限制 |
| 偶尔重连 | 低 | 5秒 |
| 频繁测试 | 中 | 10秒 |
| 自动重连循环 | 高 | 指数退避 |
| 多账号同时启动 | 高 | 每个间隔10秒 |

遵循这些建议可以有效避免触发频率限制，保证系统稳定运行。
