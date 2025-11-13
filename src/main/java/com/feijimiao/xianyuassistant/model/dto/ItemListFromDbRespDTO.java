package com.feijimiao.xianyuassistant.model.dto;

import com.feijimiao.xianyuassistant.entity.XianyuGoodsInfo;
import lombok.Data;

import java.util.List;

/**
 * 从数据库获取商品列表响应DTO
 */
@Data
public class ItemListFromDbRespDTO {
    
    /**
     * 商品列表（旧版，保留兼容性）
     */
    private List<XianyuGoodsInfo> items;
    
    /**
     * 商品列表（包含配置信息）
     */
    private List<ItemWithConfigDTO> itemsWithConfig;
    
    /**
     * 商品总数
     */
    private Integer totalCount;
}
