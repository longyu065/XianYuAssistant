# 注册成功后保存Token说明

## 功能概述

只有在 WebSocket `/reg` 注册成功（收到 `code=200` 且包含 `reg-sid`）后，才将 accessToken 保存到数据库，确保 Token 是有效的。

## 为什么要这样做？

### 问题场景

**之前的逻辑：**
```
获取Token → 立即保存到数据库 → 连接WebSocket → 注册
```

**问题：**
- 如果 Token 无效，仍然会被保存
- 如果注册失败（401错误），数据库中保存的是无效 Token
- 下次连接时会使用无效 Token，导致重复失败

### 优化后的逻辑

```
获取Token → 连接WebSocket → 发送注册消息 → 等待注册响应
    ↓
收到 code=200 且 reg-sid 存在
    ↓
✅ 注册成功 → 保存Token到数据库
```

**优势：**
- ✅ 只保存经过验证的有效 Token
- ✅ 避免保存无效 Token
- ✅ 提高数据库中 Token 的可靠性

## 实现原理

### 1. 注册成功回调机制

#### XianyuWebSocketClient 添加回调

```java
// 注册成功回调
private Runnable onRegistrationSuccess;

public void setOnRegistrationSuccess(Runnable callback) {
    this.onRegistrationSuccess = callback;
}
```

#### 检测注册成功并触发回调

```java
// 检查是否是注册响应
if (code == 200 && headers.containsKey("reg-sid")) {
    log.info("【账号{}】✅ 注册成功，reg-sid: {}", accountId, headers.get("reg-sid"));
    
    // 触发注册成功回调
    if (onRegistrationSuccess != null) {
        onRegistrationSuccess.run();
    }
}
```

### 2. 设置回调保存Token

#### WebSocketServiceImpl 设置回调

```java
// 设置注册成功回调（保存Token）
client.setOnRegistrationSuccess(() -> {
    log.info("【账号{}】注册成功回调被触发，开始保存Token到数据库", accountId);
    tokenService.saveToken(accountId, accessToken);
    log.info("【账号{}】✅ Token已成功保存到数据库", accountId);
});
```

## 工作流程

### 完整流程图

```
1. 用户调用 /api/websocket/start
    ↓
2. 获取或使用提供的 accessToken
    ↓
3. 创建 WebSocket 客户端
    ↓
4. 设置注册成功回调（保存Token的逻辑）
    ↓
5. 连接 WebSocket
    ↓
6. 发送 /reg 注册消息（包含Token）
    ↓
7. 等待服务器响应
    ↓
8. 收到响应：code=200 且包含 reg-sid
    ↓
9. ✅ 注册成功！触发回调
    ↓
10. 保存 Token 到数据库
    ↓
11. 继续接收消息
```

### 失败场景

```
1. 用户调用 /api/websocket/start
    ↓
2. 获取 accessToken
    ↓
3. 连接 WebSocket
    ↓
4. 发送 /reg 注册消息
    ↓
5. 收到响应：code=401（Token无效）
    ↓
6. ❌ 注册失败！不触发回调
    ↓
7. Token 不会被保存到数据库 ✅
    ↓
8. 返回错误给用户
```

## 日志示例

### 成功场景

```
【账号1】准备调用通用连接方法（Token将在注册成功后保存）...
【账号1】正在连接WebSocket: wss://wss-goofish.dingtalk.com/
【账号1】WebSocket连接建立成功
【账号1】已发送注册消息
【账号1】消息#1 [响应(code=200)]: {...}
【账号1】✅ 注册成功，reg-sid: xxx
【账号1】注册成功回调被触发，开始保存Token到数据库
【账号1】Token已保存到数据库，过期时间: 2025-11-13 13:00:00
【账号1】✅ Token已成功保存到数据库
【账号1】WebSocket连接成功
```

### 失败场景（Token无效）

