# 扫码刷新Cookie功能说明

## 功能概述

在连接管理页面的Cookie信息卡片中添加"扫码刷新Cookie"按钮，允许用户通过扫码方式刷新账号的Cookie信息。

## 实现内容

### 1. 新增组件

#### 扫码刷新Cookie组件
**文件**: `vue-code/src/views/connection/components/RefreshCookieDialog.vue`

创建了专门用于刷新Cookie的扫码对话框组件，复用了扫码登录的核心逻辑，但增加了账号匹配验证。

#### 手动更新Cookie组件
**文件**: `vue-code/src/views/connection/components/ManualUpdateCookieDialog.vue`

创建了手动更新Cookie的对话框组件，允许用户直接粘贴Cookie字符串进行更新。

#### 组件特性
- 接收参数：
  - `accountId`: 当前账号ID
  - `currentUnb`: 当前账号的UNB标识
- 扫码流程：
  1. 生成二维码
  2. 轮询检查扫码状态
  3. 扫码成功后获取Cookie
  4. 验证扫码账号与当前账号是否匹配

### 2. 账号匹配逻辑

#### 匹配情况
```typescript
if (scannedUnb === props.currentUnb) {
  // 账号匹配，更新Cookie
  await updateAccount({
    id: props.accountId,
    cookieText
  })
  showSuccess('Cookie刷新成功')
}
```

#### 不匹配情况
```typescript
else {
  // 账号不匹配，弹窗提示
  await ElMessageBox.alert(
    `扫码登录账号(${scannedUnb})与当前账号(${props.currentUnb})不匹配，已刷新或新增账号`,
    '账号不匹配',
    { type: 'warning' }
  )
}
```

### 3. 连接管理页面集成

**文件**: `vue-code/src/views/connection/index.vue`

#### 新增功能
1. **导入组件**: 引入 `RefreshCookieDialog` 组件
2. **添加按钮**: 在Cookie信息卡片底部添加"扫码刷新Cookie"按钮
3. **状态管理**: 
   - `showRefreshCookieDialog`: 控制对话框显示
   - `currentAccount`: 计算属性，获取当前选中账号的完整信息
4. **事件处理**:
   - `handleRefreshCookie`: 打开扫码对话框
   - `handleRefreshCookieSuccess`: Cookie刷新成功后的回调

#### 按钮布局
在Cookie信息卡片底部添加了两个按钮，采用并排布局：
- **扫码刷新** (warning类型): 通过扫码方式刷新Cookie
- **手动更新** (primary plain类型): 手动输入Cookie字符串更新

两个按钮使用 `flex` 布局，各占50%宽度，间距8px，保持页面简洁美观。

## 使用流程

### 扫码刷新流程
1. 用户在连接管理页面选择一个账号
2. 在Cookie信息卡片中点击"扫码刷新"按钮
3. 弹出扫码对话框，显示二维码
4. 用户使用闲鱼APP扫描二维码并确认登录
5. 系统获取Cookie并验证账号：
   - **匹配**: 更新当前账号的Cookie，提示"Cookie刷新成功"
   - **不匹配**: 弹窗提示账号不匹配，说明已刷新或新增账号

### 手动更新流程
1. 用户在连接管理页面选择一个账号
2. 在Cookie信息卡片中点击"手动更新"按钮
3. 弹出手动更新对话框，显示当前Cookie值
4. 用户修改或粘贴新的Cookie字符串
5. 点击"确定更新"按钮
6. 系统更新Cookie并刷新连接状态

## 安全性考虑

1. **账号验证**: 通过UNB标识验证扫码账号与当前账号是否一致
2. **明确提示**: 对话框中提示"请确保扫码账号与当前账号一致"
3. **错误处理**: 不匹配时给出明确的警告提示，避免误操作

## 技术细节

### 复用现有API
- `generateQRCode`: 生成二维码
- `getQRCodeStatus`: 轮询检查扫码状态
- `getQRCodeCookies`: 获取扫码登录后的Cookie
- `updateAccount`: 更新账号信息

### 状态管理
```typescript
const showRefreshCookieDialog = ref(false)
const currentAccount = computed(() => {
  return accounts.value.find(acc => acc.id === selectedAccountId.value)
})
```

### 成功回调
```typescript
const handleRefreshCookieSuccess = async () => {
  addLog('Cookie已刷新')
  if (selectedAccountId.value) {
    await loadConnectionStatus(selectedAccountId.value)
  }
}
```

## 用户体验优化

1. **按钮布局**: 两个按钮并排显示，各占50%宽度，间距适中
2. **按钮样式**: 
   - 扫码刷新使用警告色(warning)，突出主要操作
   - 手动更新使用朴素主色(primary plain)，作为辅助选项
3. **即时反馈**: 更新成功后自动刷新连接状态
4. **日志记录**: 在操作日志中记录Cookie更新操作
5. **友好提示**: 
   - 扫码不匹配时给出清晰的说明
   - 手动更新时提供格式示例和说明
6. **预填充**: 手动更新对话框自动填充当前Cookie值，方便修改

## 注意事项

1. 扫码刷新Cookie时，如果扫码的账号与当前账号不一致，系统会提示但不会阻止操作
2. 不匹配的情况下，扫码的账号可能会被添加为新账号或更新其他已存在的账号
3. 建议用户在扫码前确认使用正确的闲鱼账号进行扫码
