package com.feijimiao.xianyuassistant.model.dto;

import lombok.Data;

/**
 * 从数据库获取商品列表请求DTO
 */
@Data
public class ItemListFromDbReqDTO {
    
    /**
     * 商品状态（0=在售, 1=已下架, 2=已售出）
     * 默认0
     */
    private Integer status = 0;
    
    /**
     * 闲鱼账号ID（可选）
     */
    private Long xianyuAccountId;
}
