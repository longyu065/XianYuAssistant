package com.feijimiao.xianyuassistant.websocket.handler;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 注册响应处理器
 * 处理 /r/register 类型的注册响应
 */
@Slf4j
@Component
public class RegisterHandler extends AbstractLwpHandler {
    
    @Override
    public String getLwpPath() {
        return "/r/register";
    }
    
    @Override
    protected Object parseParams(String accountId, Map<String, Object> messageData) {
        RegisterParams params = new RegisterParams();
        
        // 获取响应码
        params.setCode(getInteger(messageData, "code"));
        
        // 获取headers
        Map<String, Object> headers = getMap(messageData, "headers");
        if (headers != null) {
            params.setRegSid(getString(headers, "reg-sid"));
            params.setSid(getString(headers, "sid"));
            params.setMid(getString(headers, "mid"));
        }
        
        return params;
    }
    
    @Override
    protected Object doHandle(String accountId, Object params, Map<String, Object> messageData) {
        RegisterParams registerParams = (RegisterParams) params;
        
        if (registerParams.getCode() != null && registerParams.getCode() == 200) {
            if (registerParams.getRegSid() != null) {
                log.info("【账号{}】✅ 注册成功: reg-sid={}, sid={}", 
                        accountId, registerParams.getRegSid(), registerParams.getSid());
                return "REGISTERED";
            } else {
                log.info("【账号{}】收到注册响应: sid={}", accountId, registerParams.getSid());
                return "RESPONSE";
            }
        } else {
            log.error("【账号{}】注册失败: code={}", accountId, registerParams.getCode());
            return "FAILED";
        }
    }
    
    @Override
    protected void postHandle(String accountId, Object result, Map<String, Object> messageData) {
        if ("REGISTERED".equals(result)) {
            log.info("【账号{}】WebSocket注册流程完成，可以开始收发消息", accountId);
        }
    }
    
    /**
     * 注册参数
     */
    @Data
    public static class RegisterParams {
        private Integer code;
        private String regSid;
        private String sid;
        private String mid;
    }
}
