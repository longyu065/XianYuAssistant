# Playwright验证失败排查指南

## 问题：弹窗浏览器一直报错"验证失败"

### 可能的原因

1. **Cookie 过期或无效**
2. **Cookie 域名不匹配**
3. **检测到自动化工具**
4. **网络问题**
5. **验证URL已过期**

## 解决方案

### 方案1：重新获取最新 Cookie（推荐）

#### 步骤1：扫码登录获取新 Cookie
1. 访问 http://localhost:8080/qrlogin.html
2. 使用闲鱼 APP 扫码登录
3. 等待登录成功，Cookie 自动保存

#### 步骤2：立即尝试连接
1. 访问 http://localhost:8080/websocket.html
2. 输入账号ID
3. 点击"启动监听"
4. 在弹出的浏览器中完成验证

**重要：** Cookie 有时效性，建议扫码后立即使用。

### 方案2：手动设置 Cookie

如果自动设置 Cookie 失败，可以手动设置：

#### 步骤1：在正常浏览器中登录
1. 打开 Chrome/Edge 浏览器
2. 访问 https://www.goofish.com
3. 登录你的闲鱼账号

#### 步骤2：导出 Cookie
1. 按 F12 打开开发者工具
2. 在 Console 执行：
```javascript
copy(document.cookie)
```
3. Cookie 已复制到剪贴板

#### 步骤3：更新数据库 Cookie
1. 打开数据库管理工具
2. 更新 `xianyu_cookie` 表中对应账号的 `cookie_value`
3. 粘贴刚才复制的 Cookie

#### 步骤4：重新尝试
1. 重启应用
2. 再次点击"启动监听"

### 方案3：使用手动模式（最可靠）

如果 Playwright 自动化一直失败，使用手动模式：

#### 步骤1：触发滑块验证
1. 点击"启动监听"
2. 等待系统检测到滑块验证

#### 步骤2：在正常浏览器中完成验证
1. 复制验证 URL
2. 在正常浏览器（非 Playwright）中打开
3. 完成滑块验证

#### 步骤3：获取 Token
1. 在验证完成的浏览器中按 F12
2. Network 标签 → 搜索 `token`
3. 找到 `mtop.taobao.idlemessage.pc.login.token` 请求
4. 复制 Response 中的 `data.accessToken`

#### 步骤4：使用 Token 连接
1. 返回 WebSocket 页面
2. 粘贴 Token 到输入框
3. 点击"启动监听"

## 详细排查步骤

### 1. 检查 Cookie 是否有效

#### 方法A：通过日志
查看后端日志：
```
【账号1】已设置12个Cookie  ← 应该有多个Cookie
```

如果 Cookie 数量很少（<5个），说明 Cookie 可能不完整。

#### 方法B：通过数据库
```sql
SELECT cookie_value FROM xianyu_cookie WHERE id = 1;
```

检查 Cookie 是否包含关键字段：
- `_m_h5_tk` - 必需
- `_m_h5_tk_enc` - 必需
- `cookie2` - 必需
- `t` - 必需
- `unb` - 必需

### 2. 检查验证页面加载

查看日志：
```
【账号1】验证页面加载成功  ← 应该看到这个
```

如果看到：
```
【账号1】验证页面加载失败: ...
```

说明页面加载有问题，可能是：
- 网络问题
- URL 已过期
- Cookie 无效

### 3. 检查自动化检测

如果页面显示"检测到异常行为"或类似提示，说明被检测到了。

**解决方法：**
- 使用最新版本的 Playwright
- 确保已注入反检测脚本（代码已包含）
- 尝试使用真实浏览器的 Cookie

### 4. 检查网络请求

在 Playwright 打开的浏览器中：
1. 按 F12 打开开发者工具
2. 切换到 Network 标签
3. 查看是否有失败的请求（红色）
4. 检查 Cookie 是否正确发送

### 5. 查看详细错误信息

在浏览器页面上，查看具体的错误提示：
- "Cookie 无效" → 重新扫码登录
- "请求过于频繁" → 等待一段时间
- "账号异常" → 检查账号状态
- "验证失败" → 可能是 Cookie 问题

## 常见错误及解决方法

### 错误1：页面显示"请登录"

**原因：** Cookie 无效或未正确设置

**解决：**
1. 重新扫码登录
2. 确保 Cookie 包含所有必要字段
3. 检查 Cookie 的域名设置

### 错误2：页面显示"验证失败，请重试"

**原因：** 
- Cookie 过期
- 验证 URL 过期
- 网络问题

**解决：**
1. 重新触发验证（点击"启动监听"）
2. 使用最新的 Cookie
3. 检查网络连接

### 错误3：页面一直加载

**原因：** 网络问题或 URL 无效

**解决：**
1. 检查网络连接
2. 查看后端日志中的 URL 是否正确
3. 尝试在正常浏览器中打开该 URL

### 错误4：页面显示"检测到异常行为"

**原因：** 被识别为自动化工具

**解决：**
1. 确保使用最新代码（已包含反检测）
2. 尝试手动模式
3. 使用真实浏览器的 Cookie

## 最佳实践

### 1. Cookie 管理
- 定期更新 Cookie（建议每天扫码一次）
- 不要在多个地方同时使用同一个 Cookie
- 保持 Cookie 的完整性

### 2. 验证时机
- 避免频繁触发验证
- 利用 Token 缓存（20小时有效期）
- 在非高峰时段操作

### 3. 故障恢复
- 如果 Playwright 失败，自动回退到手动模式
- 保存验证 URL，可以稍后重试
- 记录失败原因，便于排查

## 调试技巧

### 1. 启用详细日志

在 `application.properties` 中添加：
```properties
logging.level.com.feijimiao.xianyuassistant.service.impl.PlaywrightCaptchaServiceImpl=DEBUG
```

### 2. 保存截图

代码已包含截图功能，失败时会自动截图。

### 3. 手动测试 Cookie

在 Playwright 打开的浏览器中，Console 执行：
```javascript
console.log(document.cookie);
```

检查 Cookie 是否正确设置。

### 4. 对比正常浏览器

1. 在正常浏览器中打开验证 URL
2. 对比 Cookie、请求头等
3. 找出差异并调整

## 终极解决方案

如果所有方法都失败，使用纯手动模式：

1. **不使用 Playwright**
2. **不使用自动获取 Token**
3. **完全手动操作**

### 步骤：
1. 在正常浏览器中登录闲鱼
2. 手动触发 Token 请求（访问消息页面）
3. 从 Network 中复制 Token
4. 粘贴到 WebSocket 页面
5. 连接成功 ✅

这种方式最可靠，但需要手动操作。

## 联系支持

如果问题仍未解决，请提供：
1. 完整的后端日志
2. 浏览器控制台的错误信息
3. 验证页面的截图
4. Cookie 的前50个字符（脱敏）

## 相关文档

- [Playwright自动化滑块验证说明.md](./Playwright自动化滑块验证说明.md)
- [滑块验证完整解决方案.md](./滑块验证完整解决方案.md)
- [获取AccessToken指南.md](./获取AccessToken指南.md)
