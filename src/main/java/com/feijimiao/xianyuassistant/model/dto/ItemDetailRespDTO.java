package com.feijimiao.xianyuassistant.model.dto;

import com.feijimiao.xianyuassistant.entity.XianyuGoodsInfo;
import lombok.Data;

/**
 * 获取商品详情响应DTO
 */
@Data
public class ItemDetailRespDTO {
    
    /**
     * 商品信息（旧版，保留兼容性）
     */
    private XianyuGoodsInfo item;
    
    /**
     * 商品信息（包含配置信息）
     */
    private ItemWithConfigDTO itemWithConfig;
}
