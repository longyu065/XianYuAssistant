package com.feijimiao.xianyuassistant.websocket.handler;

import com.feijimiao.xianyuassistant.service.ChatMessageService;
import com.feijimiao.xianyuassistant.utils.MessageDecryptUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 同步包消息处理器
 * 处理 /s/para 和 /s/sync 类型的聊天消息
 */
@Slf4j
@Component
public class SyncMessageHandler extends AbstractLwpHandler {
    
    @Autowired
    private ChatMessageService chatMessageService;
    
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
        
        log.info("【账号{}】处理同步包，包含 {} 条消息, lwp={}, ChatMessageService状态: {}", 
                accountId, syncParams.getMessageCount(), lwp, 
                chatMessageService != null ? "已注入" : "未注入");
        
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
            
            // 解密消息
            String decryptedData = MessageDecryptUtils.decrypt(encryptedData);
            if (decryptedData != null) {
                log.info("【账号{}】解密消息 #{}: {}", accountId, i + 1, decryptedData);
                decryptedMessages.add(decryptedData);
                
                // 保存到数据库（传递lwp字段）
                saveMessage(accountId, decryptedData, lwp);
            }
        }
        
        return decryptedMessages;
    }
    
    @Override
    protected void postHandle(String accountId, Object result, Map<String, Object> messageData) {
        @SuppressWarnings("unchecked")
        List<String> messages = (List<String>) result;
        log.info("【账号{}】同步包处理完成，成功解密 {} 条消息", accountId, messages.size());
    }
    
    /**
     * 保存消息到数据库
     */
    private void saveMessage(String accountId, String decryptedData, String lwp) {
        log.info("【账号{}】开始保存消息: lwp={}, 数据长度={}", accountId, lwp, decryptedData != null ? decryptedData.length() : 0);
        
        if (chatMessageService == null) {
            log.warn("【账号{}】ChatMessageService未注入，无法保存消息", accountId);
            return;
        }
        
        try {
            Long accountIdLong = Long.parseLong(accountId);
            boolean saved = chatMessageService.saveChatMessageWithLwp(accountIdLong, decryptedData, lwp);
            log.info("【账号{}】消息保存结果: {}, lwp={}", accountId, saved ? "成功" : "失败", lwp);
        } catch (Exception e) {
            log.error("【账号{}】保存消息异常: lwp={}, error={}", accountId, lwp, e.getMessage(), e);
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
