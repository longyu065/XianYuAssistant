# Playwright自动化滑块验证说明

## 功能概述

使用 Playwright 自动化浏览器处理滑块验证，无需手动复制 Token，系统会自动：
1. 检测到滑块验证
2. 打开浏览器窗口
3. 等待用户完成验证
4. 自动捕获 Token
5. 自动连接 WebSocket

## 安装 Playwright

### 首次使用需要安装浏览器驱动

在项目根目录执行：

```bash
# Windows
mvn exec:java -e -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args="install"

# 或者直接运行
mvn exec:java -e -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args="install chromium"
```

这会下载 Chromium 浏览器驱动（约 100MB）。

### 验证安装

```bash
mvn exec:java -e -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args="--version"
```

## 使用流程

### 自动模式（推荐）

1. **启动应用**
   ```bash
   mvn spring-boot:run
   ```

2. **访问 WebSocket 页面**
   ```
   http://localhost:8080/websocket.html
   ```

3. **启动监听**
   - 输入账号ID
   - 留空 AccessToken 输入框
   - 点击"启动监听"

4. **自动处理滑块验证**
   - 系统检测到滑块验证
   - 自动打开 Chromium 浏览器窗口
   - 在浏览器中完成滑块验证
   - 系统自动捕获 Token
   - 自动连接 WebSocket
   - ✅ 完成！

### 手动模式（备用）

如果 Playwright 不可用或自动处理失败，系统会自动回退到手动模式：
1. 弹出验证窗口
2. 显示详细的手动操作指引
3. 用户手动获取 Token
4. 粘贴 Token 后重新连接

## 工作原理

### 1. 滑块验证检测
```java
// WebSocketTokenServiceImpl.java
if (needCaptcha) {
    String captchaUrl = (String) dataMap.get("url");
    
    // 尝试使用Playwright自动处理
    if (playwrightCaptchaService.isPlaywrightAvailable()) {
        String token = playwrightCaptchaService.handleCaptchaAndGetToken(...);
        if (token != null) {
            return token; // 成功！
        }
    }
    
    // 回退到手动模式
    throw new CaptchaRequiredException(captchaUrl);
}
```

### 2. Playwright 自动化流程
```java
// PlaywrightCaptchaServiceImpl.java

// 1. 启动浏览器（显示界面）
Browser browser = playwright.chromium().launch(
    new BrowserType.LaunchOptions().setHeadless(false)
);

// 2. 设置 Cookie
context.addCookies(parseCookies(cookieStr));

// 3. 监听网络请求，捕获 Token
page.onResponse(response -> {
    if (url.contains("mtop.taobao.idlemessage.pc.login.token")) {
        // 提取 accessToken
        String token = extractToken(response.text());
        tokenFuture.complete(token);
    }
});

// 4. 打开验证页面
page.navigate(captchaUrl);

// 5. 等待用户完成验证（最多2分钟）
String token = tokenFuture.get(120, TimeUnit.SECONDS);
```

### 3. Token 自动捕获

系统监听所有网络请求，当检测到 Token API 响应时：
- 自动解析 JSON 响应
- 提取 `data.accessToken` 字段
- 缓存 Token（20小时有效期）
- 继续 WebSocket 连接流程

## 优势

### vs 手动模式

| 特性 | Playwright 自动模式 | 手动模式 |
|------|-------------------|---------|
| 用户操作 | 只需完成滑块验证 | 需要打开F12、复制Token等多步操作 |
| 技术要求 | 无 | 需要了解浏览器开发者工具 |
| 出错概率 | 低 | 高（容易复制错误） |
| 体验 | 流畅 | 繁琐 |

### vs Python 方案

| 特性 | Java + Playwright | Python + Playwright |
|------|------------------|-------------------|
| 集成度 | 原生集成 | 需要额外进程 |
| 性能 | 高 | 中等 |
| 维护性 | 统一技术栈 | 多语言维护 |

## 配置选项

### 超时时间

默认等待 120 秒（2分钟），可以在 `PlaywrightCaptchaServiceImpl` 中修改：

```java
private static final int TIMEOUT_SECONDS = 120; // 修改这里
```

### 浏览器选项

#### 无头模式（不显示界面）

```java
browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
    .setHeadless(true) // 改为 true
);
```

**注意：** 无头模式下无法完成滑块验证，建议保持 `false`。

#### 浏览器类型

支持 Chromium、Firefox、WebKit：

```java
// Chromium（推荐）
browser = playwright.chromium().launch(...);

// Firefox
browser = playwright.firefox().launch(...);

// WebKit（Safari内核）
browser = playwright.webkit().launch(...);
```

