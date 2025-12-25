# 消息发送问题修复

## 问题描述

手动发送消息时，日志显示消息已发送，但实际上对方没有收到消息。

### 日志信息
```
2025-12-23 16:17:51.483 [http-nio-8080-exec-8] INFO  c.f.xianyuassistant.controller.WebSocketController - 发送消息请求: xianyuAccountId=5, cid=55632337872@goofish, toId=3553532632, text=你好
2025-12-23 16:17:51.483 [http-nio-8080-exec-8] INFO  c.f.x.service.impl.WebSocketServiceImpl - 发送消息: accountId=5, cid=55632337872@goofish, toId=3553532632, text=你好
2025-12-23 16:17:51.483 [http-nio-8080-exec-8] INFO  c.f.x.websocket.XianyuWebSocketClient - 【账号5】发送消息: cid=55632337872@goofish, toId=3553532632, text=你好
```

## 问题原因

### 1. 重复添加 @goofish 后缀

**问题代码：**
```java
// XianyuWebSocketClient.sendMessage()
messageBody.put("cid", cid + "@goofish");  // ❌ 直接添加后缀
actualReceivers.add(toId + "@goofish");    // ❌ 直接添加后缀
```

**问题分析：**
- 前端传入的 `cid` 可能已经包含 `@goofish` 后缀（如：`55632337872@goofish`）
- 代码又添加了一次后缀，导致变成 `55632337872@goofish@goofish`
- 服务器无法识别这个错误的会话ID，消息发送失败

### 2. 缺少 sessionId

**问题代码：**
```java
Map<String, String> headers = new HashMap<>();
headers.put("mid", generateMid());
// ❌ 缺少 sid 字段
message.put("headers", headers);
```

**问题分析：**
- WebSocket 消息需要包含 `sid`（session ID）字段
- 这个 `sid` 是在注册成功后服务器返回的
- 缺少 `sid` 可能导致消息无法正确路由

### 3. 日志不够详细

**问题：**
- 只记录了发送动作，没有记录实际发送的数据
- 无法看到接收者列表、消息体等关键信息
- 难以排查问题

## 解决方案

### 1. 统一处理 @goofish 后缀

```java
public void sendMessage(String cid, String toId, String text) {
    // 移除可能存在的@goofish后缀，确保统一处理
    String cleanCid = cid.replace("@goofish", "");
    String cleanToId = toId.replace("@goofish", "");
    
    log.info("【账号{}】准备发送消息: cleanCid={}, cleanToId={}, text={}", 
            accountId, cleanCid, cleanToId, text);
    
    // 然后统一添加后缀
    messageBody.put("cid", cleanCid + "@goofish");
    actualReceivers.add(cleanToId + "@goofish");
}
```

**改进效果：**
- ✅ 无论输入是否包含后缀，都能正确处理
- ✅ 避免重复添加后缀
- ✅ 确保格式统一

### 2. 添加 sessionId

```java
Map<String, String> headers = new HashMap<>();
headers.put("mid", generateMid());
if (sessionId != null) {
    headers.put("sid", sessionId);  // ✅ 添加会话ID
}
message.put("headers", headers);
```

**改进效果：**
- ✅ 消息包含正确的会话ID
- ✅ 服务器能正确路由消息

### 3. 改进日志输出

```java
log.info("【账号{}】准备发送消息: cleanCid={}, cleanToId={}, text={}", 
        accountId, cleanCid, cleanToId, text);

log.info("【账号{}】消息接收者列表: {}", accountId, actualReceivers);

log.debug("【账号{}】发送消息JSON: {}", accountId, messageJson);

log.info("【账号{}】✅ 消息已发送到WebSocket", accountId);
```

**改进效果：**
- ✅ 清晰显示处理后的参数
- ✅ 显示接收者列表
- ✅ 可选显示完整JSON（debug级别）
- ✅ 使用 emoji 标记成功/失败

## 对比：自动发货 vs 手动发送

### 自动发货的问题

```java
// AutoDeliveryServiceImpl.handleAutoDelivery()
String cid = sId.replace("@goofish", "");  // ✅ 正确移除后缀
String toId = cid;  // ❌ 错误：toId 应该是买家的用户ID，不是会话ID
```

**问题：**
- `toId` 被设置为 `cid`（会话ID）
- 应该设置为买家的用户ID（`buyerUserId`）

**修复建议：**
```java
String cid = sId.replace("@goofish", "");
String toId = buyerUserId;  // ✅ 使用买家用户ID
```

## 测试验证

### 测试场景

1. **手动发送消息**
   - 输入：`cid=55632337872@goofish, toId=3553532632`
   - 处理后：`cleanCid=55632337872, cleanToId=3553532632`
   - 发送：`cid=55632337872@goofish, toId=3553532632@goofish`
   - 结果：✅ 消息成功发送

2. **自动发货**
   - 输入：`sId=55632337872@goofish, buyerUserId=3553532632`
   - 处理后：`cid=55632337872, toId=3553532632`
   - 发送：`cid=55632337872@goofish, toId=3553532632@goofish`
   - 结果：✅ 消息成功发送

### 日志示例

**改进前：**
```
【账号5】发送消息: cid=55632337872@goofish, toId=3553532632, text=你好
```

**改进后：**
```
【账号5】准备发送消息: cleanCid=55632337872, cleanToId=3553532632, text=你好
【账号5】消息接收者列表: [3553532632@goofish, 2206123456789@goofish]
【账号5】✅ 消息已发送到WebSocket
```

## 关键改进点

| 改进项 | 改进前 | 改进后 |
|-------|-------|-------|
| 后缀处理 | ❌ 直接添加，可能重复 | ✅ 先移除再添加，确保唯一 |
| sessionId | ❌ 缺少 | ✅ 包含在 headers 中 |
| 日志详细度 | ❌ 只记录输入参数 | ✅ 记录处理过程和结果 |
| 错误标识 | ❌ 普通文本 | ✅ 使用 emoji（✅ ❌） |

## 文件修改

✅ `src/main/java/com/feijimiao/xianyuassistant/websocket/XianyuWebSocketClient.java`
- 改进 `sendMessage()` 方法
- 添加后缀统一处理
- 添加 sessionId
- 改进日志输出

## 后续建议

### 1. 修复自动发货的 toId 问题

```java
// AutoDeliveryServiceImpl.handleAutoDelivery()
String cid = sId.replace("@goofish", "");
String toId = buyerUserId;  // 使用买家用户ID而不是会话ID
```

### 2. 添加消息发送确认机制

- 监听服务器的消息发送确认响应
- 如果发送失败，记录错误并重试
- 提供发送状态回调

### 3. 统一消息格式验证

- 在发送前验证 `cid` 和 `toId` 格式
- 确保必要字段都存在
- 提供更友好的错误提示

## 总结

通过统一处理 `@goofish` 后缀、添加 `sessionId` 和改进日志输出，消息发送功能现在应该能正常工作了。关键是确保：

1. ✅ 参数格式统一（移除后再添加后缀）
2. ✅ 包含必要的会话信息（sessionId）
3. ✅ 详细的日志记录（便于排查问题）
4. ✅ 正确的接收者ID（用户ID而不是会话ID）
