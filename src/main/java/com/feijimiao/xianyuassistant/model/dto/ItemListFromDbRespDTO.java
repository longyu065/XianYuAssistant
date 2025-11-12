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
     * 商品列表
     */
    private List<XianyuGoodsInfo> items;
    
    /**
     * 商品总数
     */
    private Integer totalCount;
}
