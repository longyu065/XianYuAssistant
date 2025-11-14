package com.feijimiao.xianyuassistant.model.dto;

import lombok.Data;

/**
 * 消息列表查询请求DTO
 */
@Data
public class MsgListReqDTO {
    
    /**
     * 闲鱼账号ID（必选）
     */
    private Long xianyuAccountId;
    
    /**
     * 闲鱼商品ID（可选）
     */
    private String xyGoodsId;
    
    /**
     * 页码，从1开始
     * 默认1
     */
    private Integer pageNum = 1;
    
    /**
     * 每页数量
     * 默认20
     */
    private Integer pageSize = 20;
}

