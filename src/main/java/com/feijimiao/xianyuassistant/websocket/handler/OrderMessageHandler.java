package com.feijimiao.xianyuassistant.websocket.handler;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 订单消息处理器
 * 处理 /s/order 类型的订单消息
 */
@Slf4j
@Component
public class OrderMessageHandler extends AbstractLwpHandler {
    
    @Override
    public String getLwpPath() {
        return "/s/order";
    }
    
    @Override
    protected Object parseParams(String accountId, Map<String, Object> messageData) {
        OrderMessageParams params = new OrderMessageParams();
        
        // 获取body
        Map<String, Object> body = getMap(messageData, "body");
        if (body == null) {
            return null;
        }
        
        // 解析订单信息
        params.setOrderId(getString(body, "orderId"));
        params.setStatus(getString(body, "status"));
        params.setBuyerId(getString(body, "buyerId"));
        params.setSellerId(getString(body, "sellerId"));
        params.setItemId(getString(body, "itemId"));
        params.setAmount(getString(body, "amount"));
        
        return params;
    }
    
    @Override
    protected Object doHandle(String accountId, Object params, Map<String, Object> messageData) {
        OrderMessageParams orderParams = (OrderMessageParams) params;
        
        log.info("【账号{}】收到订单消息: orderId={}, status={}, amount={}", 
                accountId, orderParams.getOrderId(), orderParams.getStatus(), orderParams.getAmount());
        
        // TODO: 实现订单自动处理逻辑
        // 例如：自动发货、自动确认收货等
        
        return orderParams;
    }
    
    @Override
    protected void postHandle(String accountId, Object result, Map<String, Object> messageData) {
        OrderMessageParams orderParams = (OrderMessageParams) result;
        
        // TODO: 可以在这里触发订单状态变更的通知
        log.info("【账号{}】订单消息处理完成: orderId={}", accountId, orderParams.getOrderId());
    }
    
    /**
     * 订单消息参数
     */
    @Data
    public static class OrderMessageParams {
        private String orderId;
        private String status;
        private String buyerId;
        private String sellerId;
        private String itemId;
        private String amount;
    }
}
