# WebSocket消息监听功能说明

## 功能概述

实现了闲鱼WebSocket消息监听功能，可以实时接收闲鱼平台的消息推送。

## 功能特点

1. **实时消息监听**：连接闲鱼WebSocket服务器，实时接收消息
2. **自动心跳**：每30秒自动发送心跳，保持连接活跃
3. **消息打印**：收到的消息会打印到控制台日志
4. **连接管理**：支持启动、停止、查询状态
5. **多账号支持**：可以同时监听多个账号的消息

## 技术实现

### 1. 核心类

**XianyuWebSocketClient.java**
- 继承 `WebSocketClient`
- 实现消息接收和处理
- 支持心跳机制

**WebSocketServiceImpl.java**
- 管理WebSocket连接
- 处理连接的启动和停止
- 管理心跳任务

**WebSocketController.java**
- 提供HTTP接口控制WebSocket
- 启动、停止、查询状态

### 2. 依赖库

使用 `Java-WebSocket` 库：
```xml
<dependency>
    <groupId>org.java-websocket</groupId>
    <artifactId>Java-WebSocket</artifactId>
    <version>1.5.4</version>
</dependency>
```

## API接口

### 1. 启动WebSocket监听

**接口地址：** `POST /api/websocket/start`

**请求参数：**
```json
{
  "xianyuAccountId": 1
}
```

**响应示例：**
```json
{
  "code": 200,
  "msg": "操作成功",
  "data": "WebSocket连接已启动"
}
```

### 2. 停止WebSocket监听

**接口地址：** `POST /api/websocket/stop`

**请求参数：**
```json
{
  "xianyuAccountId": 1
}
```

**响应示例：**
```json
{
  "code": 200,
  "msg": "操作成功",
  "data": "WebSocket连接已停止"
}
```

### 3. 查询WebSocket状态

**接口地址：** `POST /api/websocket/status`

**请求参数：**
```json
{
  "xianyuAccountId": 1
}
```

**响应示例：**
```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "xianyuAccountId": 1,
    "connected": true,
    "status": "已连接"
  }
}
```

## 使用方法

### 方法1：通过Web界面

1. 访问：`http://localhost:8080/websocket.html`
2. 输入账号ID
3. 点击"启动监听"
4. 查看控制台日志中的消息

### 方法2：通过API调用

```bash
# 启动监听
curl -X POST "http://localhost:8080/api/websocket/start" \
  -H "Content-Type: application/json" \
  -d '{"xianyuAccountId":1}'

# 停止监听
curl -X POST "http://localhost:8080/api/websocket/stop" \
  -H "Content-Type: application/json" \
  -d '{"xianyuAccountId":1}'

# 查询状态
curl -X POST "http://localhost:8080/api/websocket/status" \
  -H "Content-Type: application/json" \
  -d '{"xianyuAccountId":1}'
```

## 日志示例

### 连接成功日志

```
2024-11-12 16:00:00.123 [main] INFO  WebSocketServiceImpl - 启动WebSocket连接: accountId=1
2024-11-12 16:00:00.500 [WebSocketConnectReadThread-1] INFO  XianyuWebSocketClient - 【账号1】WebSocket连接建立成功
2024-11-12 16:00:00.501 [WebSocketConnectReadThread-1] INFO  XianyuWebSocketClient - 【账号1】服务器握手状态: 101
2024-11-12 16:00:00.502 [main] INFO  WebSocketServiceImpl - WebSocket连接成功: accountId=1
2024-11-12 16:00:00.503 [main] INFO  WebSocketServiceImpl - 心跳任务已启动: accountId=1
```

### 收到消息日志

```
2024-11-12 16:00:30.123 [WebSocketWorker-1] INFO  XianyuWebSocketClient - 【账号1】收到WebSocket消息: 256 字节
2024-11-12 16:00:30.124 [WebSocketWorker-1] INFO  XianyuWebSocketClient - 【账号1】原始消息内容: {"type":"message","data":{"content":"你好"}}
2024-11-12 16:00:30.125 [WebSocketWorker-1] INFO  XianyuWebSocketClient - 【账号1】解析后的消息: {type=message, data={content=你好}}
2024-11-12 16:00:30.126 [WebSocketWorker-1] INFO  XianyuWebSocketClient - 【账号1】消息类型: message
```