```
【账号1】准备调用通用连接方法（Token将在注册成功后保存）...
【账号1】正在连接WebSocket: wss://wss-goofish.dingtalk.com/
【账号1】WebSocket连接建立成功
【账号1】已发送注册消息
【账号1】消息#1 [响应(code=401)]: {"body":{"reason":"decode token failed",...}}
【账号1】❌ 注册失败：decode token failed
【账号1】Token未保存（因为注册失败）
```

## 代码变更

### 1. XianyuWebSocketClient

**添加回调机制：**

```java
// 添加字段
private Runnable onRegistrationSuccess;

// 添加方法
public void setOnRegistrationSuccess(Runnable callback) {
    this.onRegistrationSuccess = callback;
}

// 触发回调
if (headers.containsKey("reg-sid")) {
    if (onRegistrationSuccess != null) {
        onRegistrationSuccess.run();
    }
}
```

### 2. WebSocketServiceImpl

**移除立即保存，改为回调保存：**

```java
// 之前：立即保存
tokenService.saveToken(accountId, accessToken);

// 现在：设置回调
client.setOnRegistrationSuccess(() -> {
    tokenService.saveToken(accountId, accessToken);
});
```

## 优势对比

### vs 立即保存

| 特性 | 立即保存 | 注册成功后保存 |
|------|---------|--------------|
| Token有效性 | 未验证 | 已验证 ✅ |
| 失败处理 | 保存无效Token | 不保存 ✅ |
| 数据可靠性 | 低 | 高 ✅ |
| 用户体验 | 可能重复失败 | 避免重复失败 ✅ |

## 测试场景

### 场景1：有效Token

```bash
# 1. 使用有效Token
curl -X POST http://localhost:8080/api/websocket/start \
  -H "Content-Type: application/json" \
  -d '{
    "xianyuAccountId": 1,
    "accessToken": "VALID_TOKEN_HERE"
  }'

# 2. 查看日志
# 应该看到：✅ 注册成功 → Token已成功保存到数据库

# 3. 查询数据库
SELECT websocket_token FROM xianyu_cookie WHERE xianyu_account_id = 1;
# 应该有值
```

### 场景2：无效Token

```bash
# 1. 使用无效Token
curl -X POST http://localhost:8080/api/websocket/start \
  -H "Content-Type: application/json" \
  -d '{
    "xianyuAccountId": 1,
    "accessToken": "INVALID_TOKEN"
  }'

# 2. 查看日志
# 应该看到：❌ 注册失败 → Token未保存

# 3. 查询数据库
SELECT websocket_token FROM xianyu_cookie WHERE xianyu_account_id = 1;
# 应该为空或保持旧值
```

### 场景3：自动获取Token

```bash
# 1. 不提供Token，让系统自动获取
curl -X POST http://localhost:8080/api/websocket/start \
  -H "Content-Type: application/json" \
  -d '{
    "xianyuAccountId": 1
  }'

# 2. 如果自动获取成功
# 应该看到：✅ 注册成功 → Token已成功保存到数据库

# 3. 如果遇到滑块验证
# 应该看到：需要滑块验证 → Token未保存
```

## 注意事项

### 1. 回调执行时机

回调在收到注册成功响应时立即执行，通常在连接建立后 1-2 秒内。

### 2. 异常处理

如果回调执行失败（如数据库错误），会记录错误日志但不影响 WebSocket 连接。

### 3. 并发安全

回调在 WebSocket 消息处理线程中执行，已通过信号量控制并发。

### 4. Token 更新

如果数据库中已有 Token，注册成功后会更新为新的 Token。

## 相关文件

- `src/main/java/com/feijimiao/xianyuassistant/websocket/XianyuWebSocketClient.java` - 添加回调机制
- `src/main/java/com/feijimiao/xianyuassistant/service/impl/WebSocketServiceImpl.java` - 设置回调
- `src/main/java/com/feijimiao/xianyuassistant/service/impl/WebSocketTokenServiceImpl.java` - 保存Token

## 总结

通过在注册成功后才保存 Token：
- ✅ 确保数据库中只保存有效的 Token
- ✅ 避免无效 Token 导致的重复失败
- ✅ 提高系统的可靠性和用户体验
- ✅ 减少不必要的数据库写入
