package com.feijimiao.xianyuassistant.entity;

import lombok.Data;

/**
 * 商品自动发货配置实体类
 */
@Data
public class XianyuGoodsAutoDeliveryConfig {
    
    /**
     * 主键ID
     */
    private Long id;
    
    /**
     * 闲鱼账号ID
     */
    private Long xianyuAccountId;
    
    /**
     * 本地闲鱼商品ID
     */
    private Long xianyuGoodsId;
    
    /**
     * 闲鱼的商品ID
     */
    private String xyGoodsId;
    
    /**
     * 发货类型（1-文本，2-自定义）
     */
    private Integer type;
    
    /**
     * 自动发货的文本内容
     */
    private String autoDeliveryContent;
    
    /**
     * 创建时间
     */
    private String createTime;
    
    /**
     * 更新时间
     */
    private String updateTime;
}
