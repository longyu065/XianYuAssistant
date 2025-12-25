# Token保活机制说明

## 概述

闲鱼助手需要维护两种Token来保持功能正常运行：
1. **`_m_h5_tk`** - HTTP API签名Token
2. **`websocket_token`** - WebSocket连接认证Token

## Token特性对比

| Token类型 | 用途 | 有效期 | 获取方式 | 刷新方式 |
|----------|------|--------|---------|---------|
| `_m_h5_tk` | HTTP API签名 | ~2-4小时 | 扫码登录/调用API | 调用任意闲鱼API |
| `websocket_token` | WebSocket认证 | ~20小时 | 专用Token API | 重新调用Token API |
| Cookie | 整体认证 | 取决于最短Token | 扫码登录 | 刷新各个Token |

## 保活策略

### 1. `_m_h5_tk` Token保活（随机时间刷新）

**问题：**
- Token有效期较短（2-4小时）
- 过期后HTTP API调用失败
- WebSocket可能仍然连接，但无法调用HTTP API（如确认发货）

**解决方案：**
```java
// 定时任务：基础间隔1.5小时，随机延迟0-60分钟
// 实际刷新间隔：1.5-2.5小时（90-150分钟）
@Scheduled(fixedDelay = 90 * 60 * 1000)
public void scheduledRefreshMh5tk() {
    // 随机延迟0-60分钟
    int randomDelayMinutes = new Random().nextInt(61);
    Thread.sleep(randomDelayMinutes * 60 * 1000L);
    
    // 刷新所有账号
    refreshAllAccountsTokens();
}
```

**随机化策略：**
- ✅ 基础间隔：90分钟（1.5小时）
- ✅ 随机延迟：0-60分钟
- ✅ 实际间隔：90-150分钟（1.5-2.5小时）
- ✅ 账号间隔：2-5秒随机
- ✅ 避免固定时间被检测

**刷新机制：**
1. 调用任意闲鱼API（如首页数据接口）
2. 从响应的`Set-Cookie`头中提取新的`_m_h5_tk`
3. 更新到数据库的`cookie_text`和`m_h5_tk`字段

### 2. `websocket_token` Token保活（随机时间刷新）

**问题：**
- Token有效期较长（~20小时）
- 过期后WebSocket连接失败
- 需要重新获取

**解决方案：**
```java
// 定时任务：基础间隔10小时，随机延迟0-4小时
// 实际刷新间隔：10-14小时（600-840分钟）
@Scheduled(fixedDelay = 10 * 60 * 60 * 1000)
public void scheduledRefreshWebSocketToken() {
    // 随机延迟0-4小时（0-240分钟）
    int randomDelayMinutes = new Random().nextInt(241);
    Thread.sleep(randomDelayMinutes * 60 * 1000L);
    
    // 检查并刷新
    if (needsRefresh(accountId)) {
        refreshWebSocketToken(accountId);
    }
}
```

**随机化策略：**
- ✅ 基础间隔：10小时
- ✅ 随机延迟：0-4小时（0-240分钟）
- ✅ 实际间隔：10-14小时
- ✅ 账号间隔：2-5秒随机
- ✅ 提前1小时刷新（距离过期<1小时时触发）

**刷新机制：**
1. 检查`token_expire_time`字段
2. 如果距离过期时间<1小时，调用Token API重新获取
3. 更新到数据库的`websocket_token`和`token_expire_time`字段

### 3. Cookie整体保活

**策略：**
- 定期刷新`_m_h5_tk`（1.5-2.5小时随机）
- 定期检查`websocket_token`（10-14小时随机）
- 监控账号状态，自动处理异常
- 随机化避免被检测为机器人

## 实现细节

### TokenRefreshService

**核心功能：**
```java
public interface TokenRefreshService {
    // 刷新_m_h5_tk token
    boolean refreshMh5tkToken(Long accountId);
    
    // 刷新WebSocket token
    boolean refreshWebSocketToken(Long accountId);
    
    // 检查是否需要刷新
    boolean needsRefresh(Long accountId);
    
    // 刷新所有账号
    void refreshAllAccountsTokens();
}
```

**定时任务：**
1. **1.5-2.5小时随机** - 刷新所有账号的`_m_h5_tk`（基础90分钟+随机0-60分钟）
2. **10-14小时随机** - 检查并刷新`websocket_token`（基础10小时+随机0-4小时）
3. **账号间隔随机** - 每个账号刷新间隔2-5秒随机，避免被检测

### 数据库字段

**xianyu_cookie表：**
```sql
- cookie_text: 完整Cookie字符串（包含_m_h5_tk）
- m_h5_tk: _m_h5_tk token值
- websocket_token: WebSocket认证token
- token_expire_time: Token过期时间戳（毫秒）
- cookie_status: Cookie状态（1-有效，2-过期，3-失效）
```

## 使用建议

### 1. 启动时检查
```java
@PostConstruct
public void init() {
    // 启动时检查所有账号的token状态
    // 标记过期的账号
}
```

