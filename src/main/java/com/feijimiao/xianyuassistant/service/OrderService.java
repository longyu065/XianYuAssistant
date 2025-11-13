package com.feijimiao.xianyuassistant.service;

/**
 * 订单服务接口
 */
public interface OrderService {
    
    /**
     * 确认发货
     *
     * @param accountId 账号ID
     * @param orderId 订单ID
     * @return 是否成功
     */
    boolean confirmShipment(Long accountId, String orderId);
}
