package com.feijimiao.xianyuassistant.service;

import com.feijimiao.xianyuassistant.model.dto.ItemDTO;

import java.util.List;

/**
 * 商品信息服务接口
 */
public interface GoodsInfoService {
    
    /**
     * 保存或更新商品信息
     * 如果商品已存在（根据xy_good_id判断），则更新；否则新增
     *
     * @param itemDTO 商品DTO
     * @return 是否成功
     */
    boolean saveOrUpdateGoodsInfo(ItemDTO itemDTO);
    
    /**
     * 批量保存或更新商品信息
     *
     * @param itemList 商品列表
     * @return 成功保存的数量
     */
    int batchSaveOrUpdateGoodsInfo(List<ItemDTO> itemList);
    
    /**
     * 根据闲鱼商品ID查询商品信息
     *
     * @param xyGoodId 闲鱼商品ID
     * @return 商品信息
     */
    com.feijimiao.xianyuassistant.entity.XianyuGoodsInfo getByXyGoodId(String xyGoodId);
    
    /**
     * 根据状态查询商品列表
     *
     * @param status 商品状态
     * @return 商品列表
     */
    List<com.feijimiao.xianyuassistant.entity.XianyuGoodsInfo> listByStatus(Integer status);
}
