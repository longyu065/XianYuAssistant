package com.feijimiao.xianyuassistant.websocket.handler;

import com.feijimiao.xianyuassistant.entity.XianyuChatMessage;
import com.feijimiao.xianyuassistant.mapper.XianyuChatMessageMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 消息发送响应处理器
 * 处理消息发送成功后的响应，保存自己发送的消息
 */
@Slf4j
@Component
public class MessageSendHandler extends AbstractLwpHandler {
    
    @Autowired(required = false)
    private XianyuChatMessageMapper chatMessageMapper;
    
    @Override
    public String getLwpPath() {
        return "/r/MessageSend/sendByReceiverScope";
    }
    
    @Override
    protected Object parseParams(String accountId, Map<String, Object> messageData) {
        MessageSendParams params = new MessageSendParams();
        
        // 获取响应码
        params.setCode(getInteger(messageData, "code"));
        
        // 获取headers
        Map<String, Object> headers = getMap(messageData, "headers");
        if (headers != null) {
            params.setMid(getString(headers, "mid"));
            params.setSid(getString(headers, "sid"));
        }
        
        // 获取body - 可能是Map或List
        Object bodyObj = messageData.get("body");
        
        // 如果body是List（发送请求格式）
        if (bodyObj instanceof java.util.List) {
            @SuppressWarnings("unchecked")
            java.util.List<Object> bodyList = (java.util.List<Object>) bodyObj;
            
            if (!bodyList.isEmpty() && bodyList.get(0) instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> messageInfo = (Map<String, Object>) bodyList.get(0);
                
                // 提取消息信息
                params.setUuid(getString(messageInfo, "uuid"));
                params.setCid(getString(messageInfo, "cid"));
                params.setConversationType(getInteger(messageInfo, "conversationType"));
                
                // 获取content
                Map<String, Object> content = getMap(messageInfo, "content");
                if (content != null) {
                    params.setContentType(getInteger(content, "contentType"));
                    
                    Map<String, Object> custom = getMap(content, "custom");
                    if (custom != null) {
                        params.setCustomData(getString(custom, "data"));
                        params.setCustomType(getInteger(custom, "type"));
                        
                        // 尝试解析custom.data中的文本内容
                        String customData = getString(custom, "data");
                        if (customData != null) {
                            try {
                                // Base64解码
                                byte[] decoded = java.util.Base64.getDecoder().decode(customData);
                                String decodedStr = new String(decoded, "UTF-8");
                                
                                // 解析JSON获取文本
                                @SuppressWarnings("unchecked")
                                Map<String, Object> decodedMap = objectMapper.readValue(decodedStr, Map.class);
                                
                                Object textObj = decodedMap.get("text");
                                if (textObj instanceof Map) {
                                    @SuppressWarnings("unchecked")
                                    Map<String, Object> textMap = (Map<String, Object>) textObj;
                                    params.setMessageText(getString(textMap, "text"));
                                }
                            } catch (Exception e) {
                                log.error("解析custom.data失败: {}", e.getMessage(), e);
                            }
                        }
                    }
                }
                
                // 获取extension
                Map<String, Object> extension = getMap(messageInfo, "extension");
                if (extension != null) {
                    params.setExtJson(getString(extension, "extJson"));
                }
                
                // 获取ctx
                Map<String, Object> ctx = getMap(messageInfo, "ctx");
                if (ctx != null) {
                    params.setAppVersion(getString(ctx, "appVersion"));
                    params.setPlatform(getString(ctx, "platform"));
                }
            }
            
            // 获取接收者信息（第二个元素）
            if (bodyList.size() > 1 && bodyList.get(1) instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> receiversInfo = (Map<String, Object>) bodyList.get(1);
                
                Object receiversObj = receiversInfo.get("actualReceivers");
                if (receiversObj instanceof java.util.List) {
                    @SuppressWarnings("unchecked")
                    java.util.List<String> receivers = (java.util.List<String>) receiversObj;
                    params.setActualReceivers(receivers);
                }
            }
        }
        // 如果body是Map（响应格式）
        else if (bodyObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> body = (Map<String, Object>) bodyObj;
            
            params.setMessageId(getString(body, "messageId"));
            params.setUuid(getString(body, "uuid"));
            params.setCreateAt(getLong(body, "createAt"));
            
            // 获取extension中的消息内容
            Map<String, Object> extension = getMap(body, "extension");
            if (extension != null) {
                params.setReminderContent(getString(extension, "reminderContent"));
                params.setReminderTitle(getString(extension, "reminderTitle"));
                params.setReminderUrl(getString(extension, "reminderUrl"));
                params.setSenderUserId(getString(extension, "senderUserId"));
                params.setAppVersion(getString(extension, "_appVersion"));
                params.setPlatform(getString(extension, "_platform"));
            }
            
            // 获取content
            Map<String, Object> content = getMap(body, "content");
            if (content != null) {
                params.setContentType(getInteger(content, "contentType"));
                
                Map<String, Object> custom = getMap(content, "custom");
                if (custom != null) {
                    params.setCustomData(getString(custom, "data"));
                    params.setCustomType(getInteger(custom, "type"));
                }
            }
        }
        
        return params;
    }
    
