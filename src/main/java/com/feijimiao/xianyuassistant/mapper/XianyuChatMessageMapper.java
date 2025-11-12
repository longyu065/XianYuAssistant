package com.feijimiao.xianyuassistant.mapper;

import com.feijimiao.xianyuassistant.entity.XianyuChatMessage;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 闲鱼聊天消息Mapper
 */
@Mapper
public interface XianyuChatMessageMapper {
    
    /**
     * 插入聊天消息
     */
    @Insert("INSERT INTO xianyu_chat_message (" +
            "xianyu_account_id, session_id, message_id, message_type, direction, " +
            "sender_user_id, sender_nickname, receiver_user_id, receiver_nickname, " +
            "content_type, content_text, content_json, " +
            "item_id, item_title, is_read, raw_data, message_time" +
            ") VALUES (" +
            "#{xianyuAccountId}, #{sessionId}, #{messageId}, #{messageType}, #{direction}, " +
            "#{senderUserId}, #{senderNickname}, #{receiverUserId}, #{receiverNickname}, " +
            "#{contentType}, #{contentText}, #{contentJson}, " +
            "#{itemId}, #{itemTitle}, #{isRead}, #{rawData}, #{messageTime}" +
            ")")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(XianyuChatMessage message);
    
    /**
     * 根据消息ID查询（防止重复）
     */
    @Select("SELECT * FROM xianyu_chat_message " +
            "WHERE xianyu_account_id = #{accountId} AND message_id = #{messageId}")
    XianyuChatMessage findByMessageId(@Param("accountId") Long accountId, 
                                      @Param("messageId") String messageId);
    
    /**
     * 查询账号的所有消息
     */
    @Select("SELECT * FROM xianyu_chat_message " +
            "WHERE xianyu_account_id = #{accountId} " +
            "ORDER BY message_time DESC " +
            "LIMIT #{limit} OFFSET #{offset}")
    List<XianyuChatMessage> findByAccountId(@Param("accountId") Long accountId,
                                            @Param("limit") int limit,
                                            @Param("offset") int offset);
    
    /**
     * 查询会话的消息
     */
    @Select("SELECT * FROM xianyu_chat_message " +
            "WHERE session_id = #{sessionId} " +
            "ORDER BY message_time ASC")
    List<XianyuChatMessage> findBySessionId(@Param("sessionId") String sessionId);
}
