package com.feijimiao.xianyuassistant.model.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * 获取所有商品响应DTO
 */
@Data
public class AllItemsRespDTO {
    /**
     * 是否成功
     */
    private Boolean success;
    
    /**
     * 总页数
     */
    private Integer totalPages;
    
    /**
     * 总商品数量
     */
    private Integer totalCount;
    
    /**
     * 已保存数量
     */
    private Integer totalSaved;
    
    /**
     * 商品列表
     */
    private List<Map<String, Object>> items;
}
