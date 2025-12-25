# 随机刷新策略说明

## 为什么需要随机刷新？

固定时间刷新Token容易被闲鱼服务器检测为机器人行为，可能导致：
- ❌ 账号被限流
- ❌ 触发更频繁的验证码
- ❌ 账号被标记为异常
- ❌ 功能受限或封号

## 随机化策略

### 1. `_m_h5_tk` Token刷新

**刷新间隔：**
- 基础间隔：90分钟（1.5小时）
- 随机延迟：0-60分钟
- **实际间隔：90-150分钟（1.5-2.5小时）**

**示例时间线：**
```
启动后10分钟  → 第1次刷新（延迟35分钟） → 125分钟后
第1次刷新后   → 第2次刷新（延迟18分钟） → 108分钟后
第2次刷新后   → 第3次刷新（延迟52分钟） → 142分钟后
...
```

**代码实现：**
```java
@Scheduled(fixedDelay = 90 * 60 * 1000, initialDelay = 10 * 60 * 1000)
public void scheduledRefreshMh5tk() {
    // 随机延迟0-60分钟
    int randomDelayMinutes = new Random().nextInt(61);
    Thread.sleep(randomDelayMinutes * 60 * 1000L);
    
    // 执行刷新
    refreshAllAccountsTokens();
}
```

### 2. `websocket_token` Token刷新

**刷新间隔：**
- 基础间隔：10小时（600分钟）
- 随机延迟：0-4小时（0-240分钟）
- **实际间隔：10-14小时（600-840分钟）**

**示例时间线：**
```
启动后30分钟  → 第1次检查（延迟127分钟） → 12.1小时后
第1次检查后   → 第2次检查（延迟45分钟）  → 10.75小时后
第2次检查后   → 第3次检查（延迟203分钟） → 13.4小时后
...
```

**代码实现：**
```java
@Scheduled(fixedDelay = 10 * 60 * 60 * 1000, initialDelay = 30 * 60 * 1000)
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

### 3. 账号间隔随机化

**多账号刷新时：**
- 固定间隔：2秒
- 随机延迟：0-3秒
- **实际间隔：2-5秒**

**代码实现：**
```java
for (XianyuAccount account : accounts) {
    refreshMh5tkToken(account.getId());
    
    // 随机间隔2-5秒
    int randomInterval = 2000 + new Random().nextInt(3001);
    Thread.sleep(randomInterval);
}
```

## 随机化效果

### 刷新时间分布示例（24小时）

**`_m_h5_tk` 刷新（预期10-16次）：**
```
00:10 → 02:15 → 04:38 → 06:21 → 08:47 → 10:19 → 12:54 → 14:26 → 16:58 → 18:33 → 20:47 → 22:15
```

**`websocket_token` 刷新（预期2-3次）：**
```
00:30 → 12:37 → 23:45
```

### 对比固定时间刷新

**固定时间（容易被检测）：**
```
_m_h5_tk:     00:00 → 02:00 → 04:00 → 06:00 → 08:00 → 10:00 → 12:00 → ...
websocket:    00:00 → 12:00 → 24:00
```

**随机时间（模拟真实用户）：**
```
_m_h5_tk:     00:10 → 02:15 → 04:38 → 06:21 → 08:47 → 10:19 → 12:54 → ...
websocket:    00:30 → 12:37 → 23:45
```

## 日志示例

### 启动日志
```
2025-12-24 10:00:00 [main] INFO  TokenRefreshServiceImpl - 应用启动完成
2025-12-24 10:10:00 [scheduler] INFO  TokenRefreshServiceImpl - 🔄 _m_h5_tk token刷新任务启动，随机延迟35分钟后执行...
2025-12-24 10:30:00 [scheduler] INFO  TokenRefreshServiceImpl - 🔄 WebSocket token检查任务启动，随机延迟127分钟后执行...
```

### 刷新日志
```
2025-12-24 10:45:00 [scheduler] INFO  TokenRefreshServiceImpl - 🔄 开始刷新所有账号的_m_h5_tk token...
2025-12-24 10:45:00 [scheduler] INFO  TokenRefreshServiceImpl - 【账号1】开始刷新_m_h5_tk token...
2025-12-24 10:45:01 [scheduler] INFO  TokenRefreshServiceImpl - 【账号1】✅ _m_h5_tk token刷新成功: abc123...
2025-12-24 10:45:04 [scheduler] INFO  TokenRefreshServiceImpl - 【账号2】开始刷新_m_h5_tk token...
2025-12-24 10:45:05 [scheduler] INFO  TokenRefreshServiceImpl - 【账号2】✅ _m_h5_tk token刷新成功: def456...
2025-12-24 10:45:05 [scheduler] INFO  TokenRefreshServiceImpl - ✅ _m_h5_tk token刷新完成: 成功2个, 失败0个
```

## 优势总结

### ✅ 安全性
- 避免固定时间模式被检测
- 模拟真实用户行为
- 降低被限流或封号风险

### ✅ 可靠性
- 刷新间隔在Token有效期内
- 提前刷新避免过期
- 失败自动重试

### ✅ 灵活性
- 可调整基础间隔
- 可调整随机范围
- 可针对不同账号定制策略

### ✅ 可观测性
- 详细的刷新日志
- 成功/失败统计
- 时间分布可追踪

## 配置建议

### 保守策略（更安全）
```java
// _m_h5_tk: 2-4小时随机
@Scheduled(fixedDelay = 120 * 60 * 1000)  // 基础2小时
int randomDelay = new Random().nextInt(121);  // 随机0-2小时

// websocket_token: 12-18小时随机
@Scheduled(fixedDelay = 12 * 60 * 60 * 1000)  // 基础12小时
int randomDelay = new Random().nextInt(361);  // 随机0-6小时
```

### 激进策略（更频繁）
```java
// _m_h5_tk: 1-2小时随机
@Scheduled(fixedDelay = 60 * 60 * 1000)  // 基础1小时
int randomDelay = new Random().nextInt(61);  // 随机0-1小时

// websocket_token: 8-12小时随机
@Scheduled(fixedDelay = 8 * 60 * 60 * 1000)  // 基础8小时
int randomDelay = new Random().nextInt(241);  // 随机0-4小时
```

### 当前策略（推荐）
```java
// _m_h5_tk: 1.5-2.5小时随机
@Scheduled(fixedDelay = 90 * 60 * 1000)  // 基础1.5小时
int randomDelay = new Random().nextInt(61);  // 随机0-1小时

// websocket_token: 10-14小时随机
@Scheduled(fixedDelay = 10 * 60 * 60 * 1000)  // 基础10小时
int randomDelay = new Random().nextInt(241);  // 随机0-4小时
```

## 监控指标

### 关键指标
1. **刷新成功率**：应保持在95%以上
2. **刷新间隔分布**：应均匀分布在预期范围内
3. **Token过期次数**：应接近0
4. **被限流次数**：应为0

### 异常告警
- 连续3次刷新失败 → 检查Cookie是否过期
- 刷新间隔异常 → 检查定时任务是否正常
- 频繁触发验证码 → 考虑增加随机延迟范围

## 总结

随机刷新策略通过模拟真实用户行为，有效降低了被检测为机器人的风险。关键要点：

1. ✅ **不使用固定时间** - 避免规律性被检测
2. ✅ **合理的随机范围** - 既保证Token有效，又足够随机
3. ✅ **账号间隔随机** - 多账号刷新时避免批量特征
4. ✅ **详细的日志记录** - 便于监控和调试

**核心理念：** 让机器人的行为看起来像真实用户！
