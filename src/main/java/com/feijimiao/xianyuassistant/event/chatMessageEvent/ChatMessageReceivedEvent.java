package com.feijimiao.xianyuassistant.event.chatMessageEvent;

import com.feijimiao.xianyuassistant.entity.XianyuChatMessage;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 聊天消息接收事件
 * 
 * <p>当WebSocket接收到消息并解析后触发此事件（消息尚未入库）</p>
 * 
 * <p>此事件采用一对多广播模式，多个监听器可以同时监听此事件：</p>
 * <ul>
 *   <li>ChatMessagePersistenceListener - 负责将消息异步保存到数据库</li>
 *   <li>AutoDeliveryTriggerListener - 负责判断是否需要触发自动发货</li>
 *   <li>其他业务监听器 - 可以根据需要添加更多监听器</li>
 * </ul>
 * 
 * <p>所有监听器使用@Async异步执行，互不阻塞</p>
 * 
 * @author feijimiao
 * @since 1.0
 */
@Getter
public class ChatMessageReceivedEvent extends ApplicationEvent {
    
    /**
     * 解析后的聊天消息对象
     * 
     * <p>注意：此时消息尚未入库，id字段为null</p>
     */
    private final XianyuChatMessage chatMessage;
    
    /**
     * 构造函数
     * 
     * @param source 事件源（通常是发布事件的Service）
     * @param chatMessage 解析后的聊天消息对象
     */
    public ChatMessageReceivedEvent(Object source, XianyuChatMessage chatMessage) {
        super(source);
        this.chatMessage = chatMessage;
    }
}
