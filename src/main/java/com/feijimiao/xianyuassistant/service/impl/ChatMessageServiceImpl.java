package com.feijimiao.xianyuassistant.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.feijimiao.xianyuassistant.entity.XianyuChatMessage;
import com.feijimiao.xianyuassistant.mapper.XianyuChatMessageMapper;
import com.feijimiao.xianyuassistant.service.ChatMessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 聊天消息服务实现
 */
@Slf4j
@Service
public class ChatMessageServiceImpl implements ChatMessageService {
    
    @Autowired
    private XianyuChatMessageMapper chatMessageMapper;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    public boolean saveChatMessage(Long accountId, String decryptedData) {
        try {
            // 解析JSON
            @SuppressWarnings("unchecked")
            Map<String, Object> data = objectMapper.readValue(decryptedData, Map.class);
            return saveChatMessageFromMap(accountId, data);
        } catch (Exception e) {
            log.error("保存聊天消息失败", e);
            return false;
        }
    }
    
    @Override
    public boolean saveChatMessageFromMap(Long accountId, Map<String, Object> messageData) {
        try {
            // 检查消息类型
            Object typeObj = messageData.get("2");
            if (typeObj != null && "2".equals(typeObj.toString())) {
                // 已读回执，不保存
                log.debug("收到已读回执，跳过保存");
                return true;
            }
            
            // 检查是否是聊天消息
            Object field1 = messageData.get("1");
            if (!(field1 instanceof Map)) {
                log.debug("不是聊天消息格式，跳过保存");
                return false;
            }
            
            @SuppressWarnings("unchecked")
            Map<String, Object> messageInfo = (Map<String, Object>) field1;
            
            // 创建消息实体
            XianyuChatMessage message = new XianyuChatMessage();
            message.setXianyuAccountId(accountId);
            
            // 提取消息ID（字段1.3）
            String messageId = extractString(messageInfo, "3");
            if (messageId == null || messageId.isEmpty()) {
                log.warn("消息ID为空，跳过保存");
                return false;
            }
            message.setMessageId(messageId);
            
            // 检查是否已存在
            try {
                XianyuChatMessage existing = chatMessageMapper.findByMessageId(accountId, messageId);
                if (existing != null) {
                    log.debug("【账号{}】消息已存在，跳过保存: {}", accountId, messageId);
                    return true;
                }
            } catch (Exception e) {
                // 查询失败也跳过，可能是数据库问题
                log.debug("【账号{}】查询消息失败，跳过保存: {}", accountId, messageId);
                return true;
            }
            
            // 提取发送者信息（字段1.1）
            Object senderObj = messageInfo.get("1");
            if (senderObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> senderInfo = (Map<String, Object>) senderObj;
                message.setSenderUserId(extractString(senderInfo, "1"));
                message.setSenderNickname(extractString(senderInfo, "2"));
            }
            
            // 提取接收者信息（字段1.2）
            message.setReceiverUserId(extractString(messageInfo, "2"));
            
            // 提取消息内容（字段1.4）
            Object contentObj = messageInfo.get("4");
            if (contentObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> contentInfo = (Map<String, Object>) contentObj;
                
                // 内容类型（字段1.4.1）
                message.setContentType(extractInteger(contentInfo, "1"));
                
                // 文本内容（字段1.4.2）
                message.setContentText(extractString(contentInfo, "2"));
                
                // 完整内容JSON
                message.setContentJson(objectMapper.writeValueAsString(contentInfo));
            }
            
            // 提取时间戳（字段1.5）
            message.setMessageTime(extractLong(messageInfo, "5"));
            
            // 提取会话ID（字段1.6）
            message.setSessionId(extractString(messageInfo, "6"));
            
            // 判断消息方向
            String receiverUserId = message.getReceiverUserId();
            String senderUserId = message.getSenderUserId();
            String accountIdStr = String.valueOf(accountId);
            
            if (receiverUserId != null && receiverUserId.contains(accountIdStr)) {
                message.setDirection(2); // 接收
            } else if (senderUserId != null && senderUserId.contains(accountIdStr)) {
                message.setDirection(1); // 发送
            } else {
                message.setDirection(2); // 默认接收
            }
            
            // 设置默认值
            message.setMessageType(1); // 默认文本消息
            message.setIsRead(0); // 默认未读
            
            // 保存原始数据
            message.setRawData(objectMapper.writeValueAsString(messageData));
            
            // 插入数据库
            try {
                int result = chatMessageMapper.insert(message);
                
                if (result > 0) {
                    log.info("【账号{}】保存聊天消息成功: messageId={}, direction={}, content={}", 
                            accountId, messageId, message.getDirection() == 1 ? "发送" : "接收", 
                            message.getContentText());
                    return true;
                } else {
                    log.debug("【账号{}】保存聊天消息失败: messageId={}", accountId, messageId);
                    return false;
                }
            } catch (org.springframework.dao.DuplicateKeyException e) {
                // 唯一索引冲突，说明消息已存在，静默跳过
                log.debug("【账号{}】消息ID冲突，跳过保存: {}", accountId, messageId);
                return true;
            } catch (Exception e) {
                // 其他数据库异常，静默跳过
                log.debug("【账号{}】保存消息时发生异常，跳过: messageId={}, error={}", 
                        accountId, messageId, e.getMessage());
                return true;
            }
            
        } catch (Exception e) {
            // 解析或处理异常，静默跳过
            log.debug("【账号{}】处理聊天消息异常，跳过保存: {}", accountId, e.getMessage());
            return false;
        }
    }
    
    @Override
    public List<XianyuChatMessage> getMessagesByAccountId(Long accountId, int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        return chatMessageMapper.findByAccountId(accountId, pageSize, offset);
    }
    
    @Override
    public List<XianyuChatMessage> getMessagesBySessionId(String sessionId) {
        return chatMessageMapper.findBySessionId(sessionId);
    }
    
    /**
     * 从Map中提取字符串值
     */
    private String extractString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }
    
    /**
     * 从Map中提取整数值
     */
    private Integer extractInteger(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * 从Map中提取长整数值
     */
    private Long extractLong(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(value.toString());
        } catch (Exception e) {
            return null;
        }
    }
}
