package com.feijimiao.xianyuassistant.websocket.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.feijimiao.xianyuassistant.entity.XianyuChatMessage;
import com.feijimiao.xianyuassistant.event.chatMessageEvent.ChatMessageData;
import com.feijimiao.xianyuassistant.event.chatMessageEvent.ChatMessageReceivedEvent;
import com.feijimiao.xianyuassistant.utils.MessageDecryptUtils;
import org.springframework.beans.BeanUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 同步包消息处理器
 * 处理 /s/para 和 /s/sync 类型的聊天消息
 * 
 * <p>职责：</p>
 * <ul>
 *   <li>解密同步包中的加密消息</li>
 *   <li>解析消息字段，构建 XianyuChatMessage 对象</li>
 *   <li>发布 ChatMessageReceivedEvent 事件</li>
 * </ul>
 */
@Slf4j
@Component
public class SyncMessageHandler extends AbstractLwpHandler {
    
    @Autowired
    private ApplicationEventPublisher eventPublisher;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    public String getLwpPath() {
        return "/s/para"; // 也支持 /s/sync
    }
    
    @Override
    protected Object parseParams(String accountId, Map<String, Object> messageData) {
        try {
            SyncMessageParams params = new SyncMessageParams();
            
            // 获取body
            Map<String, Object> body = getMap(messageData, "body");
            if (body == null) {
                return null;
            }
            
            // 获取syncPushPackage
            Map<String, Object> syncPushPackage = getMap(body, "syncPushPackage");
            if (syncPushPackage == null) {
                return null;
            }
            
            // 获取data列表
            List<Object> dataList = getList(syncPushPackage, "data");
            if (dataList == null || dataList.isEmpty()) {
                return null;
            }
            
            params.setDataList(dataList);
            params.setMessageCount(dataList.size());
            
            return params;
            
        } catch (Exception e) {
            log.error("【账号{}】解析同步包参数失败", accountId, e);
            return null;
        }
    }
    
    @Override
    protected Object doHandle(String accountId, Object params, Map<String, Object> messageData) {
        SyncMessageParams syncParams = (SyncMessageParams) params;
        List<String> decryptedMessages = new ArrayList<>();
        
        // 获取lwp字段
        String lwp = getString(messageData, "lwp");
        
        log.info("【账号{}】收到闲鱼原始消息: lwp={}, messageCount={}", accountId, lwp, syncParams.getMessageCount());
        
        // 处理每条加密消息
        for (int i = 0; i < syncParams.getDataList().size(); i++) {
            Object item = syncParams.getDataList().get(i);
            if (!(item instanceof Map)) {
                continue;
            }
            
            @SuppressWarnings("unchecked")
            Map<String, Object> syncData = (Map<String, Object>) item;
            
            String encryptedData = getString(syncData, "data");
            if (encryptedData == null) {
                continue;
            }
            
            log.info("【账号{}】加密消息[{}]: {}", accountId, i, encryptedData);
            
            // 解密消息
            String decryptedData = MessageDecryptUtils.decrypt(encryptedData);
            if (decryptedData != null) {
                log.info("【账号{}】解密消息[{}]: {}", accountId, i, decryptedData);
                decryptedMessages.add(decryptedData);
                
                // 解析并发布事件
                parseAndPublishEvent(accountId, decryptedData, lwp);
            } else {
                log.warn("【账号{}】消息解密失败[{}]", accountId, i);
            }
        }
        
        return decryptedMessages;
    }
    
    @Override
    protected void postHandle(String accountId, Object result, Map<String, Object> messageData) {
        // 处理完成，不需要额外日志
    }
    
    /**
     * 解析消息并发布事件
     * 
     * @param accountId 账号ID
     * @param decryptedData 解密后的JSON数据
     * @param lwp WebSocket消息路径
     */
    private void parseAndPublishEvent(String accountId, String decryptedData, String lwp) {
        try {
            // 解析JSON
            @SuppressWarnings("unchecked")
            Map<String, Object> data = objectMapper.readValue(decryptedData, Map.class);
            
            // 检查消息类型
            Object typeObj = data.get("2");
            if (typeObj != null && "2".equals(typeObj.toString())) {
                // 已读回执，不处理
                return;
            }
            
            // 检查是否是聊天消息
            Object field1 = data.get("1");
            if (!(field1 instanceof Map)) {
                return;
            }
            
            @SuppressWarnings("unchecked")
            Map<String, Object> messageInfo = (Map<String, Object>) field1;
            
            // 创建消息实体
            XianyuChatMessage message = new XianyuChatMessage();
            Long accountIdLong = Long.parseLong(accountId);
            message.setXianyuAccountId(accountIdLong);
            message.setLwp(lwp);
            
            // 提取pnm_id（字段1.3）
            String pnmId = extractString(messageInfo, "3");
            if (pnmId == null || pnmId.isEmpty()) {
                return;
            }
            message.setPnmId(pnmId);
            
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
            message.setCompleteMsg(decryptedData);
            
            // 发布消息接收事件
            publishChatMessageReceivedEvent(message);
            
        } catch (Exception e) {
            log.error("【账号{}】解析消息异常: lwp={}, error={}", accountId, lwp, e.getMessage(), e);
        }
    }
    
