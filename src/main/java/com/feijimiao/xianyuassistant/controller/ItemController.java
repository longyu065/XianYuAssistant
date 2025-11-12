package com.feijimiao.xianyuassistant.controller;

import com.feijimiao.xianyuassistant.common.ResultObject;
import com.feijimiao.xianyuassistant.model.dto.*;
import com.feijimiao.xianyuassistant.service.ItemService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 商品管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/items")
@CrossOrigin(origins = "*")
public class ItemController {

    @Autowired
    private ItemService itemService;

    /**
     * 刷新商品数据
     * 从闲鱼API获取最新商品信息并更新到数据库
     *
     * @param reqDTO 请求参数
     * @return 更新成功的商品ID列表
     */
    @PostMapping("/refresh")
    public ResultObject<RefreshItemsRespDTO> refreshItems(@RequestBody AllItemsReqDTO reqDTO) {
        try {
            log.info("刷新商品数据请求: cookieId={}", reqDTO.getCookieId());
            return itemService.refreshItems(reqDTO);
        } catch (Exception e) {
            log.error("刷新商品数据失败", e);
            return ResultObject.failed("刷新商品数据失败: " + e.getMessage());
        }
    }

    /**
     * 从数据库获取商品列表
     *
     * @param reqDTO 请求参数
     * @return 商品列表
     */
    @PostMapping("/list")
    public ResultObject<ItemListFromDbRespDTO> getItemsFromDb(@RequestBody ItemListFromDbReqDTO reqDTO) {
        try {
            log.info("从数据库获取商品列表: status={}", reqDTO.getStatus());
            return itemService.getItemsFromDb(reqDTO);
        } catch (Exception e) {
            log.error("获取数据库商品失败", e);
            return ResultObject.failed("获取数据库商品失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取商品详情
     *
     * @param reqDTO 请求参数
     * @return 商品详情
     */
    @PostMapping("/detail")
    public ResultObject<ItemDetailRespDTO> getItemDetail(@RequestBody ItemDetailReqDTO reqDTO) {
        try {
            log.info("获取商品详情: xyGoodId={}", reqDTO.getXyGoodId());
            return itemService.getItemDetail(reqDTO);
        } catch (Exception e) {
            log.error("获取商品详情失败", e);
            return ResultObject.failed("获取商品详情失败: " + e.getMessage());
        }
    }
}