### 心跳日志

```
2024-11-12 16:01:00.123 [pool-1-thread-1] DEBUG XianyuWebSocketClient - 【账号1】发送心跳消息
```

### 连接关闭日志

```
2024-11-12 16:05:00.123 [main] INFO  WebSocketServiceImpl - 停止WebSocket连接: accountId=1
2024-11-12 16:05:00.124 [main] INFO  WebSocketServiceImpl - 心跳任务已停止: accountId=1
2024-11-12 16:05:00.200 [WebSocketWorker-1] INFO  XianyuWebSocketClient - 【账号1】WebSocket连接关闭 - 关闭方: 客户端, 代码: 1000, 原因: 
2024-11-12 16:05:00.201 [main] INFO  WebSocketServiceImpl - WebSocket连接已关闭: accountId=1
```

## 消息格式

### 心跳消息

**发送：**
```json
{
  "action": "ping"
}
```

**接收：**
```json
{
  "action": "pong"
}
```

### 业务消息

根据闲鱼平台的实际消息格式，可能包含：
- 聊天消息
- 订单通知
- 系统消息
- 等等

## 注意事项

1. **Cookie有效性**
   - 必须使用有效的Cookie才能连接
   - Cookie过期会导致连接失败

2. **WebSocket URL**
   - 当前使用：`wss://wss-goofish.dingtalk.com/`
   - 来源：Python代码配置文件
   - 如果URL变更，需要修改代码

3. **心跳机制**
   - 每30秒自动发送心跳
   - 保持连接活跃，防止超时断开

4. **资源管理**
   - 应用关闭时会自动清理所有连接
   - 建议不用时手动停止连接

5. **并发限制**
   - 可以同时监听多个账号
   - 每个账号独立管理连接和心跳

## 故障排查

### 问题1：连接失败

**可能原因：**
- Cookie无效或过期
- 网络问题
- WebSocket URL错误

**解决方法：**
1. 检查Cookie是否有效
2. 重新登录获取新Cookie
3. 检查网络连接

### 问题2：收不到消息

**可能原因：**
- 连接已断开
- 心跳失败
- 服务器未推送消息

**解决方法：**
1. 查询连接状态
2. 重新启动连接
3. 检查日志中的错误信息

### 问题3：连接频繁断开

**可能原因：**
- Cookie过期
- 心跳失败
- 服务器主动断开

**解决方法：**
1. 更新Cookie
2. 检查心跳日志
3. 查看服务器返回的关闭原因

## 未来扩展

1. **消息处理**
   - 解析不同类型的消息
   - 自动回复功能
   - 消息存储

2. **重连机制**
   - 自动重连
   - 指数退避策略
   - 连接状态通知

3. **消息过滤**
   - 按类型过滤消息
   - 关键词过滤
   - 自定义过滤规则

4. **统计分析**
   - 消息数量统计
   - 连接时长统计
   - 错误率统计

## 文件清单

### 后端文件

1. **src/main/java/com/feijimiao/xianyuassistant/websocket/XianyuWebSocketClient.java**
   - WebSocket客户端实现

2. **src/main/java/com/feijimiao/xianyuassistant/service/WebSocketService.java**
   - WebSocket服务接口

3. **src/main/java/com/feijimiao/xianyuassistant/service/impl/WebSocketServiceImpl.java**
   - WebSocket服务实现

4. **src/main/java/com/feijimiao/xianyuassistant/controller/WebSocketController.java**
   - WebSocket控制器

### 前端文件

1. **src/main/resources/static/websocket.html**
   - WebSocket测试页面

### 配置文件

1. **pom.xml**
   - 添加Java-WebSocket依赖

## 总结

WebSocket消息监听功能已经实现，可以：
- ✅ 连接闲鱼WebSocket服务器
- ✅ 实时接收消息
- ✅ 自动发送心跳
- ✅ 打印消息到控制台
- ✅ 管理连接状态

目前实现的是基础的消息监听功能，收到的消息会打印到控制台日志中。后续可以根据需要扩展消息处理、自动回复等功能。

## 更新时间

2024-11-12
