package com.feijimiao.xianyuassistant.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 闲鱼聊天消息实体
 */
@Data
public class XianyuChatMessage {
    
    private Long id;
    
    // 关联信息
    private Long xianyuAccountId;
    private String sessionId;
    
    // 消息基本信息
    private String messageId;
    private Integer messageType;
    private Integer direction;  // 1=发送 2=接收
    
    // 用户信息
    private String senderUserId;
    private String senderNickname;
    private String receiverUserId;
    private String receiverNickname;
    
    // 消息内容
    private Integer contentType;
    private String contentText;
    private String contentJson;
    
    // 商品信息
    private String itemId;
    private String itemTitle;
    
    // 消息状态
    private Integer isRead;
    private LocalDateTime readTime;
    
    // 扩展信息
    private String rawData;
    private String extraInfo;
    
    // 时间信息
    private Long messageTime;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}
