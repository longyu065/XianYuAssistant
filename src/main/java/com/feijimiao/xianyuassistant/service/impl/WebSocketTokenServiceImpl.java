package com.feijimiao.xianyuassistant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.feijimiao.xianyuassistant.entity.XianyuCookie;
import com.feijimiao.xianyuassistant.exception.CaptchaRequiredException;
import com.feijimiao.xianyuassistant.mapper.XianyuCookieMapper;
import com.feijimiao.xianyuassistant.service.PlaywrightCaptchaService;
import com.feijimiao.xianyuassistant.service.WebSocketTokenService;
import com.feijimiao.xianyuassistant.utils.HttpClientUtils;
import com.feijimiao.xianyuassistant.utils.XianyuSignUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * WebSocket Token服务实现
 * 参考Python的refresh_token方法
 */
@Slf4j
@Service
public class WebSocketTokenServiceImpl implements WebSocketTokenService {

    @Autowired
    private PlaywrightCaptchaService playwrightCaptchaService;
    
    @Autowired
    private XianyuCookieMapper xianyuCookieMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Token API地址
     */
    private static final String TOKEN_API_URL = "https://h5api.m.goofish.com/h5/mtop.taobao.idlemessage.pc.login.token/1.0/";
    
    /**
     * Token 有效期（20小时，参考 Python 的 TOKEN_REFRESH_INTERVAL）
     */
    private static final long TOKEN_VALID_DURATION = 20 * 60 * 60 * 1000; // 20小时
    