## 故障排查

### 问题1：Playwright 不可用

**症状：** 日志显示 "Playwright不可用，使用手动模式"

**原因：** 未安装浏览器驱动

**解决：**
```bash
mvn exec:java -e -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args="install chromium"
```

### 问题2：浏览器无法启动

**症状：** 抛出异常 "Failed to launch browser"

**原因：** 
- 缺少系统依赖（Linux）
- 权限问题
- 端口占用

**解决：**

**Windows：** 通常无需额外配置

**Linux：**
```bash
# Ubuntu/Debian
sudo apt-get install -y \
    libnss3 \
    libnspr4 \
    libatk1.0-0 \
    libatk-bridge2.0-0 \
    libcups2 \
    libdrm2 \
    libxkbcommon0 \
    libxcomposite1 \
    libxdamage1 \
    libxfixes3 \
    libxrandr2 \
    libgbm1 \
    libasound2
```

### 问题3：Token 捕获失败

**症状：** 等待超时，未捕获到 Token

**原因：**
- 验证未完成
- 网络请求被拦截
- Token API 地址变化

**解决：**
1. 确保完成滑块验证
2. 检查网络连接
3. 查看浏览器 Network 标签，确认 Token 请求
4. 如果失败，系统会自动回退到手动模式

### 问题4：浏览器窗口一闪而过

**症状：** 浏览器打开后立即关闭

**原因：** 代码执行完毕，资源被清理

**解决：** 这是正常现象，如果需要保持窗口打开用于调试：

```java
// 在 finally 块前添加
log.info("按任意键关闭浏览器...");
System.in.read();
```

## 性能优化

### 1. 浏览器复用

当前每次都创建新的浏览器实例，可以优化为复用：

```java
// 单例模式
private static Browser sharedBrowser = null;

private Browser getBrowser() {
    if (sharedBrowser == null || !sharedBrowser.isConnected()) {
        sharedBrowser = playwright.chromium().launch(...);
    }
    return sharedBrowser;
}
```

### 2. 并发处理

支持多个账号同时处理滑块验证：

```java
// 使用线程池
ExecutorService executor = Executors.newFixedThreadPool(3);
Future<String> future = executor.submit(() -> 
    handleCaptchaAndGetToken(...)
);
```

### 3. 缓存优化

Token 已经缓存 20 小时，减少验证频率。

## 安全考虑

1. **Cookie 安全**
   - Cookie 仅在内存中传递
   - 不会记录到日志（除非 DEBUG 级别）

2. **浏览器隔离**
   - 每次使用独立的浏览器上下文
   - 验证完成后立即清理

3. **超时保护**
   - 最多等待 2 分钟
   - 防止资源泄漏

## 未来改进

1. **验证码识别**
   - 集成 OCR 识别验证码
   - 自动完成滑块验证（需要AI模型）

2. **多浏览器支持**
   - 支持用户选择浏览器类型
   - 自动选择最快的浏览器

3. **分布式部署**
   - 支持远程浏览器服务
   - 多机器并发处理

4. **监控告警**
   - 验证失败率统计
   - 自动告警通知

## 相关文件

- `src/main/java/com/feijimiao/xianyuassistant/service/PlaywrightCaptchaService.java` - 服务接口
- `src/main/java/com/feijimiao/xianyuassistant/service/impl/PlaywrightCaptchaServiceImpl.java` - 服务实现
- `src/main/java/com/feijimiao/xianyuassistant/service/impl/WebSocketTokenServiceImpl.java` - Token服务（集成Playwright）
- `pom.xml` - Playwright 依赖配置

## 测试步骤

1. 安装 Playwright 浏览器驱动
2. 启动应用
3. 访问 http://localhost:8080/websocket.html
4. 输入账号ID，点击"启动监听"
5. 在自动打开的浏览器中完成滑块验证
6. 等待系统自动捕获 Token
7. 验证 WebSocket 连接成功

## 日志示例

成功的日志应该类似：

```
【账号1】使用Playwright处理滑块验证...
【账号1】验证URL: https://...
【账号1】已设置12个Cookie
【账号1】正在打开验证页面...
【账号1】等待用户完成滑块验证（最多120秒）...
【账号1】请在打开的浏览器窗口中完成滑块验证
【账号1】捕获到Token响应: {"ret":["SUCCESS::调用成功"],"data":{"accessToken":"AAA..."}}
【账号1】成功提取Token，长度: 156
【账号1】✅ Playwright自动处理成功，已获取并缓存Token
【账号1】accessToken获取成功并已缓存
【账号1】WebSocket连接成功
```
