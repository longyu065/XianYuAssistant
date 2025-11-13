package com.feijimiao.xianyuassistant.websocket.handler;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 心跳消息处理器
 * 处理 /! 类型的心跳消息
 */
@Slf4j
@Component
public class HeartbeatHandler extends AbstractLwpHandler {
    
    @Override
    public String getLwpPath() {
        return "/!";
    }
    
    @Override
    protected boolean preHandle(String accountId, Map<String, Object> messageData) {
        // 心跳消息不需要详细日志
        return true;
    }
    
    @Override
    protected Object parseParams(String accountId, Map<String, Object> messageData) {
        HeartbeatParams params = new HeartbeatParams();
        
        // 获取headers
        Map<String, Object> headers = getMap(messageData, "headers");
        if (headers != null) {
            params.setMid(getString(headers, "mid"));
        }
        
        params.setTimestamp(System.currentTimeMillis());
        
        return params;
    }
    
    @Override
    protected Object doHandle(String accountId, Object params, Map<String, Object> messageData) {
        HeartbeatParams heartbeatParams = (HeartbeatParams) params;
        
        log.debug("【账号{}】收到心跳: mid={}", accountId, heartbeatParams.getMid());
        
        // 心跳消息通常不需要特殊处理，只需要记录即可
        return "OK";
    }
    
    @Override
    protected void postHandle(String accountId, Object result, Map<String, Object> messageData) {
        // 心跳消息不需要后置处理
    }
    
    /**
     * 心跳参数
     */
    @Data
    public static class HeartbeatParams {
        private String mid;
        private long timestamp;
    }
}
