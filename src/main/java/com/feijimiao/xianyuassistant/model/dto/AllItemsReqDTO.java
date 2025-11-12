package com.feijimiao.xianyuassistant.model.dto;

import lombok.Data;

/**
 * 获取所有商品请求DTO
 */
@Data
public class AllItemsReqDTO {
    /**
     * 账号ID
     */
    private String cookieId;
    
    /**
     * 每页数量，默认20
     */
    private Integer pageSize = 20;
    
    /**
     * 最大页数限制
     */
    private Integer maxPages;
}
