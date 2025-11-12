# WebSocket 代码结构对照说明

## Python vs Java 实现对照

### 1. 主要实现文件对照

| Python 文件/方法 | Java 对应实现 | 说明 |
|-----------------|--------------|------|
| `XianyuAutoAsync.py` | `WebSocketServiceImpl.java` | 主要业务逻辑 |
| `main()` | `startWebSocket()` | WebSocket连接和消息监听主循环 |
| `handle_message()` | `XianyuWebSocketClient.onMessage()` | 消息处理核心逻辑 |
| `_handle_message_with_semaphore()` | 未实现 | 带信号量的消息处理包装器（建议添加） |
| `handle_heartbeat_response()` | `onMessage()` 中的心跳处理 | 心跳响应处理 |
| `_create_websocket_connection()` | `startWebSocket()` | WebSocket连接创建 |

### 2. 工具类对照

| Python 工具类 | Java 对应实现 | 说明 |
|--------------|--------------|------|
| `utils/ws_utils.py` | `XianyuWebSocketClient.java` | WebSocket客户端工具类 |
| `WebSocketClient.connect()` | `connectBlocking()` | 建立连接 |
| `WebSocketClient.receive()` | `onMessage()` | 接收消息 |
| `WebSocketClient.send()` | `send()` | 发送消息 |
| `WebSocketClient.run()` | 自动运行（继承自WebSocketClient） | 运行客户端 |

## 当前 Java 实现的文件结构

### 核心文件

1. **XianyuWebSocketClient.java** - WebSocket客户端
   - 位置: `src/main/java/com/feijimiao/xianyuassistant/websocket/`
   - 功能: 处理WebSocket连接、消息接收、心跳发送
   - 对应Python: `utils/ws_utils.py` 的 `WebSocketClient` 类

2. **WebSocketServiceImpl.java** - WebSocket服务实现
   - 位置: `src/main/java/com/feijimiao/xianyuassistant/service/impl/`
   - 功能: 管理WebSocket连接生命周期、心跳调度
   - 对应Python: `XianyuAutoAsync.py` 的主要逻辑

3. **WebSocketController.java** - WebSocket控制器
   - 位置: `src/main/java/com/feijimiao/xianyuassistant/controller/`
   - 功能: 提供HTTP API接口控制WebSocket
   - Python中无对应（Python是命令行工具）

### 接口定义

4. **WebSocketService.java** - WebSocket服务接口
   - 位置: `src/main/java/com/feijimiao/xianyuassistant/service/`
   - 功能: 定义WebSocket服务的接口规范

## 方法名称映射

### Python → Java 方法映射

```
Python方法                              Java方法
─────────────────────────────────────────────────────────────
main()                          →       startWebSocket()
handle_message()                →       onMessage()
_handle_message_with_semaphore() →      (建议添加) handleMessageWithLimit()
handle_heartbeat_response()     →       onMessage() 中的心跳处理逻辑
_create_websocket_connection()  →       startWebSocket() + XianyuWebSocketClient构造
WebSocketClient.connect()       →       connectBlocking()
WebSocketClient.receive()       →       onMessage() (回调)
WebSocketClient.send()          →       send()
WebSocketClient.run()           →       自动运行（框架处理）
```

## 建议的改进点

### 1. 添加消息处理限流机制

参考Python的 `_handle_message_with_semaphore()`，建议在Java中添加：

```java
// 在 XianyuWebSocketClient 中添加
private final Semaphore messageSemaphore = new Semaphore(5); // 限制并发处理数

private void handleMessageWithLimit(String message) {
    try {
        messageSemaphore.acquire();
        handleMessage(message);
    } catch (InterruptedException e) {
        log.error("消息处理被中断", e);
    } finally {
        messageSemaphore.release();
    }
}
```

### 2. 分离消息处理逻辑

建议创建独立的消息处理器：

```java
// 新建 WebSocketMessageHandler.java
public interface WebSocketMessageHandler {
    void handleMessage(String accountId, Map<String, Object> message);
    void handleHeartbeat(String accountId);
    void handleError(String accountId, Exception e);
}
```

### 3. 添加消息类型枚举

```java
// 新建 WebSocketMessageType.java
public enum WebSocketMessageType {
    HEARTBEAT("pong"),
    CHAT_MESSAGE("chat"),
    SYSTEM_MESSAGE("system"),
    UNKNOWN("unknown");
    
    private final String type;
    // ...
}
```

### 4. 增强错误处理和重连机制

参考Python的重连逻辑，添加自动重连：

```java
// 在 WebSocketServiceImpl 中添加
private void reconnectWebSocket(Long accountId) {
    int maxRetries = 3;
    int retryCount = 0;
    
    while (retryCount < maxRetries) {
        try {
            Thread.sleep(5000); // 等待5秒
            if (startWebSocket(accountId)) {
                log.info("重连成功: accountId={}", accountId);
                return;
            }
        } catch (Exception e) {
            log.error("重连失败: accountId={}, 尝试次数={}", accountId, retryCount + 1);
        }
        retryCount++;
    }
}
```

## 使用示例

### 启动WebSocket连接

```bash
curl -X POST http://localhost:8080/api/websocket/start \
  -H "Content-Type: application/json" \
  -d '{"xianyuAccountId": 1}'
```

### 停止WebSocket连接

```bash
curl -X POST http://localhost:8080/api/websocket/stop \
  -H "Content-Type: application/json" \
  -d '{"xianyuAccountId": 1}'
```

### 查询连接状态

```bash
curl -X POST http://localhost:8080/api/websocket/status \
  -H "Content-Type: application/json" \
  -d '{"xianyuAccountId": 1}'
```

## 总结

当前Java实现已经具备了Python版本的核心功能：
- ✅ WebSocket连接管理
- ✅ 消息接收和处理
- ✅ 心跳机制
- ✅ 多账号支持

建议添加的功能：
- ⚠️ 消息处理限流（信号量机制）
- ⚠️ 自动重连机制
- ⚠️ 消息类型分类处理
- ⚠️ 更详细的消息处理器接口

Java实现的优势：
- 🎯 提供了HTTP API接口，更易于集成
- 🎯 使用Spring框架，便于依赖注入和管理
- 🎯 更好的类型安全和编译时检查
