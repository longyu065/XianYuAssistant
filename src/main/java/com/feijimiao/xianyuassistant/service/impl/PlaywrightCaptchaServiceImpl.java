package com.feijimiao.xianyuassistant.service.impl;

import com.feijimiao.xianyuassistant.service.PlaywrightCaptchaService;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.Cookie;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Playwright滑块验证服务实现
 * 使用Playwright自动化浏览器处理滑块验证
 */
@Slf4j
@Service
public class PlaywrightCaptchaServiceImpl implements PlaywrightCaptchaService {

    private static final int TIMEOUT_SECONDS = 120; // 2分钟超时
    
    @Override
    public String handleCaptchaAndGetToken(Long accountId, String cookieStr, String deviceId, String captchaUrl) {
        log.info("【账号{}】使用Playwright处理滑块验证...", accountId);
        log.info("【账号{}】验证URL: {}", accountId, captchaUrl);
        
        Playwright playwright = null;
        Browser browser = null;
        
        try {
            // 1. 创建Playwright实例
            playwright = Playwright.create();
            
            // 2. 启动浏览器（使用Chromium，显示界面）
            browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                    .setHeadless(false) // 显示浏览器界面，方便用户完成验证
                    .setSlowMo(50) // 减慢操作速度，更像人类
                    .setArgs(java.util.Arrays.asList(
                            "--disable-blink-features=AutomationControlled", // 隐藏自动化特征
                            "--disable-dev-shm-usage",
                            "--no-sandbox"
                    )));
            
            // 3. 创建浏览器上下文（模拟真实浏览器环境）
            BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                    .setViewportSize(1280, 800)
                    .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36")
                    .setLocale("zh-CN")
                    .setTimezoneId("Asia/Shanghai")
                    .setPermissions(java.util.Arrays.asList("geolocation", "notifications"))
                    .setJavaScriptEnabled(true));
            
            // 4. 设置Cookie
            List<Cookie> cookies = parseCookies(cookieStr);
            context.addCookies(cookies);
            
            log.info("【账号{}】已设置{}个Cookie", accountId, cookies.size());
            
            // 5. 创建页面
            Page page = context.newPage();
            
            // 5.1 注入脚本，隐藏自动化特征
            page.addInitScript(
                "Object.defineProperty(navigator, 'webdriver', {get: () => undefined});" +
                "Object.defineProperty(navigator, 'plugins', {get: () => [1, 2, 3, 4, 5]});" +
                "Object.defineProperty(navigator, 'languages', {get: () => ['zh-CN', 'zh', 'en']});" +
                "window.chrome = {runtime: {}};"
            );
            
            log.info("【账号{}】已注入反检测脚本", accountId);
            
            // 6. 监听网络请求，捕获Token
            CompletableFuture<String> tokenFuture = new CompletableFuture<>();
            
            page.onResponse(response -> {
                String url = response.url();
                if (url.contains("mtop.taobao.idlemessage.pc.login.token")) {
                    try {
                        String responseText = response.text();
                        log.info("【账号{}】捕获到Token响应: {}", accountId, 
                                responseText.length() > 200 ? responseText.substring(0, 200) + "..." : responseText);
                        
                        // 解析响应获取Token
                        if (responseText.contains("accessToken")) {
                            // 简单的JSON解析
                            int start = responseText.indexOf("\"accessToken\":\"") + 15;
                            int end = responseText.indexOf("\"", start);
                            if (start > 14 && end > start) {
                                String token = responseText.substring(start, end);
                                log.info("【账号{}】成功提取Token，长度: {}", accountId, token.length());
                                tokenFuture.complete(token);
                            }
                        }
                    } catch (Exception e) {
                        log.error("【账号{}】解析Token响应失败", accountId, e);
                    }
                }
            });
            
            // 7. 访问验证页面
            log.info("【账号{}】正在打开验证页面...", accountId);
            log.info("【账号{}】验证URL: {}", accountId, captchaUrl);
            
