package com.feijimiao.xianyuassistant.websocket.handler;

import com.feijimiao.xianyuassistant.service.AutoDeliveryService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 自动发货消息处理器
 * 监听content_type=26的消息，当消息内容是"[我已拍下，待付款]"时触发自动发货
 */
@Slf4j
@Component
public class AutoDeliveryHandler extends AbstractLwpHandler {
    
    @Autowired
    private AutoDeliveryService autoDeliveryService;
    
    @Override
    public String getLwpPath() {
        // 使用同步消息路径，因为聊天消息通常通过这个路径接收
        return "/s/para";
    }
    
    @Override
    protected Object parseParams(String accountId, Map<String, Object> messageData) {
        AutoDeliveryParams params = new AutoDeliveryParams();
        
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
            
            // 获取contentType
            Integer contentType = getInteger(extension, "contentType");
            if (contentType == null || contentType != 26) {
                continue;
            }
            
            // 获取消息内容
            String reminderContent = getString(extension, "reminderContent");
            if (reminderContent == null || !reminderContent.contains("[我已拍下，待付款]")) {
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
                params.setReminderContent(reminderContent);
                params.setReminderUrl(reminderUrl);
                
                log.info("【账号{}】检测到待付款消息: xyGoodsId={}, sId={}, content={}", 
                        accountId, xyGoodsId, sId, reminderContent);
                
                break; // 找到一个就够了
            }
        }
        
        return params;
    }
    
    @Override
    protected Object doHandle(String accountId, Object params, Map<String, Object> messageData) {
        AutoDeliveryParams deliveryParams = (AutoDeliveryParams) params;
        
        // 如果没有提取到商品ID，说明不是目标消息
        if (deliveryParams.getXyGoodsId() == null) {
            return "NOT_TARGET_MESSAGE";
        }
        
        try {
            Long accountIdLong = Long.parseLong(accountId);
            
            // 触发自动发货
            autoDeliveryService.handleAutoDelivery(
                    accountIdLong, 
                    deliveryParams.getXyGoodsId(), 
                    deliveryParams.getSId()
            );
            
            return "AUTO_DELIVERY_TRIGGERED";
            
        } catch (Exception e) {
            log.error("【账号{}】触发自动发货失败", accountId, e);
            return "AUTO_DELIVERY_FAILED";
        }
    }
    
    /**
     * 从URL中提取商品ID
     * URL格式: https://market.m.taobao.com/app/idleFish-F2e/fish-mini-im/chat?itemId=123456&...
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
            log.debug("提取商品ID失败: url={}", url, e);
        }
        
        return null;
    }
    
    /**
     * 自动发货参数
     */
    @Data
    public static class AutoDeliveryParams {
        private String xyGoodsId;
        private String sId;
        private String reminderContent;
        private String reminderUrl;
    }
}
