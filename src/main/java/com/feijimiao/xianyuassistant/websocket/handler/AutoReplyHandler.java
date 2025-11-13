package com.feijimiao.xianyuassistant.websocket.handler;

import com.feijimiao.xianyuassistant.service.AutoDeliveryService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 自动回复消息处理器
 * 监听买家发送的普通消息，根据关键词自动回复
 */
@Slf4j
@Component
public class AutoReplyHandler extends AbstractLwpHandler {
    
    @Autowired
    private AutoDeliveryService autoDeliveryService;
    
    @Override
    public String getLwpPath() {
        // 使用同步消息路径
        return "/s/para";
    }
    
    @Override
    protected Object parseParams(String accountId, Map<String, Object> messageData) {
        AutoReplyParams params = new AutoReplyParams();
        
        // 获取body
        Object bodyObj = messageData.get("body");
        if (!(bodyObj instanceof java.util.List)) {
            return params;
        }
        
        @SuppressWarnings("unchecked")
        java.util.List<Object> bodyList = (java.util.List<Object>) bodyObj;
        
        if (bodyList.isEmpty()) {
            return params;
        }
        
        // 遍历消息列表
        for (Object item : bodyList) {
            if (!(item instanceof Map)) {
                continue;
            }
            
            @SuppressWarnings("unchecked")
            Map<String, Object> messageItem = (Map<String, Object>) item;
            
            // 获取extension
            Map<String, Object> extension = getMap(messageItem, "extension");
            if (extension == null) {
                continue;
            }
            
            // 获取contentType - 只处理普通文本消息（contentType=1）
            Integer contentType = getInteger(extension, "contentType");
            if (contentType == null || contentType != 1) {
                continue;
            }
            
            // 获取消息内容
            String reminderContent = getString(extension, "reminderContent");
            if (reminderContent == null || reminderContent.isEmpty()) {
                continue;
            }
            
            // 获取发送者ID，确保不是自己发的消息
            String senderUserId = getString(extension, "senderUserId");
            if (senderUserId != null && senderUserId.equals(accountId)) {
                log.debug("【账号{}】跳过自己发送的消息", accountId);
                continue;
            }
            
            // 提取商品ID
            String reminderUrl = getString(extension, "reminderUrl");
            String xyGoodsId = extractItemIdFromUrl(reminderUrl);
            
            // 提取会话ID
            String sId = getString(messageItem, "cid");
            
            if (xyGoodsId != null && sId != null) {
                params.setXyGoodsId(xyGoodsId);
                params.setSId(sId);
                params.setBuyerMessage(reminderContent);
                params.setReminderUrl(reminderUrl);
                
                log.info("【账号{}】检测到买家消息: xyGoodsId={}, sId={}, message={}", 
                        accountId, xyGoodsId, sId, reminderContent);
                
                break; // 找到一个就够了
            }
        }
        
        return params;
    }
    
    @Override
    protected Object doHandle(String accountId, Object params, Map<String, Object> messageData) {
        AutoReplyParams replyParams = (AutoReplyParams) params;
        
        // 如果没有提取到商品ID或消息内容，说明不是目标消息
        if (replyParams.getXyGoodsId() == null || replyParams.getBuyerMessage() == null) {
            return "NOT_TARGET_MESSAGE";
        }
        
        try {
            Long accountIdLong = Long.parseLong(accountId);
            
            // 触发自动回复
            autoDeliveryService.handleAutoReply(
                    accountIdLong, 
                    replyParams.getXyGoodsId(), 
                    replyParams.getSId(),
                    replyParams.getBuyerMessage()
            );
            
            return "AUTO_REPLY_TRIGGERED";
            
        } catch (Exception e) {
            log.error("【账号{}】触发自动回复失败", accountId, e);
            return "AUTO_REPLY_FAILED";
        }
    }
    
    /**
     * 从URL中提取商品ID
     */
    private String extractItemIdFromUrl(String url) {
        if (url == null || url.isEmpty()) {
            return null;
        }
        
        try {
            if (url.contains("itemId=")) {
                String[] parts = url.split("itemId=");
                if (parts.length > 1) {
                    String itemId = parts[1].split("&")[0];
                    return itemId;
                }
            }
        } catch (Exception e) {
            log.error("提取商品ID失败: url={}", url, e);
        }
        
        return null;
    }
    
    /**
     * 自动回复参数
     */
    @Data
    public static class AutoReplyParams {
        private String xyGoodsId;
        private String sId;
        private String buyerMessage;
        private String reminderUrl;
    }
}
