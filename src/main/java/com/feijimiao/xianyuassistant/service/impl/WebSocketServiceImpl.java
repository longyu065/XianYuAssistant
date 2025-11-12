package com.feijimiao.xianyuassistant.service.impl;

import com.feijimiao.xianyuassistant.service.AccountService;
import com.feijimiao.xianyuassistant.service.WebSocketService;
import com.feijimiao.xianyuassistant.service.WebSocketTokenService;
import com.feijimiao.xianyuassistant.utils.XianyuSignUtils;
import com.feijimiao.xianyuassistant.websocket.WebSocketInitializer;
import com.feijimiao.xianyuassistant.websocket.WebSocketMessageHandler;
import com.feijimiao.xianyuassistant.websocket.XianyuWebSocketClient;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

/**
 * WebSocket服务实现类
 * 参考Python代码的XianyuAutoAsync类
 */
@Slf4j
@Service
public class WebSocketServiceImpl implements WebSocketService {

    @Autowired
    private AccountService accountService;
    
    @Autowired
    private WebSocketMessageHandler messageHandler;
    
    @Autowired
    private WebSocketTokenService tokenService;
    
    @Autowired
    private WebSocketInitializer initializer;
    
    @Autowired
    private com.feijimiao.xianyuassistant.service.ChatMessageService chatMessageService;

    // 存储WebSocket客户端
    private final Map<Long, XianyuWebSocketClient> webSocketClients = new ConcurrentHashMap<>();
    
    // 心跳定时器
    private final ScheduledExecutorService heartbeatScheduler = Executors.newScheduledThreadPool(1);
    
    // 心跳任务
    private final Map<Long, ScheduledFuture<?>> heartbeatTasks = new ConcurrentHashMap<>();

    /**
     * 闲鱼WebSocket URL
     * 参考Python代码：wss://wss-goofish.dingtalk.com/
     */
    private static final String WEBSOCKET_URL = "wss://wss-goofish.dingtalk.com/";

    @Override
    public boolean startWebSocket(Long accountId) {
        try {
            log.info("启动WebSocket连接: accountId={}", accountId);

            // 检查是否已经连接
            if (webSocketClients.containsKey(accountId)) {
                XianyuWebSocketClient existingClient = webSocketClients.get(accountId);
                if (existingClient.isConnected()) {
                    log.info("WebSocket已连接: accountId={}", accountId);
                    return true;
                } else {
                    // 关闭旧连接
                    stopWebSocket(accountId);
                }
            }

            // 获取Cookie
            String cookieStr = accountService.getCookieByAccountId(accountId);
            if (cookieStr == null || cookieStr.isEmpty()) {
                log.error("未找到账号Cookie: accountId={}", accountId);
                return false;
            }

            // 解析Cookie
            Map<String, String> cookies = XianyuSignUtils.parseCookies(cookieStr);
            
            // 生成设备ID（参考Python的generate_device_id）
            String unb = cookies.get("unb");
            if (unb == null || unb.isEmpty()) {
                log.error("Cookie中缺少unb字段: accountId={}", accountId);
                return false;
            }
            String deviceId = "web_" + unb;
            
            // 获取accessToken（参考Python的refresh_token）
            log.info("正在获取accessToken: accountId={}", accountId);
            String accessToken = tokenService.getAccessToken(accountId, cookieStr, deviceId);
            if (accessToken == null || accessToken.isEmpty()) {
                log.error("获取accessToken失败: accountId={}", accountId);
                log.error("无法继续WebSocket连接，请检查Cookie是否有效");
                return false;
            }
            log.info("accessToken获取成功: accountId={}, token长度={}", accountId, accessToken.length());
            
            // 调用通用连接方法
            return connectWebSocket(accountId, cookieStr, deviceId, accessToken);

        } catch (com.feijimiao.xianyuassistant.exception.CaptchaRequiredException e) {
            log.warn("启动WebSocket需要滑块验证: accountId={}, url={}", accountId, e.getCaptchaUrl());
            throw e; // 重新抛出，让Controller处理
        } catch (Exception e) {
            log.error("启动WebSocket失败: accountId={}", accountId, e);
            return false;
        }
    }

