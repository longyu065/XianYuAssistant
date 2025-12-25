# 手动更新WebSocket Token功能

## 功能说明

在连接管理页面的WebSocket Token部分，新增了"✏️ 手动更新"按钮，支持手动填入WebSocket Token。

## 使用场景

1. **从其他来源获取Token**：如果你从其他渠道获取了有效的WebSocket Token
2. **测试Token**：测试特定Token是否有效
3. **紧急恢复**：在自动刷新失败时手动输入Token

## 使用方法

### 1. 打开手动更新对话框

1. 打开"连接管理"页面
2. 选择需要更新Token的账号
3. 在"WebSocket Token"部分，点击"✏️ 手动更新"按钮

### 2. 填入新Token

1. 在对话框中查看当前Token
2. 在"新Token"输入框中填入新的WebSocket Token
3. 点击"确定更新"按钮

### 3. 更新结果

更新成功后：
- ✅ 显示"Token更新成功"提示
- ✅ Token内容自动更新
- ✅ 过期时间自动更新（20小时后）
- ✅ 操作日志记录更新结果

## 与自动刷新的区别

### 🔄 刷新Token（推荐）
- 自动调用API获取新Token
- 无需手动输入
- Token格式保证正确
- 适合日常使用

### ✏️ 手动更新
- 需要手动输入Token
- 可能输入错误导致连接失败
- 适合特殊情况

## 技术实现

### 后端接口
```
POST /api/websocket/updateToken
Body: {
  "xianyuAccountId": 5,
  "websocketToken": "your_token_here"
}
```

### 前端组件
- `ManualUpdateTokenDialog.vue` - 手动更新对话框
- 调用`updateToken` API
- 更新成功后刷新页面状态

### 数据库更新
- 更新`xianyu_cookie.websocket_token`字段
- 更新`xianyu_cookie.token_expire_time`字段（当前时间+20小时）

## 注意事项

1. **Token格式**：确保输入的Token格式正确
2. **Token有效性**：错误的Token会导致WebSocket连接失败
3. **优先使用自动刷新**：建议优先使用"刷新Token"按钮
4. **验证结果**：更新后建议测试WebSocket连接是否正常

## 相关文档

- `CONNECTION_PAGE_TOKEN_UPDATE.md` - Token更新功能说明
- `TOKEN_KEEPALIVE.md` - Token保活机制
- `TOKEN_REFRESH_SOLUTION.md` - Token刷新解决方案
