package com.feijimiao.xianyuassistant.controller;

import com.feijimiao.xianyuassistant.common.ResultObject;
import com.feijimiao.xianyuassistant.entity.XianyuAccount;
import com.feijimiao.xianyuassistant.controller.dto.UpdateCookieReqDTO;
import com.feijimiao.xianyuassistant.controller.dto.UpdateCookieRespDTO;
import com.feijimiao.xianyuassistant.service.WebSocketService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * WebSocket控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/websocket")
@CrossOrigin(origins = "*")
public class WebSocketController {

    @Autowired
    private WebSocketService webSocketService;
    
    @Autowired
    private org.springframework.context.ApplicationContext applicationContext;

    /**
     * 启动WebSocket连接
     */
    @PostMapping("/start")
    public ResultObject<CaptchaInfoDTO> startWebSocket(@RequestBody StartWebSocketReqDTO reqDTO) {
        try {
            log.info("启动WebSocket请求: xianyuAccountId={}, 手动Token={}", 
                    reqDTO.getXianyuAccountId(), 
                    reqDTO.getAccessToken() != null ? "已提供" : "未提供");
            
            if (reqDTO.getXianyuAccountId() == null) {
                return ResultObject.failed("账号ID不能为空");
            }
            
            boolean success;
            if (reqDTO.getAccessToken() != null && !reqDTO.getAccessToken().isEmpty()) {
                // 使用手动提供的 accessToken
                success = webSocketService.startWebSocketWithToken(
                        reqDTO.getXianyuAccountId(), 
                        reqDTO.getAccessToken()
                );
            } else {
                // 自动获取 accessToken
                success = webSocketService.startWebSocket(reqDTO.getXianyuAccountId());
            }
            
            if (success) {
                return ResultObject.success(null, "WebSocket连接已启动");
            } else {
                return ResultObject.failed("WebSocket连接启动失败");
            }
            
        } catch (com.feijimiao.xianyuassistant.exception.CaptchaRequiredException e) {
            log.debug("需要滑块验证: accountId={}, url={}", reqDTO.getXianyuAccountId(), e.getCaptchaUrl());
            CaptchaInfoDTO captchaInfo = new CaptchaInfoDTO();
            captchaInfo.setNeedCaptcha(true);
            captchaInfo.setCaptchaUrl(e.getCaptchaUrl());
            captchaInfo.setMessage("需要完成滑块验证，请在浏览器中打开验证链接");
            ResultObject<CaptchaInfoDTO> result = new ResultObject<>(500, "需要滑块验证", captchaInfo);
            return result;
        } catch (Exception e) {
            log.error("启动WebSocket失败", e);
            return ResultObject.failed("启动WebSocket失败: " + e.getMessage());
        }
    }

    /**
     * 停止WebSocket连接
     */
    @PostMapping("/stop")
    public ResultObject<String> stopWebSocket(@RequestBody StopWebSocketReqDTO reqDTO) {
        try {
            log.info("停止WebSocket请求: xianyuAccountId={}", reqDTO.getXianyuAccountId());
            
            if (reqDTO.getXianyuAccountId() == null) {
                return ResultObject.failed("账号ID不能为空");
            }
            
            boolean success = webSocketService.stopWebSocket(reqDTO.getXianyuAccountId());
            
            if (success) {
                return ResultObject.success("WebSocket连接已停止");
            } else {
                return ResultObject.failed("WebSocket连接停止失败");
            }
            
        } catch (Exception e) {
            log.error("停止WebSocket失败", e);
            return ResultObject.failed("停止WebSocket失败: " + e.getMessage());
        }
    }

    /**
     * 发送消息
     */
    @PostMapping("/sendMessage")
    public ResultObject<String> sendMessage(@RequestBody SendMessageReqDTO reqDTO) {
        try {
            log.info("发送消息请求: xianyuAccountId={}, cid={}, toId={}, text={}", 
                    reqDTO.getXianyuAccountId(), reqDTO.getCid(), reqDTO.getToId(), reqDTO.getText());
            
            // 参数校验
            if (reqDTO.getXianyuAccountId() == null) {
                return ResultObject.failed("账号ID不能为空");
            }
            if (reqDTO.getCid() == null || reqDTO.getCid().isEmpty()) {
                return ResultObject.failed("会话ID(cid)不能为空");
            }
            if (reqDTO.getToId() == null || reqDTO.getToId().isEmpty()) {
                return ResultObject.failed("接收方ID(toId)不能为空");
            }
            if (reqDTO.getText() == null || reqDTO.getText().isEmpty()) {
                return ResultObject.failed("消息内容不能为空");
            }
            
            // 检查WebSocket连接状态
            if (!webSocketService.isConnected(reqDTO.getXianyuAccountId())) {
                return ResultObject.failed("WebSocket未连接，请先启动连接");
            }
            
            // 发送消息
            boolean success = webSocketService.sendMessage(
                    reqDTO.getXianyuAccountId(),
                    reqDTO.getCid(),
                    reqDTO.getToId(),
                    reqDTO.getText()
            );
            
            if (success) {
                return ResultObject.success("消息发送成功");
            } else {
                return ResultObject.failed("消息发送失败");
            }
            
        } catch (Exception e) {
            log.error("发送消息失败", e);
            return ResultObject.failed("发送消息失败: " + e.getMessage());
        }
    }

