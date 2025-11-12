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
     * 获取指定页的商品信息
     *
     * @param reqDTO 请求参数
     * @return 商品列表
     */
    @PostMapping("/list")
    public ResultObject<ItemListRespDTO> getItemList(@RequestBody ItemListReqDTO reqDTO) {
        try {
            log.info("获取商品列表请求: {}", reqDTO);
            return itemService.getItemList(reqDTO);
        } catch (Exception e) {
            log.error("获取商品列表失败", e);
            return ResultObject.failed("获取商品列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取所有商品信息（自动分页）
     *
     * @param reqDTO 请求参数
     * @return 所有商品信息
     */
    @PostMapping("/all")
    public ResultObject<AllItemsRespDTO> getAllItems(@RequestBody AllItemsReqDTO reqDTO) {
        try {
            log.info("获取所有商品请求: {}", reqDTO);
            return itemService.getAllItems(reqDTO);
        } catch (Exception e) {
            log.error("获取所有商品失败", e);
            return ResultObject.failed("获取所有商品失败: " + e.getMessage());
        }
    }

    /**
     * 从数据库获取商品信息
     *
     * @param reqDTO 请求参数
     * @return 商品列表
     */
    @PostMapping("/db")
    public ResultObject<ItemDbRespDTO> getItemsFromDb(@RequestBody ItemDbReqDTO reqDTO) {
        try {
            log.info("从数据库获取商品请求: {}", reqDTO);
            return itemService.getItemsFromDb(reqDTO);
        } catch (Exception e) {
            log.error("获取数据库商品失败", e);
            return ResultObject.failed("获取数据库商品失败: " + e.getMessage());
        }
    }
}
