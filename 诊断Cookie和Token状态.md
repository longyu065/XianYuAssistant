# Cookie和Token状态诊断指南

## 错误类型判断

### 1. AccessToken过期（401错误）

**错误信息**：
```
{"code":401,"body":{"reason":"token is not found"}}
```

**原因**：WebSocket的accessToken过期（有效期20小时）

**解决方法**：
- 系统会自动重连并刷新Token
- 如果自动刷新失败，检查Cookie是否有效

### 2. Cookie过期（FAIL_SYS_TOKEN_EXOIRED）

**错误信息**：
```
{"ret":["FAIL_SYS_TOKEN_EXOIRED::令牌过期"]}
```

**原因**：Cookie中的`_m_h5_tk`字段过期，无法获取新的accessToken

**解决方法**：
- **必须重新扫码登录**
- 访问：`http://localhost:8080/qrlogin.html`
- 扫码后Cookie会自动更新

### 3. 滑块验证（FAIL_SYS_USER_VALIDATE）

**错误信息**：
```
{"ret":["FAIL_SYS_USER_VALIDATE::需要滑块验证"]}
```

**原因**：触发了闲鱼的安全验证

**解决方法**：
- 使用人工滑块验证页面完成验证
- 访问 `http://localhost:8080/websocket-manual-captcha.html`

## 快速诊断

### 步骤1：查看数据库中的Cookie

```sql
SELECT 
    id,
    xianyu_account_id,
    m_h5_tk,
    expire_time,
    datetime(expire_time, 'unixepoch', 'localtime') as expire_time_readable,
    CASE 
        WHEN expire_time > strftime('%s', 'now') * 1000 THEN '有效'
        ELSE '已过期'
    END as status
FROM xianyu_cookie
WHERE xianyu_account_id = 1;
```

### 步骤2：查看Token状态

```sql
SELECT 
    id,
    xianyu_account_id,
    websocket_token,
    token_expire_time,
    datetime(token_expire_time/1000, 'unixepoch', 'localtime') as token_expire_readable,
    CASE 
        WHEN token_expire_time > strftime('%s', 'now') * 1000 THEN '有效'
        ELSE '已过期'
    END as token_status
FROM xianyu_cookie
WHERE xianyu_account_id = 1;
```

### 步骤3：查看日志判断

**Cookie过期的日志特征**：
```
【账号1】Token API响应: {"ret":["FAIL_SYS_TOKEN_EXOIRED::令牌过期"]}
【账号1】获取accessToken失败
```

**AccessToken过期的日志特征**：
```
【账号1】消息#1 [响应(code=401)]: {"code":401}
【账号1】❌ Token失效(401)，需要重新获取Token并重连
【账号1】Token失效，开始自动重连流程...
```

## 解决方案对照表

| 问题 | 错误信息 | 是否自动修复 | 手动操作 |
|------|---------|-------------|---------|
| AccessToken过期 | code=401 | ✅ 是 | 无需操作 |
| Cookie过期 | FAIL_SYS_TOKEN_EXOIRED | ❌ 否 | 重新扫码登录 |
| 滑块验证 | FAIL_SYS_USER_VALIDATE | ⚠️ 尝试自动 | 可能需要手动 |

## 当前问题分析

根据你的日志：
```
【账号1】Token API响应: {"ret":["FAIL_SYS_TOKEN_EXOIRED::令牌过期"]}
```

**诊断结果**：Cookie已过期

**解决步骤**：

1. **访问扫码登录页面**
   ```
   http://localhost:8080/qrlogin.html
   ```

2. **使用闲鱼App扫码**
   - 打开闲鱼App
   - 扫描页面上的二维码
   - 确认登录

3. **等待Cookie保存**
   - 扫码成功后会自动保存Cookie
   - 看到"登录成功"提示

4. **重新启动WebSocket**
   ```bash
   # 方法1：使用Web界面
   访问：http://localhost:8080/websocket-send.html
   点击"启动连接"
   
   # 方法2：使用curl命令
   curl -X POST http://localhost:8080/api/websocket/start \
     -H "Content-Type: application/json" \
     -d "{\"xianyuAccountId\": 1}"
   ```

5. **验证连接成功**
   - 查看日志应该显示：
   ```
   【账号1】✅ 注册成功
   【账号1】WebSocket连接成功
   ```

## Cookie有效期

闲鱼Cookie的有效期通常为：
- **短期Cookie**：几小时到1天
- **长期Cookie**：7-30天（取决于"记住我"选项）

**建议**：
- 定期（每周）重新扫码登录
- 或者在收到"令牌过期"错误时立即重新登录

## 预防措施

### 1. 监控Cookie状态

可以添加定时任务，定期检查Cookie是否即将过期：

```java
@Scheduled(cron = "0 0 */6 * * ?") // 每6小时检查一次
public void checkCookieStatus() {
    // 检查Cookie过期时间
    // 如果即将过期，发送通知
}
```

### 2. 自动续期

某些情况下可以通过访问闲鱼页面来续期Cookie：

```java
// 访问闲鱼首页，可能会自动续期Cookie
HttpClient.get("https://www.goofish.com/");
```

### 3. 多账号备份

如果有多个账号，可以轮流使用，避免单点故障。

## 常见问题

### Q1: 为什么刚扫码登录，Cookie就过期了？

**A**: 可能的原因：
- 闲鱼检测到异常行为（频繁请求）
- IP地址变化
- 设备指纹变化
- 触发了安全验证

**解决**：
- 降低请求频率
- 使用固定IP
- 完成滑块验证

### Q2: 可以手动更新Cookie吗？

**A**: 可以，但不推荐：
1. 在浏览器中登录闲鱼
2. 打开开发者工具（F12）
3. 复制Cookie
4. 在数据库中更新

**推荐方式**：使用扫码登录功能，更安全可靠。

### Q3: AccessToken和Cookie有什么区别？

**A**: 
- **Cookie**：用于身份认证，有效期较长（天级别）
- **AccessToken**：用于WebSocket连接，有效期较短（20小时）
- **关系**：需要有效的Cookie才能获取AccessToken

## 总结

**当前问题**：Cookie过期

**解决方法**：重新扫码登录

**访问地址**：`http://localhost:8080/qrlogin.html`

扫码登录后，系统会自动：
1. 保存新的Cookie
2. 获取新的AccessToken
3. 建立WebSocket连接
4. 开始接收消息

一切都会恢复正常！
