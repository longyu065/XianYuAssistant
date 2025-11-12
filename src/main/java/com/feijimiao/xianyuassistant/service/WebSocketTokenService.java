package com.feijimiao.xianyuassistant.service;

/**
 * WebSocket Token服务接口
 * 用于获取WebSocket连接所需的accessToken
 */
public interface WebSocketTokenService {
    
    /**
     * 获取accessToken
     * 参考Python的refresh_token方法
     * 
     * @param accountId 账号ID
     * @param cookiesStr Cookie字符串
     * @param deviceId 设备ID
     * @return accessToken，失败返回null
     */
    String getAccessToken(Long accountId, String cookiesStr, String deviceId);
    
    /**
     * 保存Token到数据库
     * 
     * @param accountId 账号ID
     * @param token accessToken
     */
    void saveToken(Long accountId, String token);
}