    /**
     * 发布聊天消息接收事件
     */
    private void publishChatMessageReceivedEvent(XianyuChatMessage message) {
        try {
            // 转换为 ChatMessageData
            ChatMessageData messageData = new ChatMessageData();
            BeanUtils.copyProperties(message, messageData);
            
            // 从完整消息中提取订单ID
            String orderId = extractOrderIdFromMessage(message.getCompleteMsg());
            messageData.setOrderId(orderId);
            
            log.info("【账号{}】准备发布ChatMessageReceivedEvent事件，完整消息对象: \n" +
                    "  pnmId={}\n" +
                    "  sId={}\n" +
                    "  lwp={}\n" +
                    "  contentType={}\n" +
                    "  msgContent={}\n" +
                    "  xyGoodsId={}\n" +
                    "  reminderUrl={}\n" +
                    "  senderUserId={}\n" +
                    "  senderUserName={}\n" +
                    "  senderAppV={}\n" +
                    "  senderOsType={}\n" +
                    "  messageTime={}\n" +
                    "  orderId={}", 
                    message.getXianyuAccountId(),
                    message.getPnmId(),
                    message.getSId(),
                    message.getLwp(),
                    message.getContentType(),
                    message.getMsgContent(),
                    message.getXyGoodsId(),
                    message.getReminderUrl(),
                    message.getSenderUserId(),
                    message.getSenderUserName(),
                    message.getSenderAppV(),
                    message.getSenderOsType(),
                    message.getMessageTime(),
                    orderId);
            
            ChatMessageReceivedEvent event = new ChatMessageReceivedEvent(this, messageData);
            eventPublisher.publishEvent(event);
            
            log.info("【账号{}】ChatMessageReceivedEvent事件已发布: pnmId={}, orderId={}", 
                    message.getXianyuAccountId(), message.getPnmId(), orderId);
        } catch (Exception e) {
            log.error("【账号{}】发布消息接收事件失败: pnmId={}", 
                    message.getXianyuAccountId(), message.getPnmId(), e);
        }
    }
    
    /**
     * 从完整消息中提取订单ID
     */
    private String extractOrderIdFromMessage(String completeMsg) {
        try {
            if (completeMsg == null || completeMsg.isEmpty()) {
                return null;
            }
            
            @SuppressWarnings("unchecked")
            Map<String, Object> data = objectMapper.readValue(completeMsg, Map.class);
            
            // 从 reminderUrl 中提取订单ID
            // 路径: 1.6.10.reminderUrl
            Object level1 = data.get("1");
            if (level1 instanceof Map) {
                Object level6 = ((Map<?, ?>) level1).get("6");
                if (level6 instanceof Map) {
                    Object level10 = ((Map<?, ?>) level6).get("10");
                    if (level10 instanceof Map) {
                        String reminderUrl = (String) ((Map<?, ?>) level10).get("reminderUrl");
                        if (reminderUrl != null && reminderUrl.contains("id=")) {
                            // 提取 id 参数
                            String[] parts = reminderUrl.split("[?&]");
                            for (String part : parts) {
                                if (part.startsWith("id=")) {
                                    return part.substring(3);
                                }
                            }
                        }
                    }
                }
            }
            
            return null;
        } catch (Exception e) {
            log.warn("提取订单ID失败", e);
            return null;
        }
    }
    
    /**
     * 从reminder_url中提取itemId（商品ID）
     */
    private String extractItemIdFromUrl(String url) {
        if (url == null || url.isEmpty()) {
            return null;
        }
        
        try {
            int itemIdIndex = url.indexOf("itemId=");
            if (itemIdIndex == -1) {
                return null;
            }
            
            int startIndex = itemIdIndex + 7;
            int endIndex = url.indexOf("&", startIndex);
            
            if (endIndex == -1) {
                return url.substring(startIndex);
            } else {
                return url.substring(startIndex, endIndex);
            }
        } catch (Exception e) {
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
    
    /**
     * 同步包消息参数
     */
    @Data
    public static class SyncMessageParams {
        private List<Object> dataList;
        private int messageCount;
    }
}
