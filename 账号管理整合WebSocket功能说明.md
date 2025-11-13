# 账号管理整合WebSocket功能说明

## 更新时间
2025-11-13

## 更新内容

### 1. 功能整合 ✅

将原本独立的"WebSocket状态"页面功能整合到"账号管理"页面中，实现一站式账号和连接管理。

### 2. 删除的内容

#### 2.1 导航菜单
- ❌ 删除"WebSocket状态"菜单项
- ✅ 保留其他菜单项

#### 2.2 页面和代码
- ❌ 删除`renderWebSocket()`方法
- ❌ 删除`WebSocketManager`管理器
- ❌ 删除WebSocket状态页面的case分支

### 3. 新增的功能

#### 3.1 账号列表增强
在账号管理页面的表格中新增：
- **WebSocket状态列**：显示每个账号的连接状态
  - 已连接：绿色标签
  - 未连接：灰色标签
- **操作列增强**：
  - 已连接账号：显示"停止"按钮
  - 未连接账号：显示"启动"和"手动Token"按钮
  - 保留"详情"按钮

#### 3.2 实时状态查询
- 加载账号列表时自动查询所有账号的WebSocket状态
- 使用Promise.all并发查询，提高加载速度
- 错误处理：查询失败时默认显示"未连接"

#### 3.3 WebSocket操作方法
在AccountManager中新增：
- `startWebSocket(accountId)` - 启动WebSocket连接
- `startWebSocketWithToken(accountId)` - 使用手动Token启动
- `stopWebSocket(accountId)` - 停止WebSocket连接

## 界面展示

### 账号列表表格结构
```
┌────┬──────┬─────┬────────┬───────────┬────────────┬──────────────────┐
│ ID │ 备注 │ UNB │账号状态│ WebSocket │ 创建时间   │ 操作             │
├────┼──────┼─────┼────────┼───────────┼────────────┼──────────────────┤
│ 1  │ 主号 │ xxx │ 正常   │ 已连接    │ 2025-11-13 │ [停止] [详情]    │
│ 2  │ 副号 │ xxx │ 正常   │ 未连接    │ 2025-11-13 │ [启动] [手动] [详情] │
└────┴──────┴─────┴────────┴───────────┴────────────┴──────────────────┘
```

### 状态标签
- **账号状态**：
  - 正常：绿色标签
  - 异常：灰色标签

- **WebSocket状态**：
  - 已连接：绿色标签 "已连接"
  - 未连接：灰色标签 "未连接"

### 操作按钮
- **已连接账号**：
  - 红色"停止"按钮
  - 灰色"详情"按钮

- **未连接账号**：
  - 绿色"启动"按钮
  - 灰色"手动Token"按钮
  - 灰色"详情"按钮

## 代码实现

### 加载账号列表（增强版）
```javascript
async loadAccounts() {
    // 1. 获取账号列表
    const accountResult = await API.account.list();
    
    // 2. 并发查询所有账号的WebSocket状态
    const statusPromises = accountResult.data.map(acc => 
        API.websocket.status(acc.id).then(result => ({
            accountId: acc.id,
            connected: result.data?.connected || false
        })).catch(() => ({
            accountId: acc.id,
            connected: false
        }))
    );
    
    const wsStatuses = await Promise.all(statusPromises);
    
    // 3. 构建状态映射
    const wsStatusMap = {};
    wsStatuses.forEach(s => {
        wsStatusMap[s.accountId] = s.connected;
    });
    
    // 4. 渲染表格（包含WebSocket状态和操作按钮）
    // ...
}
```

### WebSocket操作方法
```javascript
// 启动WebSocket
async startWebSocket(accountId) {
    const result = await API.websocket.start(accountId);
    if (result.code === 200) {
        Utils.showMessage('启动成功', 'success');
        this.loadAccounts(); // 刷新列表
    } else if (result.data?.needCaptcha) {
        // 处理滑块验证
        window.open(result.data.captchaUrl, '_blank');
    }
}

// 使用手动Token启动
async startWebSocketWithToken(accountId) {
    const token = prompt('请输入AccessToken：');
    if (!token) return;
    
    const result = await API.websocket.start(accountId, token);
    if (result.code === 200) {
        Utils.showMessage('启动成功', 'success');
        this.loadAccounts();
    }
}

// 停止WebSocket
async stopWebSocket(accountId) {
    if (!confirm('确定要停止WebSocket连接吗？')) return;
    
    const result = await API.websocket.stop(accountId);
    if (result.code === 200) {
        Utils.showMessage('已停止', 'success');
        this.loadAccounts();
    }
}
```