    /**
     * 检查WebSocket连接状态
     */
    @PostMapping("/status")
    public ResultObject<WebSocketStatusRespDTO> getWebSocketStatus(@RequestBody GetWebSocketStatusReqDTO reqDTO) {
        try {
            log.info("查询WebSocket状态: xianyuAccountId={}", reqDTO.getXianyuAccountId());
            
            if (reqDTO.getXianyuAccountId() == null) {
                return ResultObject.failed("账号ID不能为空");
            }
            
            boolean connected = webSocketService.isConnected(reqDTO.getXianyuAccountId());
            
            WebSocketStatusRespDTO respDTO = new WebSocketStatusRespDTO();
            respDTO.setXianyuAccountId(reqDTO.getXianyuAccountId());
            respDTO.setConnected(connected);
            respDTO.setStatus(connected ? "已连接" : "未连接");
            
            // 获取Cookie状态和Cookie值
            com.feijimiao.xianyuassistant.service.AccountService accountService = 
                    applicationContext.getBean(com.feijimiao.xianyuassistant.service.AccountService.class);
            
            // 查询Cookie信息
            com.feijimiao.xianyuassistant.mapper.XianyuCookieMapper cookieMapper = 
                    applicationContext.getBean(com.feijimiao.xianyuassistant.mapper.XianyuCookieMapper.class);
            
            com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.feijimiao.xianyuassistant.entity.XianyuCookie> cookieQuery = 
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
            cookieQuery.eq(com.feijimiao.xianyuassistant.entity.XianyuCookie::getXianyuAccountId, reqDTO.getXianyuAccountId())
                    .orderByDesc(com.feijimiao.xianyuassistant.entity.XianyuCookie::getCreatedTime)
                    .last("LIMIT 1");
            com.feijimiao.xianyuassistant.entity.XianyuCookie cookie = cookieMapper.selectOne(cookieQuery);
            
            if (cookie != null) {
                respDTO.setCookieStatus(cookie.getCookieStatus());
                respDTO.setCookieText(cookie.getCookieText());
                respDTO.setWebsocketToken(cookie.getWebsocketToken());
                respDTO.setTokenExpireTime(cookie.getTokenExpireTime());
            } else {
                respDTO.setCookieStatus(null);
                respDTO.setCookieText(null);
                respDTO.setWebsocketToken(null);
                respDTO.setTokenExpireTime(null);
            }
            
            return ResultObject.success(respDTO);
            
        } catch (Exception e) {
            log.error("查询WebSocket状态失败", e);
            return ResultObject.failed("查询WebSocket状态失败: " + e.getMessage());
        }
    }
    
    /**
     * 清除验证等待状态
     */
    @PostMapping("/clearCaptchaWait")
    public ResultObject<String> clearCaptchaWait(@RequestBody ClearCaptchaWaitReqDTO reqDTO) {
        try {
            log.info("清除验证等待状态: xianyuAccountId={}", reqDTO.getXianyuAccountId());
            
            if (reqDTO.getXianyuAccountId() == null) {
                return ResultObject.failed("账号ID不能为空");
            }
            
            // 调用tokenService清除等待状态
            com.feijimiao.xianyuassistant.service.WebSocketTokenService tokenService = 
                    applicationContext.getBean(com.feijimiao.xianyuassistant.service.WebSocketTokenService.class);
            tokenService.clearCaptchaWait(reqDTO.getXianyuAccountId());
            
            return ResultObject.success("验证等待状态已清除，可以重新请求");
            
        } catch (Exception e) {
            log.error("清除验证等待状态失败", e);
            return ResultObject.failed("清除验证等待状态失败: " + e.getMessage());
        }
    }

