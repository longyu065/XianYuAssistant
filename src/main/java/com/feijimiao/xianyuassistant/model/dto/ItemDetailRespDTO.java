package com.feijimiao.xianyuassistant.model.dto;

import com.feijimiao.xianyuassistant.entity.XianyuGoodsInfo;
import lombok.Data;

/**
 * 获取商品详情响应DTO
 */
@Data
public class ItemDetailRespDTO {
    
    /**
     * 商品信息
     */
    private XianyuGoodsInfo item;
}
