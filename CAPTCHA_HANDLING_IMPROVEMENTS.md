# 滑块验证处理改进

## 问题描述

当启动 WebSocket 连接时触发滑块验证，系统只在日志中输出验证 URL，用户体验不友好：
- 用户不知道需要做什么
- 需要手动从日志中复制 URL
- 完成验证后不知道如何继续

## 解决方案

### 1. 前端弹窗提示

当检测到需要滑块验证时（`code: 500, needCaptcha: true`），前端会：

1. **显示确认对话框**
   ```
   检测到账号需要完成滑块验证才能启动连接。
   
   点击"打开验证"将在新窗口中打开验证页面，
   完成验证后请点击"清除验证等待"按钮重试。
   ```

2. **自动打开验证页面**
   - 点击"打开验证"按钮后，在新窗口中打开验证链接
   - 窗口大小：800x600
   - 用户在新窗口中完成滑块验证

3. **添加操作日志**
   ```
   ⚠️ 需要完成滑块验证
   已在新窗口打开验证页面，请完成验证后点击"清除验证等待"按钮重试
   ```

### 2. 清除验证等待状态

添加了"清除验证等待"按钮和功能：

**前端 API：**
```typescript
// 清除验证等待状态
export function clearCaptchaWait(accountId: number) {
  return request({
    url: '/websocket/clearCaptchaWait',
    method: 'POST',
    data: { xianyuAccountId: accountId }
  });
}
```

**前端处理函数：**
```typescript
const handleClearCaptchaWait = async () => {
  // 清除验证等待状态
  const response = await clearCaptchaWait(selectedAccountId.value);
  if (response.code === 0 || response.code === 200) {
    showSuccess('验证等待状态已清除，可以重新启动连接');
    addLog('✅ 验证等待状态已清除');
  }
};
```

**后端接口：**
```java
@PostMapping("/clearCaptchaWait")
public ResultObject<String> clearCaptchaWait(@RequestBody ClearCaptchaWaitReqDTO reqDTO) {
    tokenService.clearCaptchaWait(reqDTO.getXianyuAccountId());
    return ResultObject.success("验证等待状态已清除，可以重新请求");
}
```

### 3. 改进的日志输出

**后端日志：**
```
⚠️ 需要滑块验证: accountId=4, url=https://...
📋 滑块验证信息:
   - 账号ID: 4
   - 验证URL: https://...
   - 提示: 请在浏览器中完成验证，然后调用 /api/websocket/clearCaptchaWait 清除等待状态
```

**前端日志：**
```
⚠️ 需要完成滑块验证
已在新窗口打开验证页面，请完成验证后点击"清除验证等待"按钮重试
✅ 验证等待状态已清除
```

## 用户操作流程

### 完整流程

1. **用户点击"启动连接"**
   
2. **系统检测到需要验证**
   - 弹出确认对话框
   - 显示验证说明

3. **用户点击"打开验证"**
   - 新窗口打开验证页面
   - 用户完成滑块验证

4. **用户点击"清除验证等待"**
   - 清除后端的等待状态
   - 系统提示可以重试

5. **用户再次点击"启动连接"**
   - 使用新的 Token 启动连接
   - 连接成功

### 流程图

```
启动连接
    ↓
检测到需要验证
    ↓
弹出对话框 → 用户点击"打开验证"
    ↓
新窗口打开验证页面
    ↓
用户完成滑块验证
    ↓
用户点击"清除验证等待"
    ↓
清除等待状态
    ↓
用户再次点击"启动连接"
    ↓
连接成功 ✅
```

## UI 改进

### 按钮布局

```
[▶ 启动连接]  [⏹ 停止连接]  [🔓 清除验证等待]
```

- **启动连接**：绿色按钮，启动 WebSocket 连接
- **停止连接**：红色按钮，断开连接
- **清除验证等待**：橙色按钮，清除验证等待状态

### 对话框示例

```
┌─────────────────────────────────────┐
│  ⚠️  需要滑块验证                    │
├─────────────────────────────────────┤
│                                     │
│  检测到账号需要完成滑块验证才能启   │
│  动连接。                           │
│                                     │
│  点击"打开验证"将在新窗口中打开验   │
│  证页面，完成验证后请点击"清除验证  │
│  等待"按钮重试。                    │
│                                     │
├─────────────────────────────────────┤
│              [取消]  [打开验证]      │
└─────────────────────────────────────┘
```

## API 响应格式

### 需要验证时的响应

```json
{
  "code": 500,
  "msg": "需要滑块验证",
  "data": {
    "needCaptcha": true,
    "captchaUrl": "https://h5api.m.goofish.com:443//h5/mtop.taobao.idlemessage.pc.login.token/1.0/_____tmd_____/punish?...",
    "message": "检测到账号需要完成滑块验证。系统将自动打开验证页面，请完成验证后点击"清除验证等待"按钮重试。"
  }
}
```

### 清除等待状态的响应

```json
{
  "code": 200,
  "msg": "验证等待状态已清除，可以重新请求",
  "data": null
}
```

## 技术实现

### 前端改进

1. **添加 API 接口**
   - `clearCaptchaWait(accountId)` - 清除验证等待状态

2. **改进启动连接处理**
   - 检测 `code === 500 && data?.needCaptcha`
   - 显示确认对话框
   - 使用 `window.open()` 打开验证页面

3. **添加清除等待按钮**
   - 调用 `clearCaptchaWait` API
   - 显示成功提示

### 后端改进

1. **改进日志输出**
   - 使用 emoji 标记（⚠️ 📋）
   - 结构化显示验证信息

2. **改进错误消息**
   - 提供更详细的操作指引
   - 说明后续步骤

## 文件修改清单

### 前端
1. ✅ `vue-code/src/api/websocket.ts` - 添加 `clearCaptchaWait` API
2. ✅ `vue-code/src/views/connection/index.vue` - 改进启动连接处理，添加清除等待按钮

### 后端
1. ✅ `src/main/java/com/feijimiao/xianyuassistant/controller/WebSocketController.java` - 改进滑块验证日志

## 测试建议

1. 触发滑块验证场景
2. 验证弹窗是否正常显示
3. 验证新窗口是否正常打开
4. 完成验证后测试清除等待状态
5. 测试清除后重新启动连接
6. 检查日志输出是否清晰

## 注意事项

1. **验证窗口可能被浏览器拦截**
   - 提示用户允许弹出窗口
   - 或提供手动复制链接的选项

2. **验证有效期**
   - 验证 URL 有 5 分钟有效期
   - 超时需要重新触发

3. **清除等待状态**
   - 完成验证后必须清除等待状态
   - 否则会一直返回旧的验证 URL
