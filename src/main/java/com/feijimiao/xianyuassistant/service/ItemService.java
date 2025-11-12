package com.feijimiao.xianyuassistant.service;

import com.feijimiao.xianyuassistant.common.ResultObject;
import com.feijimiao.xianyuassistant.model.dto.*;

/**
 * 商品服务接口
 */
public interface ItemService {

    /**
     * 获取指定页的商品信息
     *
     * @param reqDTO 请求参数
     * @return 商品列表
     */
    ResultObject<ItemListRespDTO> getItemList(ItemListReqDTO reqDTO);

    /**
     * 获取所有商品信息（自动分页）
     *
     * @param reqDTO 请求参数
     * @return 所有商品信息
     */
    ResultObject<AllItemsRespDTO> getAllItems(AllItemsReqDTO reqDTO);

    /**
     * 从数据库获取商品信息
     *
     * @param reqDTO 请求参数
     * @return 商品列表
     */
    ResultObject<ItemDbRespDTO> getItemsFromDb(ItemDbReqDTO reqDTO);
}
