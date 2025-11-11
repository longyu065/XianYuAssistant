package com.feijimiao.xianyuassistant.controller;

import com.feijimiao.xianyuassistant.model.QRLoginResponse;
import com.feijimiao.xianyuassistant.model.QRStatusResponse;
import com.feijimiao.xianyuassistant.service.QRLoginService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 二维码登录控制器
 */
@RestController
@RequestMapping("/api/qrlogin")
@CrossOrigin(origins = "*")
public class QRLoginController {
    
    @Autowired
    private QRLoginService qrLoginService;
    
    /**
     * 生成二维码
     */
    @PostMapping("/generate")
    public QRLoginResponse generateQRCode() {
        return qrLoginService.generateQRCode();
    }
    
    /**
     * 获取会话状态
     */
    @GetMapping("/status/{sessionId}")
    public QRStatusResponse getSessionStatus(@PathVariable String sessionId) {
        return qrLoginService.getSessionStatus(sessionId);
    }
    
    /**
     * 获取会话Cookie
     */
    @GetMapping("/cookies/{sessionId}")
    public Map<String, String> getSessionCookies(@PathVariable String sessionId) {
        return qrLoginService.getSessionCookies(sessionId);
    }
    
    /**
     * 清理过期会话
     */
    @PostMapping("/cleanup")
    public void cleanupExpiredSessions() {
        qrLoginService.cleanupExpiredSessions();
    }
}
