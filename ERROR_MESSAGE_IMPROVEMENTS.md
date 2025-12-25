# WebSocket 启动接口错误提示改进

## 改进说明

`/api/websocket/start` 接口现在能够准确识别并提示以下错误情况：

## 错误类型及提示信息

### 1. Cookie 未找到
**错误提示：**
```
WebSocket连接启动失败：未找到账号Cookie，请先配置Cookie
```

**触发条件：**
- 数据库中没有该账号的 Cookie 记录
- Cookie 文本为空

### 2. Cookie 已过期
**错误提示：**
```
WebSocket连接启动失败：Cookie已过期，请更新Cookie后重试
```

**触发条件：**
- Cookie 状态标记为 2（过期）
- Cookie 中缺少 unb 字段
- Token API 返回 `FAIL_SYS_SESSION_EXPIRED` 或包含"令牌过期"

### 3. Cookie 已失效
**错误提示：**
```
WebSocket连接启动失败：Cookie已失效，请重新获取Cookie
```

**触发条件：**
- Cookie 状态标记为 3（失效）
- Token API 返回 `FAIL_SYS_ILLEGAL_ACCESS` 或包含"非法访问"

### 4. WebSocket Token 已过期
**错误提示：**
```
WebSocket连接启动失败：WebSocket Token已过期，系统将自动刷新Token，请稍后重试
```

**触发条件：**
- 数据库中存在 Token 但已超过有效期（20小时）

### 5. WebSocket Token 无效
**错误提示：**
```
WebSocket连接启动失败：WebSocket Token无效或连接被拒绝，请尝试更新Cookie或稍后重试
```

**触发条件：**
- Token 存在且未过期，但 WebSocket 连接失败
- Token 格式错误或被服务器拒绝

### 6. 无法获取 WebSocket Token
**错误提示：**
```
WebSocket连接启动失败：无法获取WebSocket Token，请检查Cookie是否有效
```

**触发条件：**
- Token API 调用失败
- Token API 返回空值
- 网络请求异常

### 7. 需要滑块验证
**错误提示：**
```json
{
  "code": 500,
  "message": "需要滑块验证",
  "data": {
    "needCaptcha": true,
    "captchaUrl": "https://...",
    "message": "需要完成滑块验证，请在浏览器中打开验证链接"
  }
}
```

**触发条件：**
- Token API 返回 `FAIL_SYS_USER_VALIDATE`
- 需要人工完成滑块验证

## 技术实现

### 新增异常类

1. **CookieNotFoundException** - Cookie 未找到异常
2. **CookieExpiredException** - Cookie 过期异常
3. **TokenInvalidException** - Token 无效异常

### 改进点

1. **精确的错误识别**
   - 在 `WebSocketServiceImpl` 中抛出具体的异常类型
   - 在 `WebSocketTokenServiceImpl` 中检查 Token API 响应并更新 Cookie 状态

2. **自动状态更新**
   - Cookie 过期时自动更新状态为 2
   - Cookie 失效时自动更新状态为 3
   - Token 获取成功时自动更新账号状态为 1（正常）

3. **详细的错误回退**
   - 如果异常未被捕获，Controller 会检查数据库状态提供详细错误信息
   - 通过 `getDetailedErrorMessage()` 方法分析 Cookie 和 Token 状态

## 使用示例

### 前端调用
```typescript
import { startConnection } from '@/api/websocket';

try {
  const response = await startConnection(accountId);
  if (response.code === 200) {
    console.log('WebSocket 连接成功');
  } else {
    // 显示具体的错误信息
    console.error(response.message);
    // 例如：
    // "WebSocket连接启动失败：Cookie已过期，请更新Cookie后重试"
    // "WebSocket连接启动失败：WebSocket Token无效或连接被拒绝，请尝试更新Cookie或稍后重试"
  }
} catch (error) {
  console.error('请求失败', error);
}
```

### 错误处理建议

根据不同的错误提示，前端可以采取不同的处理策略：

1. **Cookie 未找到/过期/失效** → 引导用户更新 Cookie
2. **Token 已过期** → 自动重试或提示用户稍后再试
3. **Token 无效** → 建议用户更新 Cookie 或联系技术支持
4. **需要滑块验证** → 打开验证链接让用户完成验证

## 测试建议

1. 测试 Cookie 不存在的情况
2. 测试 Cookie 过期的情况（手动设置 cookieStatus = 2）
3. 测试 Token 过期的情况（手动设置 tokenExpireTime 为过去的时间）
4. 测试滑块验证触发的情况
5. 测试网络异常的情况
