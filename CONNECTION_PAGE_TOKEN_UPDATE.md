# 连接管理页面 - Token更新功能

## 功能说明

在连接管理页面的WebSocket Token部分，新增了"🔄 刷新Token"按钮，支持手动刷新WebSocket Token。

## 使用方法

### 1. 刷新WebSocket Token

1. 打开"连接管理"页面
2. 选择需要刷新Token的账号
3. 在"WebSocket Token"部分，点击"🔄 刷新Token"按钮
4. 等待刷新完成，查看操作日志

### 2. 刷新结果

刷新成功后：
- ✅ 显示"WebSocket Token刷新成功"提示
- ✅ Token内容自动更新
- ✅ 过期时间自动更新
- ✅ 操作日志记录刷新结果

刷新失败时：
- ❌ 显示失败原因
- ❌ 操作日志记录错误信息
- ⚠️ 可能需要重新扫码登录

## 与"刷新Token"按钮的区别

### 全局刷新Token按钮（主操作区）
- 位置：主操作区域
- 功能：同时刷新`_m_h5_tk`和`websocket_token`
- 适用场景：全面刷新所有Token

### WebSocket Token刷新按钮（Token详情区）
- 位置：WebSocket Token部分
- 功能：仅刷新`websocket_token`
- 适用场景：WebSocket Token即将过期或已过期

## 技术实现

### 前端
- 调用`/api/websocket/refreshToken`接口
- 传入账号ID
- 接收刷新结果并更新页面

### 后端
- `TokenRefreshService.refreshWebSocketToken()`方法
- 调用Token API重新获取Token
- 更新数据库中的Token和过期时间

## 注意事项

1. **刷新频率**：不要频繁刷新，建议间隔至少5分钟
2. **Token有效期**：WebSocket Token有效期约20小时
3. **自动刷新**：系统会在10-14小时随机刷新，通常无需手动刷新
4. **刷新失败**：如果刷新失败，可能是Cookie过期，需要重新扫码登录

## 相关文档

- `TOKEN_KEEPALIVE.md` - Token保活机制说明
- `TOKEN_REFRESH_SOLUTION.md` - Token刷新解决方案
- `RANDOM_REFRESH_STRATEGY.md` - 随机刷新策略
