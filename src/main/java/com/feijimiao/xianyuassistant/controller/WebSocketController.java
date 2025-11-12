package com.feijimiao.xianyuassistant.controller;

import com.feijimiao.xianyuassistant.common.ResultObject;
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

    /**
     * 启动WebSocket连接
     */
    @PostMapping("/start")
    public ResultObject<CaptchaInfoDTO> startWebSocket(@RequestBody WebSocketReqDTO reqDTO) {
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
            log.warn("需要滑块验证", e);
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
    public ResultObject<String> stopWebSocket(@RequestBody WebSocketReqDTO reqDTO) {
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
     * 检查WebSocket连接状态
     */
    @PostMapping("/status")
    public ResultObject<WebSocketStatusRespDTO> getWebSocketStatus(@RequestBody WebSocketReqDTO reqDTO) {
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
            
            return ResultObject.success(respDTO);
            
        } catch (Exception e) {
            log.error("查询WebSocket状态失败", e);
            return ResultObject.failed("查询WebSocket状态失败: " + e.getMessage());
        }
    }

    /**
     * WebSocket请求DTO
     */
    @Data
    public static class WebSocketReqDTO {
        private Long xianyuAccountId;
        private String accessToken; // 可选：手动提供的accessToken
    }

    /**
     * WebSocket状态响应DTO
     */
    @Data
    public static class WebSocketStatusRespDTO {
        private Long xianyuAccountId;
        private Boolean connected;
        private String status;
    }
    
    /**
     * 滑块验证信息DTO
     */
    @Data
    public static class CaptchaInfoDTO {
        private Boolean needCaptcha;
        private String captchaUrl;
        private String message;
    }
}
