# WebSocket接口DTO规范修复说明

## 问题描述

在 `WebSocketController` 中，多个接口共用了同一个 `WebSocketReqDTO` 对象，违反了项目开发规范。

根据 `.qoder/rules/quanju.md` 中的规范要求：
> **所有接口不能共用DTO对象**

## 修复内容

### 修复前
原来有4个接口共用 `WebSocketReqDTO`：
- `/start` - 启动WebSocket连接
- `/stop` - 停止WebSocket连接
- `/status` - 查询WebSocket状态
- `/clearCaptchaWait` - 清除验证等待状态

### 修复后
为每个接口创建了独立的请求DTO：

1. **启动WebSocket连接** (`/start`)
   - 请求DTO: `StartWebSocketReqDTO`
   - 字段: `xianyuAccountId`, `accessToken`

2. **停止WebSocket连接** (`/stop`)
   - 请求DTO: `StopWebSocketReqDTO`
   - 字段: `xianyuAccountId`

3. **查询WebSocket状态** (`/status`)
   - 请求DTO: `GetWebSocketStatusReqDTO`
   - 字段: `xianyuAccountId`

4. **清除验证等待状态** (`/clearCaptchaWait`)
   - 请求DTO: `ClearCaptchaWaitReqDTO`
   - 字段: `xianyuAccountId`

### 响应DTO
响应DTO保持不变，符合规范：
- `WebSocketStatusRespDTO` - 状态查询响应
- `CaptchaInfoDTO` - 滑块验证信息响应

## 代码改进

1. **符合规范**: 每个接口都有独立的请求DTO，不再共用
2. **清晰明确**: DTO命名清楚表明了所属接口
3. **易于维护**: 每个接口的参数变更不会影响其他接口
4. **详细注释**: 为所有DTO字段添加了详细的中文注释

## 命名规范

遵循项目规范：
- 请求DTO: `{操作名称}ReqDTO`
- 响应DTO: `{操作名称}RespDTO`

例如：
- `StartWebSocketReqDTO` - 启动WebSocket请求
- `WebSocketStatusRespDTO` - WebSocket状态响应

## 影响范围

此次修改仅涉及后端 Controller 层的 DTO 定义，不影响：
- Service 层逻辑
- 前端接口调用（接口路径和参数结构未变）
- 数据库操作

## 验证

已通过 `getDiagnostics` 工具验证，代码无语法错误。
