package com.feijimiao.xianyuassistant.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 默认的WebSocket消息处理器实现
 * 参考Python代码的handle_message方法
 */
@Slf4j
@Component
public class DefaultWebSocketMessageHandler implements WebSocketMessageHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void handleMessage(String accountId, Map<String, Object> message) {
        try {
            log.info("【账号{}】开始处理消息...", accountId);
            
            // 检查消息类型
            String messageType = getMessageType(message);
            log.info("【账号{}】消息类型: {}", accountId, messageType);
            
            // 根据消息类型分发处理
            switch (messageType) {
                case "chat":
                    handleChatMessage(accountId, message);
                    break;
                case "system":
                    handleSystemMessage(accountId, message);
                    break;
                case "order":
                    handleOrderMessage(accountId, message);
                    break;
                case "notification":
                    handleNotificationMessage(accountId, message);
                    break;
                default:
                    handleUnknownMessage(accountId, message);
                    break;
            }
            
        } catch (Exception e) {
            log.error("【账号{}】处理消息失败", accountId, e);
        }
    }

    @Override
    public void handleHeartbeat(String accountId) {
        log.debug("【账号{}】处理心跳响应", accountId);
    }

    @Override
    public void handleError(String accountId, Exception error) {
        log.error("【账号{}】消息处理错误: {}", accountId, error.getMessage(), error);
    }

    /**
     * 获取消息类型
     */
    private String getMessageType(Map<String, Object> message) {
        Object type = message.get("type");
        if (type != null) {
            return type.toString();
        }
        
        // 尝试从其他字段推断类型
        if (message.containsKey("chatId")) {
            return "chat";
        } else if (message.containsKey("orderId")) {
            return "order";
        } else if (message.containsKey("notification")) {
            return "notification";
        }
        
        return "unknown";
    }

    /**
     * 处理聊天消息
     */
    private void handleChatMessage(String accountId, Map<String, Object> message) {
        log.info("【账号{}】收到聊天消息", accountId);
        
        // 提取聊天相关信息
        Object chatId = message.get("chatId");
        Object content = message.get("content");
        Object from = message.get("from");
        
        log.info("【账号{}】聊天ID: {}, 发送者: {}, 内容: {}", accountId, chatId, from, content);
        
        // TODO: 实现自动回复逻辑
    }

    /**
     * 处理系统消息
     */
    private void handleSystemMessage(String accountId, Map<String, Object> message) {
        log.info("【账号{}】收到系统消息", accountId);
        
        Object content = message.get("content");
        log.info("【账号{}】系统消息内容: {}", accountId, content);
    }

    /**
     * 处理订单消息
     */
    private void handleOrderMessage(String accountId, Map<String, Object> message) {
        log.info("【账号{}】收到订单消息", accountId);
        
        Object orderId = message.get("orderId");
        Object status = message.get("status");
        
        log.info("【账号{}】订单ID: {}, 状态: {}", accountId, orderId, status);
        
        // TODO: 实现自动发货逻辑
    }

    /**
     * 处理通知消息
     */
    private void handleNotificationMessage(String accountId, Map<String, Object> message) {
        log.info("【账号{}】收到通知消息", accountId);
        
        Object notification = message.get("notification");
        log.info("【账号{}】通知内容: {}", accountId, notification);
    }

    /**
     * 处理未知类型消息
     */
    private void handleUnknownMessage(String accountId, Map<String, Object> message) {
        log.warn("【账号{}】收到未知类型消息", accountId);
        log.debug("【账号{}】消息内容: {}", accountId, message);
    }
}