    @Override
    public boolean startWebSocketWithToken(Long accountId, String accessToken) {
        try {
            log.info("========== 使用手动Token启动WebSocket连接 ==========");
            log.info("【账号{}】accountId={}", accountId, accountId);
            log.info("【账号{}】accessToken长度={}", accountId, accessToken != null ? accessToken.length() : 0);
            log.info("【账号{}】accessToken前50字符={}", accountId, 
                    accessToken != null && accessToken.length() > 50 ? accessToken.substring(0, 50) + "..." : accessToken);

            // 检查是否已经连接
            if (webSocketClients.containsKey(accountId)) {
                XianyuWebSocketClient existingClient = webSocketClients.get(accountId);
                if (existingClient.isConnected()) {
                    log.info("【账号{}】WebSocket已连接", accountId);
                    return true;
                } else {
                    // 关闭旧连接
                    log.info("【账号{}】关闭旧连接", accountId);
                    stopWebSocket(accountId);
                }
            }

            // 获取Cookie
            String cookieStr = accountService.getCookieByAccountId(accountId);
            if (cookieStr == null || cookieStr.isEmpty()) {
                log.error("【账号{}】未找到账号Cookie", accountId);
                return false;
            }
            log.info("【账号{}】Cookie长度={}", accountId, cookieStr.length());

            // 解析Cookie
            Map<String, String> cookies = XianyuSignUtils.parseCookies(cookieStr);
            log.info("【账号{}】解析到{}个Cookie字段", accountId, cookies.size());
            
            // 生成设备ID
            String unb = cookies.get("unb");
            if (unb == null || unb.isEmpty()) {
                log.error("【账号{}】Cookie中缺少unb字段", accountId);
                return false;
            }
            String deviceId = "web_" + unb;
            log.info("【账号{}】设备ID={}", accountId, deviceId);
            
            log.info("【账号{}】准备调用通用连接方法（Token将在注册成功后保存）...", accountId);
            
            // 调用通用连接方法
            boolean result = connectWebSocket(accountId, cookieStr, deviceId, accessToken);
            
            log.info("【账号{}】连接结果={}", accountId, result);
            log.info("========== 手动Token启动流程结束 ==========");
            
            return result;

        } catch (Exception e) {
            log.error("【账号{}】使用手动Token启动WebSocket失败", accountId, e);
            return false;
        }
    }

    /**
     * 通用WebSocket连接方法
     */
    private boolean connectWebSocket(Long accountId, String cookieStr, String deviceId, String accessToken) throws Exception {
        try {
            // 构建WebSocket请求头（参考Python的WEBSOCKET_HEADERS配置）
            Map<String, String> headers = new HashMap<>();
            headers.put("Cookie", cookieStr);
            headers.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36");
            headers.put("Origin", "https://www.goofish.com");
            headers.put("Host", "wss-goofish.dingtalk.com");
            headers.put("Accept-Encoding", "gzip, deflate, br, zstd");
            headers.put("Accept-Language", "zh-CN,zh;q=0.9");
            headers.put("Cache-Control", "no-cache");
            headers.put("Pragma", "no-cache");
            headers.put("Connection", "Upgrade");
            headers.put("Upgrade", "websocket");

            // 创建WebSocket客户端（参考Python的_create_websocket_connection）
            URI serverUri = new URI(WEBSOCKET_URL);
            XianyuWebSocketClient client = new XianyuWebSocketClient(serverUri, headers, String.valueOf(accountId));
            
            // 设置消息处理器
            client.setMessageHandler(messageHandler);
            
            // 设置聊天消息服务
            client.setChatMessageService(chatMessageService);
            
            // 设置注册成功回调（保存Token）
            final String finalAccessToken = accessToken;
            client.setOnRegistrationSuccess(() -> {
                log.info("【账号{}】注册成功回调被触发，开始保存Token到数据库", accountId);
                tokenService.saveToken(accountId, finalAccessToken);
                log.info("【账号{}】✅ Token已成功保存到数据库", accountId);
            });

            // 连接WebSocket（参考Python的connect方法）
            log.info("正在连接WebSocket: {}", WEBSOCKET_URL);
            log.info("请求头: {}", headers);
            
            boolean connected = client.connectBlocking(10, TimeUnit.SECONDS);
            
            if (connected) {
                webSocketClients.put(accountId, client);
                
                // 执行WebSocket初始化流程（参考Python的init方法）
                log.info("开始WebSocket初始化流程: accountId={}", accountId);
                initializer.initialize(client, accessToken, deviceId, String.valueOf(accountId));
                
                // 启动心跳任务
                startHeartbeat(accountId, client);
                
                log.info("WebSocket连接成功: accountId={}", accountId);
                log.info("连接状态: isOpen={}, isClosed={}", 
                        client.isOpen(), client.isClosed());
                return true;
            } else {
                log.error("WebSocket连接失败: accountId={}", accountId);
                log.error("连接状态: isOpen={}, isClosed={}", 
                        client.isOpen(), client.isClosed());
                return false;
            }
        } catch (Exception e) {
            log.error("连接WebSocket异常: accountId={}", accountId, e);
            throw e;
        }
    }