    /**
     * 更新Cookie
     */
    @PostMapping("/updateCookie")
    public ResultObject<UpdateCookieRespDTO> updateCookie(@RequestBody UpdateCookieReqDTO reqDTO) {
        try {
            log.info("更新Cookie请求: xianyuAccountId={}", reqDTO.getXianyuAccountId());
            
            if (reqDTO.getXianyuAccountId() == null) {
                return ResultObject.failed("账号ID不能为空");
            }
            
            if (reqDTO.getCookieText() == null || reqDTO.getCookieText().trim().isEmpty()) {
                return ResultObject.failed("Cookie不能为空");
            }
            
            // 检查账号是否存在
            com.feijimiao.xianyuassistant.mapper.XianyuAccountMapper accountMapper = 
                    applicationContext.getBean(com.feijimiao.xianyuassistant.mapper.XianyuAccountMapper.class);
            XianyuAccount account = accountMapper.selectById(reqDTO.getXianyuAccountId());
            if (account == null) {
                return ResultObject.failed("账号不存在");
            }
            
            // 从Cookie中提取UNB
            String unb = extractUnbFromCookie(reqDTO.getCookieText());
            if (unb == null || unb.isEmpty()) {
                return ResultObject.failed("无法从Cookie中提取UNB信息，请确保Cookie包含unb字段");
            }
            
            // 更新Cookie
            com.feijimiao.xianyuassistant.service.AccountService accountService = 
                    applicationContext.getBean(com.feijimiao.xianyuassistant.service.AccountService.class);
            accountService.updateAccountCookie(reqDTO.getXianyuAccountId(), unb, reqDTO.getCookieText());
            
            UpdateCookieRespDTO respDTO = new UpdateCookieRespDTO();
            respDTO.setMessage("Cookie更新成功");
            return ResultObject.success(respDTO);
        } catch (Exception e) {
            log.error("更新Cookie失败", e);
            return ResultObject.failed("更新Cookie失败: " + e.getMessage());
        }
    }
    
    /**
     * 从Cookie字符串中提取UNB值
     *
     * @param cookie Cookie字符串
     * @return UNB值，如果未找到则返回null
     */
    private String extractUnbFromCookie(String cookie) {
        if (cookie == null || cookie.isEmpty()) {
            return null;
        }
        
        // 查找unb=后面的值
        String[] cookieParts = cookie.split(";\\s*");
        for (String part : cookieParts) {
            if (part.startsWith("unb=")) {
                return part.substring(4); // "unb=".length() = 4
            }
        }
        
        return null;
    }

    /**
     * 启动WebSocket连接请求DTO
     */
    @Data
    public static class StartWebSocketReqDTO {
        private Long xianyuAccountId;  // 账号ID
        private String accessToken;    // 可选：手动提供的accessToken
    }

    /**
     * 停止WebSocket连接请求DTO
     */
    @Data
    public static class StopWebSocketReqDTO {
        private Long xianyuAccountId;  // 账号ID
    }

    /**
     * 获取WebSocket状态请求DTO
     */
    @Data
    public static class GetWebSocketStatusReqDTO {
        private Long xianyuAccountId;  // 账号ID
    }

    /**
     * 清除验证等待状态请求DTO
     */
    @Data
    public static class ClearCaptchaWaitReqDTO {
        private Long xianyuAccountId;  // 账号ID
    }

    /**
     * WebSocket状态响应DTO
     */
    @Data
    public static class WebSocketStatusRespDTO {
        private Long xianyuAccountId;  // 账号ID
        private Boolean connected;     // 是否已连接
        private String status;         // 连接状态描述
        private Integer cookieStatus;  // Cookie状态 1:有效 2:过期 3:失效
        private String cookieText;     // Cookie值
        private String websocketToken; // WebSocket Token
        private Long tokenExpireTime;  // Token过期时间戳（毫秒）
    }
    
    /**
     * 滑块验证信息响应DTO
     */
    @Data
    public static class CaptchaInfoDTO {
        private Boolean needCaptcha;  // 是否需要验证
        private String captchaUrl;    // 验证链接
        private String message;       // 提示信息
    }
    
    /**
     * 发送消息请求DTO
     */
    @Data
    public static class SendMessageReqDTO {
        private Long xianyuAccountId;  // 账号ID
        private String cid;            // 会话ID（不带@goofish后缀）
        private String toId;           // 接收方用户ID（不带@goofish后缀）
        private String text;           // 消息文本内容
    }
}
