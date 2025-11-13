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
            
            // 提取pnm_id（字段1.3）
            String pnmId = extractString(messageInfo, "3");
            if (pnmId == null || pnmId.isEmpty()) {
                log.warn("pnm_id为空，跳过保存");
                return false;
            }
            message.setPnmId(pnmId);
            
            // 检查是否已存在
            try {
                XianyuChatMessage existing = chatMessageMapper.findByPnmId(accountId, pnmId);
                if (existing != null) {
                    log.debug("【账号{}】消息已存在，跳过保存: {}", accountId, pnmId);
                    return true;
                }
            } catch (Exception e) {
                log.debug("【账号{}】查询消息失败，跳过保存: {}", accountId, pnmId);
                return true;
            }
            
            // 提取s_id（字段1.2）
            message.setSId(extractString(messageInfo, "2"));
            
            // 提取时间戳（字段1.5）
            message.setMessageTime(extractLong(messageInfo, "5"));
            
            // 提取字段1.6的内容
            Object field6 = messageInfo.get("6");
            if (field6 instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> field6Map = (Map<String, Object>) field6;
                
                // 提取字段1.6.3
                Object field63 = field6Map.get("3");
                if (field63 instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> field63Map = (Map<String, Object>) field63;
                    
                    // 提取字段1.6.3.5中的contentType
                    String field635 = extractString(field63Map, "5");
                    if (field635 != null) {
                        try {
                            // 解析JSON字符串获取contentType
                            @SuppressWarnings("unchecked")
                            Map<String, Object> contentMap = objectMapper.readValue(field635, Map.class);
                            message.setContentType(extractInteger(contentMap, "contentType"));
                        } catch (Exception e) {
                            log.debug("解析contentType失败: {}", e.getMessage());
                        }
                    }
                }
            }
            
            // 提取字段1.10的内容
            Object field10 = messageInfo.get("10");
            if (field10 instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> field10Map = (Map<String, Object>) field10;
                
                // 提取各个字段
                message.setMsgContent(extractString(field10Map, "reminderContent"));
                message.setSenderUserName(extractString(field10Map, "reminderTitle"));
                message.setSenderUserId(extractString(field10Map, "senderUserId"));
                
                // 提取reminderUrl并解析商品ID
                String reminderUrl = extractString(field10Map, "reminderUrl");
                message.setReminderUrl(reminderUrl);
                if (reminderUrl != null) {
                    String goodsId = extractItemIdFromUrl(reminderUrl);
                    message.setXyGoodsId(goodsId);
                }
                
                message.setSenderAppV(extractString(field10Map, "_appVersion"));
                message.setSenderOsType(extractString(field10Map, "_platform"));
            }
            
            // 保存完整消息体
            message.setCompleteMsg(objectMapper.writeValueAsString(messageData));
            
            // 插入数据库
            try {
                int result = chatMessageMapper.insert(message);
                
                if (result > 0) {
                    log.info("【账号{}】保存聊天消息成功: pnmId={}, contentType={}, content={}", 
                            accountId, pnmId, message.getContentType(), message.getMsgContent());
                    return true;
                } else {
                    log.warn("【账号{}】保存聊天消息失败: pnmId={}", accountId, pnmId);
                    return false;
                }
            } catch (org.springframework.dao.DuplicateKeyException e) {
                log.debug("【账号{}】消息ID冲突，跳过保存: {}", accountId, pnmId);
                return true;
            } catch (Exception e) {
                log.error("【账号{}】保存消息时发生异常: pnmId={}, error={}", 
                        accountId, pnmId, e.getMessage(), e);
                return false;
            }
            
        } catch (Exception e) {
            log.error("【账号{}】处理聊天消息异常: {}", accountId, e.getMessage(), e);
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
        return chatMessageMapper.findBySId(sessionId);
    }
    
    @Override
    public boolean saveChatMessageWithLwp(Long accountId, String decryptedData, String lwp) {
        try {
            // 解析JSON
            @SuppressWarnings("unchecked")
            Map<String, Object> data = objectMapper.readValue(decryptedData, Map.class);
            return saveChatMessageFromMapWithLwp(accountId, data, lwp);
        } catch (Exception e) {
            log.error("保存聊天消息失败", e);
            return false;
        }
    }
    
    /**
     * 保存聊天消息（带lwp字段）
     */
    private boolean saveChatMessageFromMapWithLwp(Long accountId, Map<String, Object> messageData, String lwp) {
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
            
            // 设置lwp字段
            message.setLwp(lwp);
            
            // 提取pnm_id（字段1.3）
            String pnmId = extractString(messageInfo, "3");
            if (pnmId == null || pnmId.isEmpty()) {
                log.warn("pnm_id为空，跳过保存");
                return false;
            }
            message.setPnmId(pnmId);
            
            // 检查是否已存在
            try {
                XianyuChatMessage existing = chatMessageMapper.findByPnmId(accountId, pnmId);
                if (existing != null) {
                    log.debug("【账号{}】消息已存在，跳过保存: {}", accountId, pnmId);
                    return true;
                }
            } catch (Exception e) {
                log.debug("【账号{}】查询消息失败，跳过保存: {}", accountId, pnmId);
                return true;
            }
            
            // 提取s_id（字段1.2）
            message.setSId(extractString(messageInfo, "2"));
            
            // 提取时间戳（字段1.5）
            message.setMessageTime(extractLong(messageInfo, "5"));
            
            // 提取字段1.6的内容
            Object field6 = messageInfo.get("6");
            if (field6 instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> field6Map = (Map<String, Object>) field6;
                
                // 提取字段1.6.3
                Object field63 = field6Map.get("3");
                if (field63 instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> field63Map = (Map<String, Object>) field63;
                    
                    // 提取字段1.6.3.5中的contentType
                    String field635 = extractString(field63Map, "5");
                    if (field635 != null) {
                        try {
                            // 解析JSON字符串获取contentType
                            @SuppressWarnings("unchecked")
                            Map<String, Object> contentMap = objectMapper.readValue(field635, Map.class);
                            message.setContentType(extractInteger(contentMap, "contentType"));
                        } catch (Exception e) {
                            log.debug("解析contentType失败: {}", e.getMessage());
                        }
                    }
                }
            }
            
            // 提取字段1.10的内容
            Object field10 = messageInfo.get("10");
            if (field10 instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> field10Map = (Map<String, Object>) field10;
                
                // 提取各个字段
                message.setMsgContent(extractString(field10Map, "reminderContent"));
                message.setSenderUserName(extractString(field10Map, "reminderTitle"));
                message.setSenderUserId(extractString(field10Map, "senderUserId"));
                
                // 提取reminderUrl并解析商品ID
                String reminderUrl = extractString(field10Map, "reminderUrl");
                message.setReminderUrl(reminderUrl);
                if (reminderUrl != null) {
                    String goodsId = extractItemIdFromUrl(reminderUrl);
                    message.setXyGoodsId(goodsId);
                }
                
                message.setSenderAppV(extractString(field10Map, "_appVersion"));
                message.setSenderOsType(extractString(field10Map, "_platform"));
            }
            
            // 保存完整消息体
            message.setCompleteMsg(objectMapper.writeValueAsString(messageData));
            
            // 插入数据库
            try {
                int result = chatMessageMapper.insert(message);
                
                if (result > 0) {
                    log.info("【账号{}】保存聊天消息成功: lwp={}, pnmId={}, contentType={}, content={}", 
                            accountId, lwp, pnmId, message.getContentType(), message.getMsgContent());
                    return true;
                } else {
                    log.warn("【账号{}】保存聊天消息失败: pnmId={}", accountId, pnmId);
                    return false;
                }
            } catch (org.springframework.dao.DuplicateKeyException e) {
                log.debug("【账号{}】消息ID冲突，跳过保存: {}", accountId, pnmId);
                return true;
            } catch (Exception e) {
                log.error("【账号{}】保存消息时发生异常: pnmId={}, error={}", 
                        accountId, pnmId, e.getMessage(), e);
                return false;
            }
            
        } catch (Exception e) {
            log.error("【账号{}】处理聊天消息异常: {}", accountId, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 从reminder_url中提取itemId（商品ID）
     * 例如：leamarket://message_chat?itemId=926462531165&peerUserId=2218021801256
     * 提取出：926462531165
     */
    private String extractItemIdFromUrl(String url) {
        if (url == null || url.isEmpty()) {
            return null;
        }
        
        try {
            // 查找itemId参数
            int itemIdIndex = url.indexOf("itemId=");
            if (itemIdIndex == -1) {
                return null;
            }
            
            // 提取itemId的值
            int startIndex = itemIdIndex + 7; // "itemId=".length()
            int endIndex = url.indexOf("&", startIndex);
            
            if (endIndex == -1) {
                // 如果没有&，说明itemId是最后一个参数
                return url.substring(startIndex);
            } else {
                return url.substring(startIndex, endIndex);
            }
        } catch (Exception e) {
            log.debug("解析itemId失败: url={}, error={}", url, e.getMessage());
            return null;
        }
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