## 使用流程

### 1. 查看账号和WebSocket状态
```
1. 进入"账号管理"页面
2. 系统自动加载所有账号
3. 同时查询每个账号的WebSocket状态
4. 在表格中显示完整信息
```

### 2. 启动WebSocket连接
```
1. 找到要启动的账号
2. 点击"启动"按钮
3. 如果需要验证，会弹出验证链接
4. 完成验证后自动连接
5. 状态更新为"已连接"
```

### 3. 使用手动Token启动
```
1. 找到要启动的账号
2. 点击"手动Token"按钮
3. 在弹窗中输入AccessToken
4. 系统使用Token直接连接
5. 状态更新为"已连接"
```

### 4. 停止WebSocket连接
```
1. 找到已连接的账号
2. 点击"停止"按钮
3. 确认操作
4. 连接断开
5. 状态更新为"未连接"
```

### 5. 刷新状态
```
1. 点击右上角"刷新状态"按钮
2. 系统重新查询所有账号和WebSocket状态
3. 表格更新显示最新状态
```

## 优势对比

### 整合前
- ❌ 需要在两个页面之间切换
- ❌ 账号信息和WebSocket状态分离
- ❌ 操作流程繁琐
- ❌ 页面冗余

### 整合后
- ✅ 一个页面完成所有操作
- ✅ 账号信息和WebSocket状态统一展示
- ✅ 操作流程简化
- ✅ 界面更简洁

## 性能优化

### 1. 并发查询
使用`Promise.all`并发查询所有账号的WebSocket状态，而不是串行查询：
```javascript
// 并发查询（快）
const statuses = await Promise.all(statusPromises);

// 串行查询（慢）
for (const acc of accounts) {
    const status = await API.websocket.status(acc.id);
}
```

### 2. 错误处理
每个状态查询都有独立的错误处理，单个账号查询失败不影响其他账号：
```javascript
.catch(() => ({
    accountId: acc.id,
    connected: false
}))
```

### 3. 状态映射
使用Map结构快速查找账号的WebSocket状态：
```javascript
const wsStatusMap = {};
wsStatuses.forEach(s => {
    wsStatusMap[s.accountId] = s.connected;
});
```

## 注意事项

### 1. 加载时间
- 首次加载需要查询所有账号的WebSocket状态
- 账号数量多时可能需要1-2秒
- 显示"加载中"提示，避免用户误操作

### 2. 状态刷新
- 操作完成后自动刷新列表
- 可以手动点击"刷新状态"按钮
- 建议定期刷新查看最新状态

### 3. 错误处理
- 查询失败时默认显示"未连接"
- 操作失败时显示错误提示
- 滑块验证时提供验证链接

### 4. 按钮状态
- 已连接账号只显示"停止"按钮
- 未连接账号显示"启动"和"手动Token"按钮
- 所有账号都显示"详情"按钮

## 后续优化建议

### 短期
1. 添加批量启动/停止功能
2. 添加自动刷新状态（定时器）
3. 优化加载速度（缓存状态）
4. 添加连接时长显示

### 长期
1. WebSocket连接日志查看
2. 连接质量监控
3. 异常自动重连
4. 连接统计图表

## 测试清单

- [x] 账号列表正常显示
- [x] WebSocket状态正确显示
- [x] 启动按钮功能正常
- [x] 停止按钮功能正常
- [x] 手动Token功能正常
- [x] 刷新状态功能正常
- [x] 滑块验证提示正常
- [x] 错误处理正常
- [x] 按钮状态切换正常
- [x] 并发查询性能正常

## 总结

通过将WebSocket状态管理功能整合到账号管理页面，实现了：

1. **界面简化**：减少一个独立页面，降低用户学习成本
2. **操作便捷**：一个页面完成账号和连接管理
3. **信息集中**：账号信息和WebSocket状态统一展示
4. **性能优化**：并发查询提高加载速度
5. **用户体验**：操作流程更加流畅

用户现在可以在账号管理页面直接查看和管理WebSocket连接，无需在多个页面之间切换，大大提升了使用效率。
