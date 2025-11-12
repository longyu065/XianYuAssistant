# 手动输入AccessToken功能说明

## 功能概述

为了解决滑块验证问题，系统现在支持手动输入 `accessToken` 来启动 WebSocket 连接，绕过自动获取 Token 时可能遇到的滑块验证。

## 使用方法

### 方式一：自动获取（默认）

1. 在前端页面输入账号ID
2. 留空 AccessToken 输入框
3. 点击"启动监听"
4. 系统自动获取 Token（可能遇到滑块验证）

### 方式二：手动输入（推荐）

1. 在前端页面输入账号ID
2. 在 AccessToken 输入框中输入手动获取的 Token
3. 点击"启动监听"
4. 系统使用提供的 Token 直接连接

## 如何获取AccessToken

### 方法1：通过浏览器开发者工具

1. 打开浏览器（Chrome/Edge）
2. 按 F12 打开开发者工具
3. 切换到 Network（网络）标签
4. 访问 https://www.goofish.com 并登录
5. 在 Network 中搜索 `mtop.taobao.idlemessage.pc.login.token`
6. 点击该请求，查看 Response（响应）
7. 找到 `data.accessToken` 字段
8. 复制该值

### 方法2：通过Console直接获取

在浏览器控制台执行以下代码：

```javascript
// 1. 获取Cookie
const cookies = document.cookie;

// 2. 解析_m_h5_tk
const mh5tk = cookies.match(/_m_h5_tk=([^;]+)/)?.[1];
const token = mh5tk?.split('_')[0] || '';

// 3. 生成设备ID
const unb = cookies.match(/unb=([^;]+)/)?.[1];
const deviceId = `web_${unb}`;

// 4. 构建请求
const timestamp = Date.now().toString();
const dataVal = JSON.stringify({
    appKey: "444e9908a51d1cb236a27862abc769c9",
    deviceId: deviceId
});

// 5. 生成签名（需要md5库）
// 注意：这里需要引入md5库，或者直接在后端获取

// 6. 发送请求获取token
fetch('https://h5api.m.goofish.com/h5/mtop.taobao.idlemessage.pc.login.token/1.0/?...')
    .then(res => res.json())
    .then(data => {
        console.log('AccessToken:', data.data.accessToken);
    });
```

### 方法3：使用Postman/Curl

使用 Postman 或 curl 工具，参考后端的请求逻辑发送请求获取 Token。

## API接口

### 启动WebSocket（支持手动Token）

**接口地址：** `POST /api/websocket/start`

**请求参数：**

```json
{
    "xianyuAccountId": 1,
    "accessToken": "可选，手动提供的accessToken"
}
```

**响应示例：**

成功：
```json
{
    "code": 200,
    "msg": "WebSocket连接已启动",
    "data": null
}
```

失败：
```json
{
    "code": 500,
    "msg": "WebSocket连接启动失败",
    "data": null
}
```

滑块验证（仅自动获取时）：
```json
{
    "code": 500,
    "msg": "需要滑块验证",
    "data": {
        "needCaptcha": true,
        "captchaUrl": "https://...",
        "message": "需要完成滑块验证，请在浏览器中打开验证链接"
    }
}
```

## 实现细节

### 后端实现

#### 1. Controller层

```java
@PostMapping("/start")
public ResultObject<CaptchaInfoDTO> startWebSocket(@RequestBody WebSocketReqDTO reqDTO) {
    if (reqDTO.getAccessToken() != null && !reqDTO.getAccessToken().isEmpty()) {
        // 使用手动提供的 accessToken
        success = webSocketService.startWebSocketWithToken(
                reqDTO.getXianyuAccountId(), 
                reqDTO.getAccessToken()
        );
    } else {
        // 自动获取 accessToken
        success = webSocketService.startWebSocket(reqDTO.getXianyuAccountId());
    }
}
```

#### 2. Service层

新增方法：
```java
boolean startWebSocketWithToken(Long accountId, String accessToken);
```

实现逻辑：
- 跳过自动获取 Token 的步骤
- 直接使用提供的 Token 连接 WebSocket
- 其他流程与自动获取相同

#### 3. 通用连接方法

提取了 `connectWebSocket` 方法，统一处理 WebSocket 连接逻辑：
- 构建请求头
- 创建 WebSocket 客户端
- 执行初始化流程
- 启动心跳任务

### 前端实现

#### 1. 添加输入框

```html
<div class="form-group">
    <label for="accessToken">AccessToken (可选):</label>
    <input type="text" id="accessToken" placeholder="如遇滑块验证，可手动输入accessToken">
</div>
```

#### 2. 修改请求逻辑

```javascript
const requestBody = {
    xianyuAccountId: parseInt(accountId)
};

// 如果提供了 accessToken，则添加到请求中
if (accessToken) {
    requestBody.accessToken = accessToken;
}
```

## 优势

1. **绕过滑块验证**：手动输入 Token 可以避免频繁触发滑块验证
2. **灵活性高**：支持自动和手动两种方式
3. **用户友好**：提供详细的获取 Token 指引
4. **向后兼容**：不影响原有的自动获取功能

## 注意事项

1. **Token有效期**：AccessToken 通常有效期为 20 小时，过期后需要重新获取
2. **安全性**：不要将 Token 分享给他人，避免账号安全风险
3. **Cookie依赖**：即使手动输入 Token，仍需要有效的 Cookie
4. **设备ID**：系统会自动从 Cookie 中提取 unb 生成设备ID

## 测试步骤

1. 访问 http://localhost:8080/websocket.html
2. 输入账号ID（如：1）
3. 输入手动获取的 AccessToken
4. 点击"启动监听"
5. 查看后端日志，确认使用手动 Token
6. 验证 WebSocket 连接成功

## 相关文件

- `src/main/java/com/feijimiao/xianyuassistant/controller/WebSocketController.java` - 控制器（支持手动Token）
- `src/main/java/com/feijimiao/xianyuassistant/service/WebSocketService.java` - 服务接口（新增方法）
- `src/main/java/com/feijimiao/xianyuassistant/service/impl/WebSocketServiceImpl.java` - 服务实现（新增方法）
- `src/main/resources/static/websocket.html` - 前端页面（添加输入框）
