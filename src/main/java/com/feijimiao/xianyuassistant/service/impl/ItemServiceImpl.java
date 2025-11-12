package com.feijimiao.xianyuassistant.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.feijimiao.xianyuassistant.common.ResultObject;
import com.feijimiao.xianyuassistant.model.dto.*;
import com.feijimiao.xianyuassistant.service.ItemService;
import com.feijimiao.xianyuassistant.utils.HttpClientUtils;
import com.feijimiao.xianyuassistant.utils.XianyuSignUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.*;

/**
 * 商品服务实现类
 */
@Slf4j
@Service
public class ItemServiceImpl implements ItemService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String API_URL = "https://h5api.m.goofish.com/h5/mtop.idle.web.xyh.item.list/1.0/";
    
    @Autowired
    private com.feijimiao.xianyuassistant.service.AccountService accountService;
    
    @Autowired
    private com.feijimiao.xianyuassistant.service.GoodsInfoService goodsInfoService;

    @Override
    public ResultObject<ItemListRespDTO> getItemList(ItemListReqDTO reqDTO) {
        try {
            log.info("开始获取商品列表: {}", reqDTO);

            // 从数据库获取Cookie
            String cookiesStr = getCookieFromDb(reqDTO.getCookieId());
            if (cookiesStr == null || cookiesStr.isEmpty()) {
                log.error("未找到账号Cookie: cookieId={}", reqDTO.getCookieId());
                return ResultObject.failed("未找到账号Cookie");
            }
            log.info("Cookie获取成功，长度: {}", cookiesStr.length());

            Map<String, String> cookies = XianyuSignUtils.parseCookies(cookiesStr);
            log.info("Cookie解析成功，字段数: {}, 包含字段: {}", cookies.size(), cookies.keySet());
            
            // 检查必需的Cookie字段
            if (!cookies.containsKey("_m_h5_tk") || cookies.get("_m_h5_tk").isEmpty()) {
                log.error("Cookie中缺少_m_h5_tk字段！这是API调用必需的。请重新登录以获取完整的Cookie。");
                log.error("当前Cookie包含的字段: {}", cookies.keySet());
                return ResultObject.failed("Cookie中缺少_m_h5_tk，请重新登录");
            }
            
            String token = XianyuSignUtils.extractToken(cookies);
            if (token == null || token.isEmpty()) {
                log.error("无法从_m_h5_tk中提取token！_m_h5_tk值: {}", cookies.get("_m_h5_tk"));
                return ResultObject.failed("Cookie格式错误，无法提取token");
            }
            log.info("Token提取成功: {}", token.substring(0, Math.min(10, token.length())));
            
            String timestamp = String.valueOf(System.currentTimeMillis());
            log.info("生成时间戳: {}", timestamp);

            // 构建请求参数
            Map<String, Object> dataMap = new HashMap<>();
            dataMap.put("needGroupInfo", false);
            dataMap.put("pageNumber", reqDTO.getPageNumber());
            dataMap.put("pageSize", reqDTO.getPageSize());
            dataMap.put("groupName", "在售");
            dataMap.put("groupId", "58877261");
            dataMap.put("defaultGroup", true);
            dataMap.put("userId", cookies.get("unb"));

            String dataJson = objectMapper.writeValueAsString(dataMap);
            log.info("数据JSON: {}", dataJson);
            
            String sign = XianyuSignUtils.generateSign(timestamp, token, dataJson);
            log.info("签名生成成功: {}", sign);

            // 构建URL参数（参考 Python 实现，使用 POST 请求）
            log.info("开始构建请求URL...");
            String url = UriComponentsBuilder.fromHttpUrl(API_URL)
                    .queryParam("jsv", "2.7.2")
                    .queryParam("appKey", "34839810")
                    .queryParam("t", timestamp)
                    .queryParam("sign", sign)
                    .queryParam("v", "1.0")
                    .queryParam("type", "originaljson")
                    .queryParam("accountSite", "xianyu")
                    .queryParam("dataType", "json")
                    .queryParam("timeout", "20000")
                    .queryParam("api", "mtop.idle.web.xyh.item.list")
                    .queryParam("sessionOption", "AutoLoginOnly")
                    .queryParam("spm_cnt", "a21ybx.im.0.0")  // Python 中的额外参数
                    .queryParam("spm_pre", "a21ybx.collection.menu.1.272b5141NafCNK")  // Python 中的额外参数
                    .toUriString();

            // 发送请求（使用 POST 方法，data 放在 body 中）
            log.info("准备发送HTTP POST请求到: {}", url);
            Map<String, String> headers = new HashMap<>();
            headers.put("Cookie", cookiesStr);
            // 参考 Python 配置中的 DEFAULT_HEADERS
            headers.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36");
            headers.put("Accept", "application/json");
            headers.put("Accept-Language", "zh-CN,zh;q=0.9");
            headers.put("Cache-Control", "no-cache");
            headers.put("Pragma", "no-cache");
            headers.put("Origin", "https://www.goofish.com");
            headers.put("Referer", "https://www.goofish.com/");
            headers.put("Sec-Fetch-Dest", "empty");
            headers.put("Sec-Fetch-Mode", "cors");
            headers.put("Sec-Fetch-Site", "same-site");
            headers.put("Sec-Ch-Ua", "\"Not(A:Brand\";v=\"99\", \"Google Chrome\";v=\"133\", \"Chromium\";v=\"133\"");
            headers.put("Sec-Ch-Ua-Mobile", "?0");
            headers.put("Sec-Ch-Ua-Platform", "\"Windows\"");

            // data 参数放在 POST body 中（注意：HttpClientUtils.post 会自动进行 URL 编码）
            Map<String, String> body = new HashMap<>();
            body.put("data", dataJson);
            log.info("POST body data 长度: {}", dataJson.length());

            log.info("发送POST请求...");
            String response = HttpClientUtils.post(url, headers, body);
            if (response == null) {
                log.error("HTTP请求返回null");
                return ResultObject.failed("请求闲鱼API失败");
            }
            log.info("HTTP请求成功，响应长度: {}", response.length());
            log.info("响应内容: {}", response);

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
    public ResultObject<AllItemsRespDTO> getAllItems(AllItemsReqDTO reqDTO) {
        try {
            log.info("开始获取所有商品: {}", reqDTO);

            AllItemsRespDTO respDTO = new AllItemsRespDTO();
            respDTO.setSuccess(true);
            respDTO.setItems(new ArrayList<>());
            
            int pageNumber = 1;
            int totalSaved = 0;

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
                    log.info("第{}页没有数据，获取完成", pageNumber);
                    break;
                }

                respDTO.getItems().addAll(pageData.getItems());
                totalSaved += pageData.getSavedCount() != null ? pageData.getSavedCount() : 0;

                log.info("第{}页获取到{}个商品", pageNumber, pageData.getItems().size());

                // 如果当前页商品数量少于页面大小，说明已经是最后一页
                if (pageData.getItems().size() < reqDTO.getPageSize()) {
                    log.info("第{}页商品数量({})少于页面大小({})，获取完成", 
                            pageNumber, pageData.getItems().size(), reqDTO.getPageSize());
                    break;
                }

                pageNumber++;
                
                // 添加延迟避免请求过快
                Thread.sleep(1000);
            }

            respDTO.setTotalPages(pageNumber);
            respDTO.setTotalCount(respDTO.getItems().size());
            respDTO.setTotalSaved(totalSaved);

            log.info("获取所有商品成功: cookieId={}, 总数量={}, 总页数={}", 
                    reqDTO.getCookieId(), respDTO.getTotalCount(), respDTO.getTotalPages());
            return ResultObject.success(respDTO);
        } catch (Exception e) {
            log.error("获取所有商品异常: cookieId={}", reqDTO.getCookieId(), e);
            return ResultObject.failed("获取所有商品异常: " + e.getMessage());
        }
    }

    @Override
    public ResultObject<ItemDbRespDTO> getItemsFromDb(ItemDbReqDTO reqDTO) {
        try {
            log.info("从数据库获取商品信息: {}", reqDTO);

            // TODO: 从数据库查询商品信息
            ItemDbRespDTO respDTO = new ItemDbRespDTO();
            respDTO.setSuccess(true);
            respDTO.setCount(0);
            respDTO.setItems(new ArrayList<>());

            log.info("从数据库获取商品成功: cookieId={}, 商品数量={}", 
                    reqDTO.getCookieId(), respDTO.getCount());
            return ResultObject.success(respDTO);
        } catch (Exception e) {
            log.error("从数据库获取商品异常: cookieId={}", reqDTO.getCookieId(), e);
            return ResultObject.failed("从数据库获取商品异常: " + e.getMessage());
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