### 2. 调用前检查（已实现）
```java
// OrderServiceImpl.confirmShipment() 方法中
public String confirmShipment(Long accountId, String orderId) {
    // 在调用API前先刷新_m_h5_tk token
    tokenRefreshService.refreshMh5tkToken(accountId);
    // 然后调用API
}
```

### 3. 失败时处理
```java
public void onApiCallFailed(Long accountId, String error) {
    if (error.contains("TOKEN_EXPIRED")) {
        // 立即刷新token
        refreshMh5tkToken(accountId);
        // 重试API调用
    }
}
```

### 4. 手动刷新（已实现）
- **前端页面**：连接管理页面 → 选择账号 → 点击"🔄 刷新Token"按钮
- **后端接口**：`POST /api/websocket/refreshToken`
- **使用场景**：当遇到Token过期错误时，可以立即手动刷新

## 与Python版本的对比

### Python实现（参考）
```python
# 定期刷新token
async def keep_alive():
    while True:
        await asyncio.sleep(2 * 60 * 60)  # 每2小时
        await refresh_all_tokens()

# 刷新单个token
async def refresh_token(account_id):
    # 调用API获取新token
    # 更新到数据库
    pass
```

### Java实现（当前）
```java
// 使用Spring的@Scheduled注解
@Scheduled(fixedRate = 2 * 60 * 60 * 1000)
public void scheduledRefreshMh5tk() {
    refreshAllAccountsTokens();
}
```

## 监控和日志

### 关键日志
```
【账号5】开始刷新_m_h5_tk token...
【账号5】✅ _m_h5_tk token刷新成功: abc123...
【账号5】⚠️ Token即将过期，需要刷新
【账号5】❌ Token刷新失败，请检查Cookie
```

### 监控指标
- Token刷新成功率
- Token过期次数
- API调用失败率（因Token过期）
- 刷新时间分布（验证随机性）

## 故障排查

### 问题1：WebSocket能连接，但确认发货失败
**原因：** `_m_h5_tk`过期，但`websocket_token`还有效

**解决：**
1. **立即解决**：前端连接管理页面 → 点击"🔄 刷新Token"按钮
2. 或调用API：`POST /api/websocket/refreshToken`，参数：`{"xianyuAccountId": 5}`
3. 或重新扫码登录

**已实现的自动修复：**
- `OrderServiceImpl.confirmShipment()`方法会在调用API前自动刷新token
- 定时任务每2小时自动刷新所有账号的`_m_h5_tk`

### 问题2：WebSocket连接失败
**原因：** `websocket_token`过期

**解决：**
1. **立即解决**：前端连接管理页面 → 点击"🔄 刷新Token"按钮
2. 或调用API：`POST /api/websocket/refreshToken`
3. 或重新扫码登录

**已实现的自动修复：**
- 定时任务每12小时检查并刷新`websocket_token`

### 问题3：所有功能都失败
**原因：** Cookie整体过期

**解决：**
1. 重新扫码登录
2. 获取新的Cookie和所有Token

## 最佳实践

1. ✅ 启用定时任务自动刷新（随机时间）
2. ✅ 监控Token过期时间
3. ✅ API调用前检查Token状态
4. ✅ 失败时自动重试（刷新后）
5. ✅ 记录详细日志便于排查
6. ✅ 随机化刷新时间避免被检测
7. ❌ 不要频繁刷新（避免被限流）
8. ❌ 不要忽略过期警告
9. ❌ 不要使用固定时间刷新

## 配置说明

### application.yml
```yaml
spring:
  task:
    scheduling:
      pool:
        size: 5  # 定时任务线程池大小
```

### 自定义刷新间隔
```java
// 修改@Scheduled注解的fixedDelay参数和随机延迟范围

// _m_h5_tk刷新：基础间隔90分钟，随机延迟0-60分钟
@Scheduled(fixedDelay = 90 * 60 * 1000)  // 基础间隔
int randomDelayMinutes = new Random().nextInt(61);  // 随机0-60分钟

// WebSocket token刷新：基础间隔10小时，随机延迟0-4小时
@Scheduled(fixedDelay = 10 * 60 * 60 * 1000)  // 基础间隔
int randomDelayMinutes = new Random().nextInt(241);  // 随机0-240分钟
```

## 总结

Token保活是保证闲鱼助手稳定运行的关键。通过随机时间刷新机制，可以：
- ✅ 避免Token过期导致的功能失败
- ✅ 减少用户重新登录的频率
- ✅ 提高系统稳定性和用户体验
- ✅ 避免固定时间刷新被检测为机器人

**核心原则：** 预防性刷新 > 失败后刷新 > 重新登录

**随机化策略：**
- `_m_h5_tk`：1.5-2.5小时随机刷新
- `websocket_token`：10-14小时随机刷新
- 账号间隔：2-5秒随机
- 模拟真实用户行为，降低被检测风险