    @Override
    protected Object doHandle(String accountId, Object params, Map<String, Object> messageData) {
        MessageSendParams sendParams = (MessageSendParams) params;
        
        // 判断是请求还是响应
        if (sendParams.getCode() != null) {
            // 这是响应
            if (sendParams.getCode() == 200) {
                log.info("【账号{}】消息发送成功: messageId={}, content={}", 
                        accountId, sendParams.getMessageId(), sendParams.getReminderContent());
                
                // 保存发送的消息到数据库
                saveSentMessage(accountId, sendParams, messageData);
                
                return "SUCCESS";
            } else {
                log.error("【账号{}】消息发送失败: code={}", accountId, sendParams.getCode());
                return "FAILED";
            }
        } else {
            // 这是请求
            log.info("【账号{}】发送消息请求: uuid={}, cid={}, text={}, receivers={}", 
                    accountId, sendParams.getUuid(), sendParams.getCid(), 
                    sendParams.getMessageText(), sendParams.getActualReceivers());
            
            // 可以在这里记录发送请求，但通常我们只在收到响应时保存
            return "REQUEST_LOGGED";
        }
    }
    
    /**
     * 保存发送的消息到数据库
     */
    private void saveSentMessage(String accountId, MessageSendParams params, Map<String, Object> messageData) {
        if (chatMessageMapper == null) {
            return;
        }
        
        try {
            Long accountIdLong = Long.parseLong(accountId);
            
            // 使用messageId作为pnm_id
            String pnmId = params.getMessageId();
            if (pnmId == null || pnmId.isEmpty()) {
                log.debug("【账号{}】messageId为空，跳过保存", accountId);
                return;
            }
            
            // 检查是否已存在
            XianyuChatMessage existing = chatMessageMapper.findByPnmId(accountIdLong, pnmId);
            if (existing != null) {
                log.debug("【账号{}】消息已存在，跳过保存: {}", accountId, pnmId);
                return;
            }
            
            // 创建消息实体
            XianyuChatMessage message = new XianyuChatMessage();
            message.setXianyuAccountId(accountIdLong);
            message.setLwp("/r/MessageSend/sendByReceiverScope"); // 标记为发送响应
            message.setPnmId(pnmId);
            
            // 从reminderUrl中提取s_id（会话ID）
            String reminderUrl = params.getReminderUrl();
            if (reminderUrl != null && reminderUrl.contains("sid=")) {
                String[] parts = reminderUrl.split("sid=");
                if (parts.length > 1) {
                    String sid = parts[1].split("&")[0];
                    message.setSId(sid);
                }
            }
            
            // 设置消息内容
            message.setContentType(1); // 用户消息
            message.setMsgContent(params.getReminderContent());
            message.setSenderUserName(params.getReminderTitle());
            message.setSenderUserId(params.getSenderUserId());
            message.setSenderAppV(params.getAppVersion());
            message.setSenderOsType(params.getPlatform());
            message.setReminderUrl(params.getReminderUrl());
            
            // 设置时间
            message.setMessageTime(params.getCreateAt());
            
            // 保存完整消息体
            message.setCompleteMsg(objectMapper.writeValueAsString(messageData));
            
            // 插入数据库
            int result = chatMessageMapper.insert(message);
            
            if (result > 0) {
                log.info("【账号{}】保存发送消息成功: pnmId={}, content={}", 
                        accountId, pnmId, params.getReminderContent());
            }
            
        } catch (org.springframework.dao.DuplicateKeyException e) {
            log.debug("【账号{}】消息ID冲突，跳过保存", accountId);
        } catch (Exception e) {
            log.error("【账号{}】保存发送消息失败: {}", accountId, e.getMessage(), e);
        }
    }
    
    /**
     * 消息发送参数
     */
    @Data
    public static class MessageSendParams {
        // 响应字段
        private Integer code;
        private String mid;
        private String sid;
        private String messageId;
        private Long createAt;
        private String reminderContent;
        private String reminderTitle;
        private String reminderUrl;
        private String senderUserId;
        
        // 请求字段
        private String uuid;
        private String cid;
        private Integer conversationType;
        private Integer contentType;
        private String customData;
        private Integer customType;
        private String messageText;
        private String extJson;
        private String appVersion;
        private String platform;
        private java.util.List<String> actualReceivers;
    }
}
