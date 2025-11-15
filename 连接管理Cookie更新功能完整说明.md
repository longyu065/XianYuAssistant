# 连接管理Cookie更新功能完整说明

## 功能概述

在连接管理页面实现了Cookie的扫码刷新和手动更新功能，所有Cookie更新接口统一放在WebSocketController中。

## 后端实现

### 1. DTO类（位于 `com.feijimiao.xianyuassistant.controller.dto`）

#### UpdateCookieReqDTO
```java
@Data
public class UpdateCookieReqDTO {
    private Long xianyuAccountId;  // 账号ID
    private String cookieText;     // Cookie文本
}
```

#### UpdateCookieRespDTO
```java
@Data
public class UpdateCookieRespDTO {
    private String message;  // 提示信息
}
```

### 2. WebSocketController 接口

**路径**: `/api/websocket/updateCookie`

**方法**: POST

**请求参数**:
```json
{
  "xianyuAccountId": 4,
  "cookieText": "unb=xxx; cookie2=xxx; ..."
}
```

**响应**:
```json
{
  "code": 200,
  "msg": "成功",
  "data": {
    "message": "Cookie更新成功"
  }
}
```

**功能**:
1. 验证账号ID和Cookie不为空
2. 检查账号是否存在
3. 从Cookie中提取UNB
4. 调用 `AccountService.updateAccountCookie()` 更新Cookie

### 3. AccountService 方法

#### updateAccountCookie
```java
boolean updateAccountCookie(Long accountId, String unb, String cookieText)
```

**功能**:
1. 更新账号表的UNB字段
2. 从Cookie中提取 `_m_h5_tk`
3. 更新或创建Cookie记录
4. 设置Cookie状态为有效（1）
5. 设置过期时间为30天后

#### extractMH5TkFromCookie (私有方法)
```java
private String extractMH5TkFromCookie(String cookie)
```

从Cookie字符串中提取 `_m_h5_tk` 字段的值。

## 前端实现

### 1. API定义 (`vue-code/src/api/websocket.ts`)

```typescript
export function updateCookie(data: { xianyuAccountId: number; cookieText: string }) {
  return request({
    url: '/websocket/updateCookie',
    method: 'POST',
    data
  });
}
```

### 2. 组件

#### ManualUpdateCookieDialog.vue
手动更新Cookie对话框

**功能**:
- 显示当前Cookie值
- 允许用户编辑Cookie
- 提供格式示例和重要字段提示
- 调用 `updateCookie` API更新

**重要字段标注**:
- `unb`: 用户唯一标识
- `_m_h5_tk`: H5 Token
- `cookie2`: 淘宝认证Cookie
- `t`: Token令牌

#### RefreshCookieDialog.vue
扫码刷新Cookie对话框

**功能**:
- 生成二维码
- 轮询检查扫码状态
- 验证扫码账号与当前账号是否匹配
- 匹配则调用 `updateCookie` API更新
- 不匹配则弹窗提示

### 3. 连接管理页面集成

在Cookie信息卡片底部添加两个按钮：
- **扫码刷新** (warning类型)
- **手动更新** (primary plain类型)

## 使用流程

### 扫码刷新Cookie
1. 点击"扫码刷新"按钮
2. 弹出二维码对话框
3. 使用闲鱼APP扫码并确认
4. 系统验证账号是否匹配
5. 匹配则更新Cookie，不匹配则提示

### 手动更新Cookie
1. 点击"手动更新"按钮
2. 弹出编辑对话框，显示当前Cookie
3. 修改或粘贴新的Cookie字符串
4. 点击"确定更新"
5. 系统验证并更新Cookie

## 重要说明

### 参数命名规范
- 后端DTO使用 `xianyuAccountId`（不是 `id` 或 `accountId`）
- 前端API调用时传递 `xianyuAccountId`
- 这是为了与WebSocket相关接口保持一致

### Cookie格式要求
必须包含以下关键字段：
- `unb`: 用于识别账号
- `_m_h5_tk`: 用于API签名
- `cookie2`: 淘宝认证
- `t`: Token令牌

### 接口位置
所有Cookie更新相关的接口都放在 `WebSocketController` 中，而不是 `AccountController`，因为：
1. Cookie更新通常与WebSocket连接相关
2. 便于统一管理连接相关的操作
3. 符合业务逻辑分组

## 故障排查

### 如果出现"账号ID不能为空"错误
1. 检查前端是否传递了 `xianyuAccountId` 参数
2. 检查参数名是否正确（不是 `id` 或 `accountId`）
3. 清除浏览器缓存，确保使用最新代码
4. 检查是否调用了正确的接口 `/api/websocket/updateCookie`

### 如果出现"无法从Cookie中提取UNB"错误
1. 检查Cookie字符串格式是否正确
2. 确保Cookie包含 `unb=xxx` 字段
3. 检查Cookie字段之间是否用分号和空格分隔

## 测试建议

1. **扫码刷新测试**:
   - 使用正确的账号扫码
   - 使用错误的账号扫码（验证不匹配提示）

2. **手动更新测试**:
   - 输入完整的Cookie字符串
   - 输入缺少UNB的Cookie（验证错误提示）
   - 输入空Cookie（验证非空校验）

3. **接口测试**:
   - 使用Postman测试 `/api/websocket/updateCookie`
   - 验证参数名必须是 `xianyuAccountId`
   - 验证Cookie格式要求
