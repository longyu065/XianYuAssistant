# WebSocket 启动接口错误提示优化总结

## 问题
原来的 `/api/websocket/start` 接口在失败时只返回通用的错误信息：
```
WebSocket连接启动失败
```

这让用户无法知道具体是什么原因导致的失败。

## 解决方案

### 1. 新增三个自定义异常类
- `CookieNotFoundException` - Cookie 未找到
- `CookieExpiredException` - Cookie 过期或无效
- `TokenInvalidException` - WebSocket Token 无效

### 2. 改进错误识别逻辑

**在 WebSocketServiceImpl 中：**
- Cookie 不存在 → 抛出 `CookieNotFoundException`
- Cookie 缺少 unb 字段 → 抛出 `CookieExpiredException`
- 无法获取 Token → 抛出 `TokenInvalidException`

**在 WebSocketTokenServiceImpl 中：**
- 检测到 `FAIL_SYS_SESSION_EXPIRED` → 更新状态并抛出 `CookieExpiredException`
- 检测到 `FAIL_SYS_ILLEGAL_ACCESS` → 更新状态并抛出 `CookieExpiredException`

**在 WebSocketController 中：**
- 捕获所有自定义异常并返回具体的错误信息
- 如果没有异常但连接失败，通过 `getDetailedErrorMessage()` 检查数据库状态

### 3. 现在的错误提示

| 场景 | 错误提示 |
|------|---------|
| Cookie 不存在 | WebSocket连接启动失败：未找到账号Cookie，请先配置Cookie |
| Cookie 内容为空 | WebSocket连接启动失败：Cookie内容为空，请重新配置Cookie |
| Cookie 已过期 | WebSocket连接启动失败：Cookie已过期，请更新Cookie后重试 |
| Cookie 已失效 | WebSocket连接启动失败：Cookie已失效，请重新获取Cookie |
| Cookie 缺少 unb | WebSocket连接启动失败：Cookie中缺少unb字段，Cookie可能已过期或无效 |
| Token 已过期 | WebSocket连接启动失败：WebSocket Token已过期，系统将自动刷新Token，请稍后重试 |
| Token 无效 | WebSocket连接启动失败：WebSocket Token无效或连接被拒绝，请尝试更新Cookie或稍后重试 |
| 无法获取 Token | WebSocket连接启动失败：无法获取WebSocket Token，请检查Cookie是否有效 |
| 需要滑块验证 | 需要滑块验证（返回验证链接） |

## 改进效果

✅ **用户体验提升**：用户能清楚知道问题所在
✅ **问题定位快速**：开发人员能快速定位问题
✅ **自动状态更新**：系统自动更新 Cookie 和账号状态
✅ **错误分类清晰**：不同错误有不同的处理建议

## 文件修改清单

1. ✅ 新增：`src/main/java/com/feijimiao/xianyuassistant/exception/CookieNotFoundException.java`
2. ✅ 新增：`src/main/java/com/feijimiao/xianyuassistant/exception/CookieExpiredException.java`
3. ✅ 新增：`src/main/java/com/feijimiao/xianyuassistant/exception/TokenInvalidException.java`
4. ✅ 修改：`src/main/java/com/feijimiao/xianyuassistant/controller/WebSocketController.java`
5. ✅ 修改：`src/main/java/com/feijimiao/xianyuassistant/service/impl/WebSocketServiceImpl.java`
6. ✅ 修改：`src/main/java/com/feijimiao/xianyuassistant/service/impl/WebSocketTokenServiceImpl.java`

所有代码已通过语法检查，无编译错误。
