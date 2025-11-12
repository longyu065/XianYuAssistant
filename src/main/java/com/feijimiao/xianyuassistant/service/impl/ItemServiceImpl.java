package com.feijimiao.xianyuassistant.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.feijimiao.xianyuassistant.common.ResultObject;
import com.feijimiao.xianyuassistant.entity.XianyuGoodsInfo;
import com.feijimiao.xianyuassistant.model.dto.*;
import com.feijimiao.xianyuassistant.service.ItemService;
import com.feijimiao.xianyuassistant.utils.XianyuApiUtils;
import com.feijimiao.xianyuassistant.utils.XianyuSignUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 商品服务实现类
 */
@Slf4j
@Service
public class ItemServiceImpl implements ItemService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Autowired
    private com.feijimiao.xianyuassistant.service.AccountService accountService;
    
    @Autowired
    private com.feijimiao.xianyuassistant.service.GoodsInfoService goodsInfoService;

    /**
     * 获取指定页的商品信息（内部方法）
     */
    private ResultObject<ItemListRespDTO> getItemList(ItemListReqDTO reqDTO) {
        try {
            log.info("开始获取商品列表: {}", reqDTO);

            // 从数据库获取Cookie
            String cookiesStr = getCookieFromDb(reqDTO.getCookieId());
            if (cookiesStr == null || cookiesStr.isEmpty()) {
                log.error("未找到账号Cookie: cookieId={}", reqDTO.getCookieId());
                return ResultObject.failed("未找到账号Cookie");
            }
            log.info("Cookie获取成功，长度: {}", cookiesStr.length());

            // 检查Cookie中是否包含必需的token
            Map<String, String> cookies = XianyuSignUtils.parseCookies(cookiesStr);
            if (!cookies.containsKey("_m_h5_tk") || cookies.get("_m_h5_tk").isEmpty()) {
                log.error("Cookie中缺少_m_h5_tk字段！请重新登录");
                return ResultObject.failed("Cookie中缺少_m_h5_tk，请重新登录");
            }
            
            // 构建请求数据
            Map<String, Object> dataMap = new HashMap<>();
            dataMap.put("needGroupInfo", false);
            dataMap.put("pageNumber", reqDTO.getPageNumber());
            dataMap.put("pageSize", reqDTO.getPageSize());
            dataMap.put("groupName", "在售");
            dataMap.put("groupId", "58877261");
            dataMap.put("defaultGroup", true);
            dataMap.put("userId", cookies.get("unb"));
            
            log.info("调用商品列表API: pageNumber={}, pageSize={}", reqDTO.getPageNumber(), reqDTO.getPageSize());
            
            // 使用工具类调用API
            String response = XianyuApiUtils.callApi(
                "mtop.idle.web.xyh.item.list",
                dataMap,
                cookiesStr,
                "a21ybx.im.0.0",
                "a21ybx.collection.menu.1.272b5141NafCNK"
            );
            
            if (response == null) {
                log.error("API调用失败：响应为空");
                return ResultObject.failed("请求闲鱼API失败");
            }
            
            log.info("API调用成功，响应长度: {}", response.length());

            // 解析响应
            log.info("开始解析响应JSON...");
            @SuppressWarnings("unchecked")
            Map<String, Object> responseMap = objectMapper.readValue(response, Map.class);
            log.info("JSON解析成功，响应字段: {}", responseMap.keySet());
            
            ItemListRespDTO respDTO = parseItemListResponse(responseMap, reqDTO.getPageNumber(), reqDTO.getPageSize());
            log.info("响应解析完成，success={}, 商品数量={}", respDTO.getSuccess(), respDTO.getCurrentCount());
            
            if (respDTO.getSuccess()) {
                log.info("获取商品列表成功: cookieId={}, 商品数量={}", 
                        reqDTO.getCookieId(), respDTO.getCurrentCount());
                
                // 保存商品信息到数据库
                if (respDTO.getItems() != null && !respDTO.getItems().isEmpty()) {
                    try {
                        int savedCount = goodsInfoService.batchSaveOrUpdateGoodsInfo(respDTO.getItems());
                        log.info("商品信息已保存到数据库: 成功数量={}", savedCount);
                    } catch (Exception e) {
                        log.error("保存商品信息到数据库失败", e);
                        // 不影响主流程，继续返回结果
                    }
                }
                
                return ResultObject.success(respDTO);
            } else {
                log.error("获取商品列表失败: success=false");
                return ResultObject.failed("获取商品列表失败");
            }
        } catch (Exception e) {
            log.error("获取商品列表异常: cookieId={}", reqDTO.getCookieId(), e);
            return ResultObject.failed("获取商品列表异常: " + e.getMessage());
        }
    }

    @Override
    public ResultObject<RefreshItemsRespDTO> refreshItems(AllItemsReqDTO reqDTO) {
        try {
            log.info("开始刷新商品数据: cookieId={}", reqDTO.getCookieId());

            RefreshItemsRespDTO respDTO = new RefreshItemsRespDTO();
            respDTO.setSuccess(false);
            respDTO.setUpdatedItemIds(new ArrayList<>());
            
            List<ItemDTO> allItems = new ArrayList<>();
            int pageNumber = 1;

            // 自动分页获取所有商品
            while (true) {
                // 检查是否达到最大页数
                if (reqDTO.getMaxPages() != null && pageNumber > reqDTO.getMaxPages()) {
                    log.info("达到最大页数限制: {}", reqDTO.getMaxPages());
                    break;
                }

                // 获取当前页
                ItemListReqDTO pageReqDTO = new ItemListReqDTO();
                pageReqDTO.setCookieId(reqDTO.getCookieId());
                pageReqDTO.setPageNumber(pageNumber);
                pageReqDTO.setPageSize(reqDTO.getPageSize());

                ResultObject<ItemListRespDTO> pageResult = getItemList(pageReqDTO);
                
                if (pageResult.getCode() != 200 || pageResult.getData() == null || !pageResult.getData().getSuccess()) {
                    log.error("获取第{}页失败", pageNumber);
                    break;
                }

                ItemListRespDTO pageData = pageResult.getData();
                if (pageData.getItems() == null || pageData.getItems().isEmpty()) {
                    log.info("第{}页没有数据，刷新完成", pageNumber);
                    break;
                }

                allItems.addAll(pageData.getItems());
                log.info("第{}页获取到{}个商品", pageNumber, pageData.getItems().size());

                // 如果当前页商品数量少于页面大小，说明已经是最后一页
                if (pageData.getItems().size() < reqDTO.getPageSize()) {
                    log.info("第{}页商品数量({})少于页面大小({})，刷新完成", 
                            pageNumber, pageData.getItems().size(), reqDTO.getPageSize());
                    break;
                }

                pageNumber++;
                
                // 添加延迟避免请求过快
                Thread.sleep(1000);
            }

            // 批量保存到数据库
            respDTO.setTotalCount(allItems.size());
            
            if (!allItems.isEmpty()) {
                // 保存商品并收集成功的商品ID
                for (ItemDTO item : allItems) {
                    try {
                        if (goodsInfoService.saveOrUpdateGoodsInfo(item)) {
                            if (item.getDetailParams() != null && item.getDetailParams().getItemId() != null) {
                                respDTO.getUpdatedItemIds().add(item.getDetailParams().getItemId());
                            }
                        }
                    } catch (Exception e) {
                        log.error("保存商品失败: itemId={}", 
                                item.getDetailParams() != null ? item.getDetailParams().getItemId() : "null", e);
                    }
                }
                
                respDTO.setSuccessCount(respDTO.getUpdatedItemIds().size());
                respDTO.setSuccess(true);
                respDTO.setMessage("刷新成功");
                
                log.info("刷新商品数据完成: cookieId={}, 总数={}, 成功={}", 
                        reqDTO.getCookieId(), respDTO.getTotalCount(), respDTO.getSuccessCount());
            } else {
                respDTO.setSuccessCount(0);
                respDTO.setMessage("没有获取到商品数据");
                log.warn("刷新商品数据完成，但没有获取到任何商品");
            }

            return ResultObject.success(respDTO);
        } catch (Exception e) {
            log.error("刷新商品数据异常: cookieId={}", reqDTO.getCookieId(), e);
            return ResultObject.failed("刷新商品数据异常: " + e.getMessage());
        }
    }
    
    @Override
    public ResultObject<ItemListFromDbRespDTO> getItemsFromDb(ItemListFromDbReqDTO reqDTO) {
        try {
            log.info("从数据库获取商品列表: status={}", reqDTO.getStatus());
            
            List<XianyuGoodsInfo> items = goodsInfoService.listByStatus(reqDTO.getStatus());
            
            ItemListFromDbRespDTO respDTO = new ItemListFromDbRespDTO();
            respDTO.setItems(items);
            respDTO.setTotalCount(items != null ? items.size() : 0);
            
            log.info("从数据库获取商品列表成功: 数量={}", respDTO.getTotalCount());
            return ResultObject.success(respDTO);
        } catch (Exception e) {
            log.error("从数据库获取商品列表失败", e);
            return ResultObject.failed("获取商品列表失败: " + e.getMessage());
        }
    }
    
    @Override
    public ResultObject<ItemDetailRespDTO> getItemDetail(ItemDetailReqDTO reqDTO) {
        try {
            log.info("获取商品详情: xyGoodId={}", reqDTO.getXyGoodId());
            
            XianyuGoodsInfo item = goodsInfoService.getByXyGoodId(reqDTO.getXyGoodId());
            
            if (item == null) {
                return ResultObject.failed("商品不存在");
            }
            
            ItemDetailRespDTO respDTO = new ItemDetailRespDTO();
            respDTO.setItem(item);
            
            log.info("获取商品详情成功: xyGoodId={}", reqDTO.getXyGoodId());
            return ResultObject.success(respDTO);
        } catch (Exception e) {
            log.error("获取商品详情失败: xyGoodId={}", reqDTO.getXyGoodId(), e);
            return ResultObject.failed("获取商品详情失败: " + e.getMessage());
        }
    }

    /**
     * 解析商品列表响应
     */
    @SuppressWarnings("unchecked")
    private ItemListRespDTO parseItemListResponse(Map<String, Object> responseMap, int pageNumber, int pageSize) {
        ItemListRespDTO respDTO = new ItemListRespDTO();
        respDTO.setPageNumber(pageNumber);
        respDTO.setPageSize(pageSize);
        respDTO.setItems(new ArrayList<>());

        try {
            log.info("开始解析响应，responseMap keys: {}", responseMap.keySet());
            
            List<String> ret = (List<String>) responseMap.get("ret");
            log.info("ret字段: {}", ret);
            
            if (ret != null && !ret.isEmpty() && ret.get(0).contains("SUCCESS")) {
                log.info("API调用成功，开始解析数据");
                respDTO.setSuccess(true);

                Map<String, Object> data = (Map<String, Object>) responseMap.get("data");
                log.info("data字段存在: {}, keys: {}", data != null, data != null ? data.keySet() : "null");
                
                if (data != null) {
                    List<Map<String, Object>> cardList = (List<Map<String, Object>>) data.get("cardList");
                    log.info("cardList存在: {}, size: {}", cardList != null, cardList != null ? cardList.size() : 0);
                    
                    if (cardList != null) {
                        for (Map<String, Object> card : cardList) {
                            Map<String, Object> cardData = (Map<String, Object>) card.get("cardData");
                            if (cardData != null) {
                                // 将 Map 转换为 ItemDTO
                                ItemDTO itemDTO = objectMapper.convertValue(cardData, ItemDTO.class);
                                respDTO.getItems().add(itemDTO);
                            }
                        }
                    }
                }

                respDTO.setCurrentCount(respDTO.getItems().size());
                respDTO.setSavedCount(respDTO.getItems().size());
                log.info("解析完成，商品数量: {}", respDTO.getItems().size());
            } else {
                log.warn("API调用失败，ret: {}", ret);
                respDTO.setSuccess(false);
            }
        } catch (Exception e) {
            log.error("解析商品列表响应失败", e);
            respDTO.setSuccess(false);
        }

        return respDTO;
    }

    /**
     * 从数据库获取Cookie（包含 m_h5_tk 补充逻辑）
     * cookieId可以是：账号ID、账号备注(account_note)或UNB
     */
    private String getCookieFromDb(String cookieId) {
        try {
            log.info("从数据库查询Cookie: cookieId={}", cookieId);
            
            String cookie = null;
            
            // 1. 先尝试作为账号ID查询（数字）
            try {
                Long accountId = Long.parseLong(cookieId);
                cookie = accountService.getCookieByAccountId(accountId);
                if (cookie != null) {
                    log.info("通过账号ID获取Cookie成功: accountId={}", accountId);
                    // 检查并补充 _m_h5_tk
                    cookie = ensureMh5tkInCookie(cookie, accountId);
                    return cookie;
                }
            } catch (NumberFormatException e) {
                // 不是数字，继续其他方式查询
                log.debug("cookieId不是数字，尝试其他查询方式: {}", cookieId);
            }
            
            // 2. 尝试按账号备注查询
            cookie = accountService.getCookieByAccountNote(cookieId);
            if (cookie != null) {
                log.info("通过账号备注获取Cookie成功: accountNote={}", cookieId);
                return cookie;
            }
            
            // 3. 尝试按UNB查询
            cookie = accountService.getCookieByUnb(cookieId);
            if (cookie != null) {
                log.info("通过UNB获取Cookie成功: unb={}", cookieId);
                return cookie;
            }
            
            log.warn("未找到Cookie: cookieId={}", cookieId);
            return null;
            
        } catch (Exception e) {
            log.error("从数据库获取Cookie失败: cookieId={}", cookieId, e);
            return null;
        }
    }
    
    /**
     * 确保Cookie中包含 _m_h5_tk
     * 如果cookie_text中没有，则从数据库的m_h5_tk字段补充
     */
    private String ensureMh5tkInCookie(String cookieText, Long accountId) {
        try {
            // 解析Cookie
            Map<String, String> cookies = XianyuSignUtils.parseCookies(cookieText);
            
            // 如果已经包含 _m_h5_tk，直接返回
            if (cookies.containsKey("_m_h5_tk") && !cookies.get("_m_h5_tk").isEmpty()) {
                return cookieText;
            }
            
            // 从数据库获取 m_h5_tk
            String mH5Tk = accountService.getMh5tkByAccountId(accountId);
            if (mH5Tk != null && !mH5Tk.isEmpty()) {
                log.info("从数据库m_h5_tk字段补充token: accountId={}", accountId);
                cookies.put("_m_h5_tk", mH5Tk);
                return XianyuSignUtils.formatCookies(cookies);
            }
            
            log.warn("数据库中也没有m_h5_tk: accountId={}", accountId);
            return cookieText;
            
        } catch (Exception e) {
            log.error("补充m_h5_tk失败: accountId={}", accountId, e);
            return cookieText;
        }
    }
}
