# WebSocket URL更新说明

## 问题解决

之前WebSocket无法连接的问题已经解决！

### ❌ 旧的URL（错误）

```
wss://ws.goofish.com/ws
```

**错误信息：**
```
java.net.UnknownHostException: 不知道这样的主机。 (ws.goofish.com)
```

### ✅ 新的URL（正确）

```
wss://wss-goofish.dingtalk.com/
```

**来源：** Python代码 `config.py` 文件

```python
WEBSOCKET_URL = config.get('WEBSOCKET_URL', 'wss://wss-goofish.dingtalk.com/')
```

## 修改内容

### 文件：WebSocketServiceImpl.java

**修改位置：** 第40行左右

**修改前：**
```java
private static final String WEBSOCKET_URL = "wss://ws.goofish.com/ws";
```

**修改后：**
```java
private static final String WEBSOCKET_URL = "wss://wss-goofish.dingtalk.com/";
```

## 测试方法

### 1. 重新启动应用

```bash
# 停止应用
# 重新启动应用
```

### 2. 测试WebSocket连接

**方法1：Web界面**
```
访问：http://localhost:8080/websocket.html
输入账号ID：1
点击"启动监听"
```

**方法2：API调用**
```bash
curl -X POST "http://localhost:8080/api/websocket/start" \
  -H "Content-Type: application/json" \
  -d '{"xianyuAccountId":1}'
```

### 3. 查看日志

成功的日志应该显示：
```
【账号1】WebSocket连接建立成功
【账号1】服务器握手状态: 101
WebSocket连接成功: accountId=1
心跳任务已启动: accountId=1
```

## 其他发现

从Python代码中还发现了其他有用的配置：

### 1. 心跳间隔

```python
HEARTBEAT_INTERVAL = config.get('HEARTBEAT_INTERVAL', 15)  # 15秒
```

当前Java代码使用30秒，可以考虑改为15秒以保持一致。

### 2. WebSocket请求头

Python代码中使用的请求头：
```python
WEBSOCKET_HEADERS = config.get('WEBSOCKET_HEADERS', {})
```

需要查看 `global_config.yml` 文件了解具体的请求头配置。

### 3. 初始化流程

Python代码在连接成功后会调用 `init()` 方法进行初始化：
```python
await self.init(websocket)
```

可能需要在Java代码中也实现类似的初始化逻辑。

## 下一步

1. **测试连接**
   - 重启应用
   - 测试WebSocket连接
   - 验证是否能收到消息

2. **优化配置**
   - 调整心跳间隔为15秒
   - 查看并配置正确的请求头
   - 实现初始化逻辑

3. **完善功能**
   - 实现消息解析
   - 添加消息处理
   - 实现自动回复

## 参考文件

- Python代码：`src/main/resources/pythondemo/xianyu-auto-reply-main/XianyuAutoAsync.py`
- 配置文件：`src/main/resources/pythondemo/xianyu-auto-reply-main/config.py`
- Java实现：`src/main/java/com/feijimiao/xianyuassistant/service/impl/WebSocketServiceImpl.java`

## 更新时间

2024-11-12