            try {
                page.navigate(captchaUrl, new Page.NavigateOptions().setTimeout(30000));
                log.info("【账号{}】验证页面加载成功", accountId);
            } catch (Exception e) {
                log.error("【账号{}】验证页面加载失败: {}", accountId, e.getMessage());
                
                // 尝试截图保存错误状态
                try {
                    byte[] screenshot = page.screenshot();
                    log.info("【账号{}】已截图，长度: {} bytes", accountId, screenshot.length);
                } catch (Exception e2) {
                    log.error("【账号{}】截图失败", accountId, e2);
                }
                
                throw e;
            }
            
            // 8. 等待用户完成滑块验证
            log.info("【账号{}】等待用户完成滑块验证（最多{}秒）...", accountId, TIMEOUT_SECONDS);
            log.info("【账号{}】请在打开的浏览器窗口中完成滑块验证", accountId);
            log.info("【账号{}】提示：如果页面显示验证失败，可能是Cookie问题，请尝试重新扫码登录", accountId);
            
            // 等待Token或超时
            String token = null;
            try {
                token = tokenFuture.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                log.info("【账号{}】✅ 成功获取Token！", accountId);
                return token;
            } catch (Exception e) {
                log.error("【账号{}】等待Token超时或失败", accountId, e);
                
                // 超时后，尝试手动触发Token请求
                log.info("【账号{}】尝试手动触发Token请求...", accountId);
                try {
                    // 访问闲鱼首页，可能会触发Token请求
                    page.navigate("https://www.goofish.com");
                    page.waitForTimeout(3000);
                    
                    // 再等待一会儿
                    token = tokenFuture.get(10, TimeUnit.SECONDS);
                    log.info("【账号{}】✅ 手动触发后成功获取Token！", accountId);
                    return token;
                } catch (Exception e2) {
                    log.error("【账号{}】手动触发也失败", accountId, e2);
                    return null;
                }
            }
            
        } catch (Exception e) {
            log.error("【账号{}】Playwright处理滑块验证失败", accountId, e);
            return null;
        } finally {
            // 清理资源
            if (browser != null) {
                try {
                    browser.close();
                } catch (Exception e) {
                    log.error("【账号{}】关闭浏览器失败", accountId, e);
                }
            }
            if (playwright != null) {
                try {
                    playwright.close();
                } catch (Exception e) {
                    log.error("【账号{}】关闭Playwright失败", accountId, e);
                }
            }
        }
    }
    
    @Override
    public boolean isPlaywrightAvailable() {
        try {
            Playwright playwright = Playwright.create();
            playwright.close();
            return true;
        } catch (Exception e) {
            log.error("Playwright不可用", e);
            return false;
        }
    }
    
    /**
     * 解析Cookie字符串为Playwright的Cookie对象列表
     */
    private List<Cookie> parseCookies(String cookieStr) {
        List<Cookie> cookies = new ArrayList<>();
        
        if (cookieStr == null || cookieStr.isEmpty()) {
            return cookies;
        }
        
        String[] cookiePairs = cookieStr.split(";");
        for (String pair : cookiePairs) {
            String[] parts = pair.trim().split("=", 2);
            if (parts.length == 2) {
                String name = parts[0].trim();
                String value = parts[1].trim();
                
                // 跳过空值
                if (name.isEmpty() || value.isEmpty()) {
                    continue;
                }
                
                // 创建Cookie对象，设置更完整的属性
                Cookie cookie = new Cookie(name, value)
                        .setDomain(".goofish.com")
                        .setPath("/")
                        .setSecure(false)
                        .setHttpOnly(false);
                
                cookies.add(cookie);
                
                // 同时为 h5api.m.goofish.com 域名添加
                Cookie apiCookie = new Cookie(name, value)
                        .setDomain(".m.goofish.com")
                        .setPath("/")
                        .setSecure(false)
                        .setHttpOnly(false);
                
                cookies.add(apiCookie);
            }
        }
        
        return cookies;
    }
}
