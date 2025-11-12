# 获取AccessToken完整指南

## 问题分析

从错误日志可以看到：
```
code: 401
reason: "decode token failed"
```

这说明手动输入的 accessToken 有问题，可能是：
1. Token 格式不正确（有空格或特殊字符）
2. Token 已过期
3. Token 不完整

## 正确获取 AccessToken 的方法

### 方法1：通过浏览器开发者工具（推荐）

#### 步骤1：打开开发者工具
1. 打开 Chrome 或 Edge 浏览器
2. 按 `F12` 打开开发者工具
3. 切换到 `Network`（网络）标签
4. 勾选 `Preserve log`（保留日志）

#### 步骤2：访问闲鱼并触发 Token 请求
1. 访问 https://www.goofish.com
2. 确保已登录
3. 在 Network 标签的过滤框中输入：`token`
4. 刷新页面或进行一些操作（如点击消息）

#### 步骤3：找到 Token 请求
查找名称包含以下内容的请求：
- `mtop.taobao.idlemessage.pc.login.token`
- 或者 URL 包含 `login.token`

#### 步骤4：提取 AccessToken
1. 点击该请求
2. 切换到 `Response`（响应）标签
3. 找到 JSON 响应中的 `data.accessToken` 字段
4. **完整复制** accessToken 的值（注意不要有空格或换行）

示例响应：
```json
{
  "ret": ["SUCCESS::调用成功"],
  "data": {
    "accessToken": "AAACaRRJ/1plADqnvUCqMzjFzpW06czOnD9Xq928AuCXGl8zins5efDJ0r97uzjrDeKvIeBTdBr7l0up8tH8X/LH18WSicQ/TrPeWgOzuE/G9BqaJhvfkFP9DbkqjlogK8bniPtoQ=="
  }
}
```

**重要提示：**
- accessToken 通常很长（100-200个字符）
- 不要有空格、换行符
- 包含 `=` 结尾是正常的（Base64编码）

### 方法2：使用 Console 脚本自动获取

在浏览器控制台（Console）执行以下脚本：

```javascript
(async function() {
    try {
        // 1. 获取Cookie
        const cookies = document.cookie;
        console.log('Cookies:', cookies);
        
        // 2. 解析必要字段
        const mh5tk = cookies.match(/_m_h5_tk=([^;]+)/)?.[1];
        if (!mh5tk) {
            console.error('未找到 _m_h5_tk');
            return;
        }
        
        const token = mh5tk.split('_')[0];
        const unb = cookies.match(/unb=([^;]+)/)?.[1];
        if (!unb) {
            console.error('未找到 unb');
            return;
        }
        
        const deviceId = `web_${unb}`;
        console.log('设备ID:', deviceId);
        
        // 3. 构建请求参数
        const timestamp = Date.now().toString();
        const dataVal = JSON.stringify({
            appKey: "444e9908a51d1cb236a27862abc769c9",
            deviceId: deviceId
        });
        
        // 4. 生成签名（简化版，使用MD5）
        // 注意：这里需要MD5库，如果没有，请使用方法1
        const signStr = `${timestamp}&${token}&34839810&${dataVal}`;
        console.log('签名字符串:', signStr);
        
        // 5. 构建URL
        const params = new URLSearchParams({
            jsv: '2.7.2',
            appKey: '34839810',
            t: timestamp,
            sign: 'YOUR_SIGN_HERE', // 需要MD5计算
            v: '1.0',
            type: 'originaljson',
            accountSite: 'xianyu',
            dataType: 'json',
            timeout: '20000',
            api: 'mtop.taobao.idlemessage.pc.login.token',
            sessionOption: 'AutoLoginOnly',
            data: dataVal
        });
        
        const url = `https://h5api.m.goofish.com/h5/mtop.taobao.idlemessage.pc.login.token/1.0/?${params}`;
        
        console.log('请求URL:', url);
        console.log('\n请使用方法1通过Network标签获取Token');
        
    } catch (error) {
        console.error('错误:', error);
    }
})();
```

### 方法3：直接使用自动获取（不推荐，会触发滑块）

留空 accessToken 输入框，让系统自动获取。但这可能会触发滑块验证。

## 验证 Token 是否正确

### 检查清单：
- [ ] Token 长度在 100-200 字符之间
- [ ] Token 只包含字母、数字、`+`、`/`、`=` 字符
- [ ] Token 没有空格或换行符
- [ ] Token 以 `=` 或 `==` 结尾（Base64特征）
- [ ] Token 是最近获取的（20小时内）

### 示例正确的 Token：
```
AAACaRRJ/1plADqnvUCqMzjFzpW06czOnD9Xq928AuCXGl8zins5efDJ0r97uzjrDeKvIeBTdBr7l0up8tH8X/LH18WSicQ/TrPeWgOzuE/G9BqaJhvfkFP9DbkqjlogK8bniPtoQ==
```

### 示例错误的 Token：
```
AAACaRRJ/1plADqnvUCqMzjFzpW06czOnD9Xq928AuCXGl8zins5efDJ0r97uz jrDeKvIeBTdBr7l0up8tH8X/LH18WSicQ/TrPeWgOzuE/G9BqaJhvfkFP9DbkqjlogK8bniPtoQ==
```
（注意中间有空格）

## 常见问题

### Q1: 为什么我的 Token 总是 401？
**A:** 可能原因：
1. Token 复制时包含了空格或换行
2. Token 已过期（超过20小时）
3. Token 不完整
4. Cookie 已失效

**解决方法：**
- 重新获取 Token，确保完整复制
- 使用最新的 Cookie
- 检查 Token 格式

### Q2: 如何知道 Token 是否有效？
**A:** 查看后端日志：
- 如果看到 `code=200` 和 `注册成功`，说明 Token 有效
- 如果看到 `code=401` 和 `decode token failed`，说明 Token 无效

### Q3: 可以不用手动输入 Token 吗？
**A:** 可以，但需要解决滑块验证问题：
1. 使用最新的 Cookie（通过扫码登录获取）
2. 不要频繁请求 Token
3. 系统会缓存 Token 20小时

### Q4: Token 有效期是多久？
**A:** 通常是 20 小时，过期后需要重新获取。

## 推荐流程

1. **首次使用**：
   - 通过扫码登录获取最新 Cookie
   - 留空 Token，让系统自动获取
   - 如果遇到滑块验证，按方法1手动获取

2. **后续使用**：
   - 系统会自动使用缓存的 Token
   - Token 有效期内无需重新获取
   - 过期后会自动刷新

3. **遇到问题**：
   - 检查 Cookie 是否有效
   - 手动获取新的 Token
   - 查看后端日志排查问题

## 测试步骤

1. 按方法1获取 accessToken
2. 复制到剪贴板（Ctrl+C）
3. 粘贴到前端页面的 AccessToken 输入框
4. 点击"启动监听"
5. 查看后端日志：
   - 应该看到 `code=200`
   - 应该看到 `注册成功`
   - 应该看到 `WebSocket连接成功`

## 调试技巧

### 查看完整的 Token
在后端日志中搜索：
```
【账号1】accessToken前50字符=
```

### 查看注册响应
在后端日志中搜索：
```
【账号1】消息#1
```

如果看到 `code=200`，说明成功；如果看到 `code=401`，说明 Token 有问题。

### 对比 Token
将你输入的 Token 和日志中的 Token 对比，看是否一致。
