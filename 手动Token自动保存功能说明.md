# 手动Token自动保存功能说明

## 功能概述

当通过 `/api/websocket/start` 接口提供手动 `accessToken` 时，系统会自动将其保存到数据库，下次连接时可以直接使用，无需重复输入。

## 使用流程

### 场景1：首次使用手动Token

```
1. 用户手动获取 accessToken
2. 调用 /api/websocket/start，传入 accessToken
3. 系统保存 Token 到数据库（有效期20小时）
4. 使用 Token 连接 WebSocket ✅
```

### 场景2：Token 有效期内再次连接

```
1. 用户调用 /api/websocket/start（不传 accessToken）
2. 系统从数据库读取之前保存的 Token
3. 直接使用数据库中的 Token 连接 ✅
```

### 场景3：Token 过期后

```
1. 用户调用 /api/websocket/start
2. 系统检测到 Token 已过期
3. 返回滑块验证或提示需要新 Token
4. 用户获取新 Token 并传入
5. 系统更新数据库中的 Token
6. 使用新 Token 连接 ✅
```

## API 使用

### 请求格式

```http
POST /api/websocket/start
Content-Type: application/json

{
    "xianyuAccountId": 1,
    "accessToken": "AAACaRRJ/1plADqnvUCqMzjFzpW06czOnD9Xq928AuCXGl8zins5efDJ0r97uzjrDeKvIeBTdBr7l0up8tH8X/LH18WSicQ/TrPeWgOzuE/G9BqaJhvfkFP9DbkqjlogK8bniPtoQ=="
}
```

### 参数说明

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| xianyuAccountId | Long | 是 | 闲鱼账号ID |
| accessToken | String | 否 | 手动提供的accessToken，如果不提供则自动获取 |

### 响应示例

#### 成功响应

```json
{
    "code": 200,
    "msg": "WebSocket连接已启动",
    "data": null
}
```

#### 失败响应

```json
{
    "code": 500,
    "msg": "WebSocket连接启动失败",
    "data": null
}
```

## 工作原理

### 1. 手动Token处理流程

```java
// WebSocketServiceImpl.startWebSocketWithToken()

// 1. 保存Token到数据库
tokenService.saveToken(accountId, accessToken);

// 2. 使用Token连接WebSocket
connectWebSocket(accountId, cookieStr, deviceId, accessToken);
```

### 2. 自动Token处理流程

```java
// WebSocketTokenServiceImpl.getAccessToken()

// 1. 检查数据库中的Token
if (tokenExpireTime > now) {
    return websocketToken; // 使用数据库中的Token
}

// 2. Token过期，重新获取
String newToken = requestTokenFromAPI();

// 3. 保存新Token到数据库
saveTokenToDatabase(accountId, newToken);

return newToken;
```

## 优势

### vs 每次手动输入

| 特性 | 每次手动输入 | 自动保存 |
|------|------------|---------|
| 用户体验 | 繁琐 | 便捷 ✅ |
| 出错概率 | 高 | 低 ✅ |
| 效率 | 低 | 高 ✅ |

### vs 仅内存缓存

| 特性 | 内存缓存 | 数据库持久化 |
|------|---------|------------|
| 重启后 | 丢失 | 保留 ✅ |
| 多实例 | 不共享 | 共享 ✅ |
| 持久性 | 差 | 好 ✅ |

## 前端集成

### 修改前端页面

在 `websocket.html` 中，Token 输入框的值会自动保存：

```javascript
async function startWebSocket() {
    const accountId = document.getElementById('accountId').value;
    let accessToken = document.getElementById('accessToken').value.trim();
    
    // 去除空格
    accessToken = accessToken.replace(/\s+/g, '');
    
    const requestBody = {
        xianyuAccountId: parseInt(accountId)
    };
    
    // 如果提供了 Token，添加到请求中
    if (accessToken) {
        requestBody.accessToken = accessToken;
        console.log('使用手动Token，系统会自动保存到数据库');
    } else {
        console.log('未提供Token，系统会尝试使用数据库中的Token或自动获取');
    }
    
    const response = await fetch(`${API_BASE}/start`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(requestBody)
    });
    
    const result = await response.json();
    
    if (result.code === 200) {
        showMessage('✅ WebSocket连接已启动');
        if (accessToken) {
            showMessage('💾 Token已保存，下次无需重复输入');
        }
    }
}
```

## 日志示例

### 使用手动Token

```
【账号1】使用手动Token启动WebSocket连接
【账号1】accessToken长度=156
【账号1】准备保存Token到数据库...
【账号1】Token已保存到数据库
【账号1】Token已保存到数据库，过期时间: 2025-11-13 13:00:00
【账号1】准备调用通用连接方法...
【账号1】WebSocket连接成功
```

### 下次自动使用

```
【账号1】启动WebSocket连接
【账号1】使用数据库中的accessToken（剩余有效期: 18小时）
【账号1】WebSocket连接成功
```

## 测试步骤

### 1. 首次使用手动Token

```bash
curl -X POST http://localhost:8080/api/websocket/start \
  -H "Content-Type: application/json" \
  -d '{
    "xianyuAccountId": 1,
    "accessToken": "YOUR_TOKEN_HERE"
  }'
```

### 2. 验证Token已保存

```sql
SELECT 
    xianyu_account_id,
    websocket_token,
    datetime(token_expire_time/1000, 'unixepoch', 'localtime') as expire_time
FROM xianyu_cookie
WHERE xianyu_account_id = 1;
```

### 3. 再次连接（不提供Token）

```bash
curl -X POST http://localhost:8080/api/websocket/start \
  -H "Content-Type: application/json" \
  -d '{
    "xianyuAccountId": 1
  }'
```

应该看到日志：
```
【账号1】使用数据库中的accessToken（剩余有效期: XX小时）
```

## 常见问题

### Q1: 手动Token会覆盖自动获取的Token吗？

**A:** 是的。无论是手动提供还是自动获取，都会保存到数据库并覆盖旧的Token。

### Q2: Token过期后会自动更新吗？

**A:** 是的。当检测到Token过期时，系统会自动获取新Token并更新数据库。

### Q3: 可以手动清除保存的Token吗？

**A:** 可以，通过SQL：
```sql
UPDATE xianyu_cookie
SET websocket_token = NULL, token_expire_time = NULL
WHERE xianyu_account_id = 1;
```

### Q4: 多个账号的Token会互相影响吗？

**A:** 不会。每个账号的Token独立存储和管理。

## 相关文件

- `src/main/java/com/feijimiao/xianyuassistant/controller/WebSocketController.java` - API接口
- `src/main/java/com/feijimiao/xianyuassistant/service/WebSocketTokenService.java` - Token服务接口
- `src/main/java/com/feijimiao/xianyuassistant/service/impl/WebSocketTokenServiceImpl.java` - Token服务实现
- `src/main/java/com/feijimiao/xianyuassistant/service/impl/WebSocketServiceImpl.java` - WebSocket服务实现

## 总结

通过自动保存手动输入的Token：
- ✅ 提升用户体验（无需重复输入）
- ✅ 减少出错概率（避免复制错误）
- ✅ 提高效率（一次输入，多次使用）
- ✅ 支持持久化（重启后仍然有效）
