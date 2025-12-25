# Token过期问题解决方案

## 问题描述

错误信息：`FAIL_SYS_TOKEN_EXOIRED::令牌过期`

**原因分析：**
- WebSocket能连接（`websocket_token`有效）
- 但确认发货失败（`_m_h5_tk`过期）
- 这两个token有不同的有效期和用途

## 已实现的解决方案

### 1. 自动刷新机制（随机时间定时任务）

**后端自动执行：**
- ✅ 1.5-2.5小时随机刷新所有账号的`_m_h5_tk` token
  - 基础间隔：90分钟
  - 随机延迟：0-60分钟
  - 实际间隔：90-150分钟
- ✅ 10-14小时随机检查并刷新`websocket_token`
  - 基础间隔：10小时
  - 随机延迟：0-4小时
  - 实际间隔：10-14小时
- ✅ 账号间隔：2-5秒随机，避免频繁请求被检测
- ✅ 启动延迟：`_m_h5_tk`延迟10分钟，`websocket_token`延迟30分钟

**随机化优势：**
- 避免固定时间刷新被检测为机器人
- 模拟真实用户行为
- 降低被限流或封号的风险

**实现文件：**
- `TokenRefreshServiceImpl.java` - 定时任务和刷新逻辑

### 2. API调用前自动刷新

**确认发货前自动刷新：**
```java
// OrderServiceImpl.confirmShipment()
public String confirmShipment(Long accountId, String orderId) {
    // 在调用API前先刷新_m_h5_tk token
    tokenRefreshService.refreshMh5tkToken(accountId);
    // 然后调用确认发货API
}
```

**实现文件：**
- `OrderServiceImpl.java` - 确认发货服务

### 3. 手动刷新功能

**前端操作：**
1. 打开"连接管理"页面
2. 选择需要刷新的账号
3. 点击"🔄 刷新Token"按钮
4. 等待刷新完成，查看日志

**后端接口：**
```
POST /api/websocket/refreshToken
Body: {"xianyuAccountId": 5}

Response: {
  "code": 0,
  "data": {
    "mh5tkRefreshed": true,
    "wsTokenRefreshed": true,
    "message": "✅ 所有Token刷新成功"
  }
}
```

**实现文件：**
- `WebSocketController.java` - 刷新Token接口
- `vue-code/src/views/connection/index.vue` - 前端页面
- `vue-code/src/api/websocket.ts` - API调用

## 立即解决当前问题

### 方法1：前端手动刷新（推荐）
1. 打开浏览器访问：http://localhost:8080
2. 进入"连接管理"页面
3. 选择账号5
4. 点击"🔄 刷新Token"按钮

### 方法2：API调用
```bash
curl -X POST http://localhost:8080/api/websocket/refreshToken \
  -H "Content-Type: application/json" \
  -d '{"xianyuAccountId": 5}'
```

### 方法3：重新扫码登录
- 重新扫码会获取全新的Cookie和所有Token

## 监控和日志

### 查看定时任务日志
```
🔄 _m_h5_tk token刷新任务启动，随机延迟35分钟后执行...
🔄 开始刷新所有账号的_m_h5_tk token...
【账号5】开始刷新_m_h5_tk token...
【账号5】✅ _m_h5_tk token刷新成功: abc123...
✅ _m_h5_tk token刷新完成: 成功1个, 失败0个

🔄 WebSocket token检查任务启动，随机延迟127分钟后执行...
🔄 开始定时检查并刷新WebSocket token...
【账号5】WebSocket token即将过期，需要刷新
【账号5】✅ WebSocket token刷新成功
✅ WebSocket token检查完成
```

### 查看API调用日志
```
【账号5】调用API前刷新_m_h5_tk token...
【账号5】✅ _m_h5_tk token刷新成功
【账号5】开始确认发货: orderId=4963939777935533226
【账号5】✅ 确认发货成功
```

## Token类型对比

| Token类型 | 用途 | 有效期 | 刷新方式 |
|----------|------|--------|---------|
| `_m_h5_tk` | HTTP API签名 | ~2-4小时 | 调用闲鱼API |
| `websocket_token` | WebSocket认证 | ~20小时 | 调用Token API |

## 相关文档

- `TOKEN_KEEPALIVE.md` - 完整的Token保活机制说明
- `TokenRefreshServiceImpl.java` - 定时任务实现
- `WebSocketTokenServiceImpl.java` - Token获取和刷新
- `OrderServiceImpl.java` - API调用前刷新

## 总结

✅ **已实现的功能：**
1. 随机时间定时刷新（1.5-2.5小时刷新`_m_h5_tk`，10-14小时刷新`websocket_token`）
2. API调用前自动刷新（确认发货前自动刷新`_m_h5_tk`）
3. 手动刷新按钮（前端页面和后端接口）
4. 账号间隔随机化（2-5秒随机，避免被检测）

✅ **解决的问题：**
- WebSocket能连接但确认发货失败 → 自动刷新`_m_h5_tk`
- Token过期导致功能失败 → 随机时间定时任务预防性刷新
- 需要立即刷新 → 手动刷新按钮
- 固定时间刷新被检测 → 随机化刷新时间和间隔

🎯 **建议：**
- 保持定时任务运行（已自动随机化）
- 遇到Token过期错误时使用手动刷新
- 定期检查日志确认刷新任务正常运行
- 观察刷新时间分布，验证随机性