    @Override
    public boolean stopWebSocket(Long accountId) {
        try {
            log.info("停止WebSocket连接: accountId={}", accountId);

            // 停止心跳任务
            stopHeartbeat(accountId);

            // 关闭WebSocket连接
            XianyuWebSocketClient client = webSocketClients.remove(accountId);
            if (client != null) {
                client.close();
                log.info("WebSocket连接已关闭: accountId={}", accountId);
                return true;
            } else {
                log.warn("WebSocket连接不存在: accountId={}", accountId);
                return false;
            }

        } catch (Exception e) {
            log.error("停止WebSocket失败: accountId={}", accountId, e);
            return false;
        }
    }

    @Override
    public boolean isConnected(Long accountId) {
        XianyuWebSocketClient client = webSocketClients.get(accountId);
        return client != null && client.isConnected();
    }

    @Override
    public void stopAllWebSockets() {
        log.info("停止所有WebSocket连接");
        
        for (Long accountId : webSocketClients.keySet()) {
            stopWebSocket(accountId);
        }
        
        // 关闭心跳调度器
        heartbeatScheduler.shutdown();
    }

    /**
     * 启动心跳任务
     */
    private void startHeartbeat(Long accountId, XianyuWebSocketClient client) {
        // 每15秒发送一次心跳（参考Python的HEARTBEAT_INTERVAL=15）
        ScheduledFuture<?> task = heartbeatScheduler.scheduleAtFixedRate(
            () -> {
                try {
                    if (client.isConnected()) {
                        log.info("【账号{}】WebSocket连接状态: isOpen={}, isClosed={}", 
                                accountId, client.isOpen(), client.isClosed());
                        client.sendHeartbeat();
                    } else {
                        log.warn("WebSocket未连接，停止心跳: accountId={}", accountId);
                        stopHeartbeat(accountId);
                    }
                } catch (Exception e) {
                    log.error("发送心跳失败: accountId={}", accountId, e);
                }
            },
            15, 15, TimeUnit.SECONDS
        );
        
        heartbeatTasks.put(accountId, task);
        log.info("心跳任务已启动: accountId={}, 间隔15秒", accountId);
    }

    /**
     * 停止心跳任务
     */
    private void stopHeartbeat(Long accountId) {
        ScheduledFuture<?> task = heartbeatTasks.remove(accountId);
        if (task != null) {
            task.cancel(false);
            log.info("心跳任务已停止: accountId={}", accountId);
        }
    }

    /**
     * 应用关闭时清理资源
     */
    @PreDestroy
    public void cleanup() {
        log.info("应用关闭，清理WebSocket资源");
        stopAllWebSockets();
    }
}
