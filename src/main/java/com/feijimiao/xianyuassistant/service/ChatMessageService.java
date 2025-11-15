package com.feijimiao.xianyuassistant.service;

import com.feijimiao.xianyuassistant.common.ResultObject;
import com.feijimiao.xianyuassistant.entity.XianyuChatMessage;
import com.feijimiao.xianyuassistant.controller.dto.MsgListReqDTO;
import com.feijimiao.xianyuassistant.controller.dto.MsgListRespDTO;
import java.util.List;
import java.util.Map;

/**
 * 聊天消息服务接口
 */
public interface ChatMessageService {
    
    /**
     * 保存聊天消息
     * 
     * @param accountId 账号ID
     * @param decryptedData 解密后的消息数据
     * @return 是否保存成功
     */
    boolean saveChatMessage(Long accountId, String decryptedData);
    
    /**
     * 保存聊天消息（从Map）
     * 
     * @param accountId 账号ID
     * @param messageData 消息数据Map
     * @return 是否保存成功
     */
    boolean saveChatMessageFromMap(Long accountId, Map<String, Object> messageData);
    
    /**
     * 查询账号的聊天消息
     * 
     * @param accountId 账号ID
     * @param page 页码（从1开始）
     * @param pageSize 每页大小
     * @return 消息列表
     */
    List<XianyuChatMessage> getMessagesByAccountId(Long accountId, int page, int pageSize);
    
    /**
     * 查询会话的聊天消息
     * 
     * @param sessionId 会话ID
     * @return 消息列表
     */
    List<XianyuChatMessage> getMessagesBySessionId(String sessionId);
    
    /**
     * 保存聊天消息（带lwp字段）
     * 
     * @param accountId 账号ID
     * @param decryptedData 解密后的消息数据
     * @param lwp WebSocket消息路径
     * @return 是否保存成功
     */
    boolean saveChatMessageWithLwp(Long accountId, String decryptedData, String lwp);
    
    /**
     * 分页查询消息列表
     * 
     * @param reqDTO 查询请求参数
     * @return 消息列表响应
     */
    ResultObject<MsgListRespDTO> getMessageList(MsgListReqDTO reqDTO);
}
