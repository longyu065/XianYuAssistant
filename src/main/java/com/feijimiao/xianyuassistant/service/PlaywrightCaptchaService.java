package com.feijimiao.xianyuassistant.service;

/**
 * Playwright滑块验证服务接口
 */
public interface PlaywrightCaptchaService {
    
    /**
     * 使用Playwright自动处理滑块验证并获取Token
     * 
     * @param accountId 账号ID
     * @param cookieStr Cookie字符串
     * @param deviceId 设备ID
     * @param captchaUrl 滑块验证URL
     * @return accessToken，如果失败返回null
     */
    String handleCaptchaAndGetToken(Long accountId, String cookieStr, String deviceId, String captchaUrl);
    
    /**
     * 检查Playwright是否可用
     * 
     * @return 是否可用
     */
    boolean isPlaywrightAvailable();
}