    @Override
    public String getAccessToken(Long accountId, String cookiesStr, String deviceId) {
        try {
            // 1. 先从数据库检查是否有有效的 Token
            XianyuCookie cookieEntity = xianyuCookieMapper.selectOne(
                    new LambdaQueryWrapper<XianyuCookie>()
                            .eq(XianyuCookie::getXianyuAccountId, accountId)
            );
            
            if (cookieEntity != null && cookieEntity.getWebsocketToken() != null 
                    && cookieEntity.getTokenExpireTime() != null) {
                long now = System.currentTimeMillis();
                if (cookieEntity.getTokenExpireTime() > now) {
                    long remainingHours = (cookieEntity.getTokenExpireTime() - now) / (60 * 60 * 1000);
                    log.info("【账号{}】使用数据库中的accessToken（剩余有效期: {}小时）", 
                            accountId, remainingHours);
                    return cookieEntity.getWebsocketToken();
                } else {
                    log.info("【账号{}】数据库中的Token已过期，需要重新获取", accountId);
                }
            }
            
            log.info("【账号{}】开始获取新的accessToken...", accountId);
            
            // 1. 生成时间戳
            String timestamp = String.valueOf(System.currentTimeMillis());
            
            // 2. 解析Cookie获取_m_h5_tk token
            Map<String, String> cookies = XianyuSignUtils.parseCookies(cookiesStr);
            String mh5tk = cookies.get("_m_h5_tk");
            String token = "";
            if (mh5tk != null && mh5tk.contains("_")) {
                token = mh5tk.split("_")[0];
            }
            
            // 3. 构建data参数
            String dataVal = String.format("{\"appKey\":\"444e9908a51d1cb236a27862abc769c9\",\"deviceId\":\"%s\"}", deviceId);
            
            // 4. 生成签名
            String sign = XianyuSignUtils.generateSign(timestamp, token, dataVal);
            
            // 5. 构建URL参数
            Map<String, String> params = new HashMap<>();
            params.put("jsv", "2.7.2");
            params.put("appKey", "34839810");
            params.put("t", timestamp);
            params.put("sign", sign);
            params.put("v", "1.0");
            params.put("type", "originaljson");
            params.put("accountSite", "xianyu");
            params.put("dataType", "json");
            params.put("timeout", "20000");
            params.put("api", "mtop.taobao.idlemessage.pc.login.token");
            params.put("sessionOption", "AutoLoginOnly");
            params.put("dangerouslySetWindvaneParams", "%5Bobject%20Object%5D");
            params.put("smToken", "token");
            params.put("queryToken", "sm");
            params.put("sm", "sm");
            params.put("spm_cnt", "a21ybx.im.0.0");
            params.put("spm_pre", "a21ybx.home.sidebar.1.4c053da6vYwnmf");
            params.put("log_id", "4c053da6vYwnmf");
            
            // 6. 构建请求体
            Map<String, String> data = new HashMap<>();
            data.put("data", dataVal);
            
            // 7. 构建请求头
            Map<String, String> headers = new HashMap<>();
            headers.put("accept", "application/json");
            headers.put("accept-language", "zh-CN,zh;q=0.9,en;q=0.8");
            headers.put("cache-control", "no-cache");
            headers.put("content-type", "application/x-www-form-urlencoded");
            headers.put("pragma", "no-cache");
            headers.put("priority", "u=1, i");
            headers.put("sec-ch-ua", "\"Not;A=Brand\";v=\"99\", \"Google Chrome\";v=\"139\", \"Chromium\";v=\"139\"");
            headers.put("sec-ch-ua-mobile", "?0");
            headers.put("sec-ch-ua-platform", "\"Windows\"");
            headers.put("sec-fetch-dest", "empty");
            headers.put("sec-fetch-mode", "cors");
            headers.put("sec-fetch-site", "same-site");
            headers.put("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/139.0.0.0 Safari/537.36");
            headers.put("referer", "https://www.goofish.com/");
            headers.put("origin", "https://www.goofish.com");
            headers.put("cookie", cookiesStr);
            
            // 8. 构建完整URL（带查询参数）
            StringBuilder urlBuilder = new StringBuilder(TOKEN_API_URL);
            urlBuilder.append("?");
            params.forEach((key, value) -> {
                try {
                    urlBuilder.append(key).append("=").append(java.net.URLEncoder.encode(value, "UTF-8")).append("&");
                } catch (Exception e) {
                    log.error("URL编码失败: key={}", key, e);
                }
            });
            String fullUrl = urlBuilder.toString();
            if (fullUrl.endsWith("&")) {
                fullUrl = fullUrl.substring(0, fullUrl.length() - 1);
            }
            
            // 9. 发送POST请求
            String response = HttpClientUtils.post(fullUrl, headers, data);
            
            if (response == null || response.isEmpty()) {
                log.error("【账号{}】获取accessToken失败：响应为空", accountId);
                return null;
            }
            
            // 10. 解析响应
            @SuppressWarnings("unchecked")
            Map<String, Object> responseMap = objectMapper.readValue(response, Map.class);
            
            log.info("【账号{}】Token API响应: {}", accountId, response);
            
            // 检查ret字段
            Object retObj = responseMap.get("ret");
            if (retObj instanceof java.util.List) {
                @SuppressWarnings("unchecked")
                java.util.List<String> retList = (java.util.List<String>) retObj;
                log.info("【账号{}】ret字段内容: {}", accountId, retList);
                
                boolean success = retList.stream().anyMatch(ret -> ret.contains("SUCCESS::调用成功"));
                
                if (success) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> dataMap = (Map<String, Object>) responseMap.get("data");
                    if (dataMap != null && dataMap.containsKey("accessToken")) {
                        String accessToken = (String) dataMap.get("accessToken");
                        
                        // 保存 token 到数据库
                        saveTokenToDatabase(accountId, accessToken);
                        
                        log.info("【账号{}】accessToken获取成功并已保存到数据库", accountId);
                        log.debug("【账号{}】accessToken: {}...", accountId, 
                                accessToken.substring(0, Math.min(20, accessToken.length())));
                        return accessToken;
                    }
                }
                
                // 检查是否需要滑块验证
                boolean needCaptcha = retList.stream().anyMatch(ret -> ret.contains("FAIL_SYS_USER_VALIDATE"));
                log.info("【账号{}】是否需要滑块验证: {}", accountId, needCaptcha);
                
                if (needCaptcha) {
                    // 提取滑块验证URL
                    @SuppressWarnings("unchecked")
                    Map<String, Object> dataMap = (Map<String, Object>) responseMap.get("data");
                    log.info("【账号{}】data字段内容: {}", accountId, dataMap);
                    
                    if (dataMap != null && dataMap.containsKey("url")) {
                        String captchaUrl = (String) dataMap.get("url");
                        log.warn("【账号{}】检测到滑块验证，URL: {}", accountId, captchaUrl);
                        
                        // 尝试使用Playwright自动处理
                        if (playwrightCaptchaService.isPlaywrightAvailable()) {
                            log.info("【账号{}】尝试使用Playwright自动处理滑块验证...", accountId);
                            String playwrightToken = playwrightCaptchaService.handleCaptchaAndGetToken(
                                    accountId, cookiesStr, deviceId, captchaUrl);
                            
                            if (playwrightToken != null && !playwrightToken.isEmpty()) {
                                // 保存 token 到数据库
                                saveTokenToDatabase(accountId, playwrightToken);
                                log.info("【账号{}】✅ Playwright自动处理成功，已获取并保存Token到数据库", accountId);
                                return playwrightToken;
                            } else {
                                log.warn("【账号{}】Playwright自动处理失败，回退到手动模式", accountId);
                            }
                        } else {
                            log.warn("【账号{}】Playwright不可用，使用手动模式", accountId);
                        }
                        
                        // 如果Playwright失败或不可用，抛出异常让用户手动处理
                        log.warn("【账号{}】抛出滑块验证异常，需要手动处理", accountId);
                        throw new CaptchaRequiredException(captchaUrl);
                    } else {
                        log.error("【账号{}】需要滑块验证但未找到URL", accountId);
                    }
                }
            }
            
            log.error("【账号{}】获取accessToken失败：{}", accountId, response);
            return null;
            
        } catch (CaptchaRequiredException e) {
            // 重新抛出滑块验证异常，让上层处理
            throw e;
        } catch (Exception e) {
            log.error("【账号{}】获取accessToken异常", accountId, e);
            return null;
        }
    }
    
    @Override
    public void saveToken(Long accountId, String token) {
        saveTokenToDatabase(accountId, token);
    }
    
    /**
     * 保存 Token 到数据库
     * 
     * @param accountId 账号ID
     * @param token accessToken
     */
    private void saveTokenToDatabase(Long accountId, String token) {
        try {
            long expireTime = System.currentTimeMillis() + TOKEN_VALID_DURATION;
            
            int updated = xianyuCookieMapper.update(null,
                    new LambdaUpdateWrapper<XianyuCookie>()
                            .eq(XianyuCookie::getXianyuAccountId, accountId)
                            .set(XianyuCookie::getWebsocketToken, token)
                            .set(XianyuCookie::getTokenExpireTime, expireTime)
            );
            
            if (updated > 0) {
                log.info("【账号{}】Token已保存到数据库，过期时间: {}", accountId, 
                        new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                                .format(new java.util.Date(expireTime)));
            } else {
                log.warn("【账号{}】Token保存失败，未找到对应的Cookie记录", accountId);
            }
        } catch (Exception e) {
            log.error("【账号{}】保存Token到数据库失败", accountId, e);
        }
    }
}
