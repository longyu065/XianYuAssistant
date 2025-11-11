package com.feijimiao.xianyuassistant.service.impl;

import com.feijimiao.xianyuassistant.model.QRLoginResponse;
import com.feijimiao.xianyuassistant.model.QRLoginSession;
import com.feijimiao.xianyuassistant.model.QRStatusResponse;
import com.feijimiao.xianyuassistant.service.QRLoginService;
import com.feijimiao.xianyuassistant.utils.CookieUtils;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 二维码登录服务实现
 */
@Service
public class QRLoginServiceImpl implements QRLoginService {
    
    private static final Logger logger = LoggerFactory.getLogger(QRLoginServiceImpl.class);
    
    private final Map<String, QRLoginSession> sessions = new ConcurrentHashMap<>();
    private final OkHttpClient httpClient;
    private final Gson gson = new Gson();
    
    private static final String HOST = "https://passport.goofish.com";
    private static final String API_MINI_LOGIN = HOST + "/mini_login.htm";
    private static final String API_GENERATE_QR = HOST + "/newlogin/qrcode/generate.do";
    private static final String API_SCAN_STATUS = HOST + "/newlogin/qrcode/query.do";
    private static final String API_H5_TK = "https://h5api.m.goofish.com/h5/mtop.gaia.nodejs.gaia.idle.data.gw.v2.index.get/1.0/";
    
    public QRLoginServiceImpl() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .followRedirects(true)
                .build();
    }
    
    private Headers generateHeaders() {
        // 注意：不要手动设置Accept-Encoding，让OkHttp自动处理gzip
        return new Headers.Builder()
                .add("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .add("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                .add("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                .add("Upgrade-Insecure-Requests", "1")
                .build();
    }
    
    private Headers generateApiHeaders() {
        // 注意：不要手动设置Accept-Encoding，让OkHttp自动处理gzip
        return new Headers.Builder()
                .add("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .add("Accept", "application/json, text/plain, */*")
                .add("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                .add("Referer", "https://passport.goofish.com/")
                .add("Origin", "https://passport.goofish.com")
                .build();
    }

    
    /**
     * 获取m_h5_tk
     */
    private void getMh5tk(QRLoginSession session) throws IOException {
        Map<String, Object> data = new HashMap<>();
        data.put("bizScene", "home");
        String dataStr = gson.toJson(data);
        long t = System.currentTimeMillis();
        String appKey = "34839810";
        
        // 第一次请求获取cookie
        Request request = new Request.Builder()
                .url(API_H5_TK)
                .headers(generateApiHeaders())
                .get()
                .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful()) {
                // 提取cookie
                List<String> cookieHeaders = response.headers("Set-Cookie");
                for (String cookie : cookieHeaders) {
                    String[] parts = cookie.split(";")[0].split("=", 2);
                    if (parts.length == 2) {
                        session.getCookies().put(parts[0], parts[1]);
                    }
                }
                
                String mh5tk = session.getCookies().get("m_h5_tk");
                String token = "";
                if (mh5tk != null && mh5tk.contains("_")) {
                    token = mh5tk.split("_")[0];
                }
                
                // 生成签名
                String signInput = token + "&" + t + "&" + appKey + "&" + dataStr;
                String sign = md5(signInput);
                
                // 构造请求参数
                HttpUrl url = HttpUrl.parse(API_H5_TK).newBuilder()
                        .addQueryParameter("jsv", "2.7.2")
                        .addQueryParameter("appKey", appKey)
                        .addQueryParameter("t", String.valueOf(t))
                        .addQueryParameter("sign", sign)
                        .addQueryParameter("v", "1.0")
                        .addQueryParameter("type", "originaljson")
                        .addQueryParameter("dataType", "json")
                        .addQueryParameter("timeout", "20000")
                        .addQueryParameter("api", "mtop.gaia.nodejs.gaia.idle.data.gw.v2.index.get")
                        .addQueryParameter("data", dataStr)
                        .build();
                
                // 第二次请求
                Request request2 = new Request.Builder()
                        .url(url)
                        .headers(generateApiHeaders())
                        .header("Cookie", CookieUtils.formatCookies(session.getCookies()))
                        .post(RequestBody.create(new byte[0]))
                        .build();
                
                try (Response response2 = httpClient.newCall(request2).execute()) {
                    logger.info("获取m_h5_tk成功: {}", session.getSessionId());
                }
            }
        }
    }
    
    /**
     * 获取登录参数
     */
    private Map<String, String> getLoginParams(QRLoginSession session) throws IOException {
        HttpUrl url = HttpUrl.parse(API_MINI_LOGIN).newBuilder()
                .addQueryParameter("lang", "zh_cn")
                .addQueryParameter("appName", "xianyu")
                .addQueryParameter("appEntrance", "web")
                .addQueryParameter("styleType", "vertical")
                .addQueryParameter("bizParams", "")
                .addQueryParameter("notLoadSsoView", "false")
                .addQueryParameter("notKeepLogin", "false")
                .addQueryParameter("isMobile", "false")
                .addQueryParameter("qrCodeFirst", "false")
                .addQueryParameter("stie", "77")
                .addQueryParameter("rnd", String.valueOf(Math.random()))
                .build();
        
        Request request = new Request.Builder()
                .url(url)
                .headers(generateHeaders())
                .header("Cookie", CookieUtils.formatCookies(session.getCookies()))
                .get()
                .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful()) {
                String html = response.body().string();
                logger.debug("获取登录页面HTML长度: {}", html.length());
                
                // 正则匹配需要的json数据
                Pattern pattern = Pattern.compile("window\\.viewData\\s*=\\s*(\\{.*?\\});", Pattern.DOTALL);
                Matcher matcher = pattern.matcher(html);
                
                if (matcher.find()) {
                    String jsonString = matcher.group(1);
                    logger.debug("提取到的viewData: {}", jsonString.substring(0, Math.min(200, jsonString.length())));
                    
                    JsonObject viewData = gson.fromJson(jsonString, JsonObject.class);
                    JsonObject loginFormData = viewData.getAsJsonObject("loginFormData");
                    
                    if (loginFormData != null) {
                        Map<String, String> params = new HashMap<>();
                        loginFormData.entrySet().forEach(entry -> {
                            if (entry.getValue().isJsonPrimitive()) {
                                params.put(entry.getKey(), entry.getValue().getAsString());
                            } else {
                                params.put(entry.getKey(), entry.getValue().toString());
                            }
                        });
                        params.put("umidTag", "SERVER");
                        session.getParams().putAll(params);
                        logger.info("获取登录参数成功: {}, 参数数量: {}", session.getSessionId(), params.size());
                        return params;
                    } else {
                        logger.error("viewData中没有loginFormData字段，viewData keys: {}", viewData.keySet());
                    }
                } else {
                    logger.error("未匹配到window.viewData，尝试查找其他模式");
                    // 尝试其他可能的模式
                    Pattern pattern2 = Pattern.compile("var\\s+viewData\\s*=\\s*(\\{.*?\\});", Pattern.DOTALL);
                    Matcher matcher2 = pattern2.matcher(html);
                    if (matcher2.find()) {
                        String jsonString = matcher2.group(1);
                        logger.debug("使用备用模式提取到viewData");
                        JsonObject viewData = gson.fromJson(jsonString, JsonObject.class);
                        JsonObject loginFormData = viewData.getAsJsonObject("loginFormData");
                        if (loginFormData != null) {
                            Map<String, String> params = new HashMap<>();
                            loginFormData.entrySet().forEach(entry -> {
                                if (entry.getValue().isJsonPrimitive()) {
                                    params.put(entry.getKey(), entry.getValue().getAsString());
                                } else {
                                    params.put(entry.getKey(), entry.getValue().toString());
                                }
                            });
                            params.put("umidTag", "SERVER");
                            session.getParams().putAll(params);
                            logger.info("获取登录参数成功(备用模式): {}", session.getSessionId());
                            return params;
                        }
                    }
                }
                
                // 如果都失败了，保存HTML用于调试
                logger.error("无法提取登录参数，HTML内容前1000字符: {}", html.substring(0, Math.min(1000, html.length())));
                
                // 尝试直接查找所有可能的参数
                Map<String, String> params = extractParamsFromHtml(html);
                if (!params.isEmpty()) {
                    params.put("umidTag", "SERVER");
                    session.getParams().putAll(params);
                    logger.info("使用备用方法提取到参数: {}", params.keySet());
                    return params;
                }
                
                throw new RuntimeException("未找到loginFormData");
            }
            throw new RuntimeException("获取登录参数失败，HTTP状态码: " + response.code());
        }
    }

    
    @Override
    public QRLoginResponse generateQRCode() {
        try {
            // 创建新会话
            String sessionId = UUID.randomUUID().toString();
            QRLoginSession session = new QRLoginSession(sessionId);
            
            // 1. 获取m_h5_tk
            getMh5tk(session);
            
            // 2. 获取登录参数
            Map<String, String> loginParams = getLoginParams(session);
            
            // 3. 生成二维码
            HttpUrl.Builder urlBuilder = HttpUrl.parse(API_GENERATE_QR).newBuilder();
            loginParams.forEach(urlBuilder::addQueryParameter);
            
            Request request = new Request.Builder()
                    .url(urlBuilder.build())
                    .headers(generateApiHeaders())
                    .get()
                    .build();
            
            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    logger.debug("获取二维码接口原始响应: {}", responseBody);
                    
                    JsonObject results = gson.fromJson(responseBody, JsonObject.class);
                    JsonObject content = results.getAsJsonObject("content");
                    
                    if (content != null && content.get("success").getAsBoolean()) {
                        JsonObject data = content.getAsJsonObject("data");
                        
                        // 更新会话参数
                        session.getParams().put("t", data.get("t").getAsString());
                        session.getParams().put("ck", data.get("ck").getAsString());
                        
                        // 获取二维码内容
                        String qrContent = data.get("codeContent").getAsString();
                        session.setQrContent(qrContent);
                        
                        // 生成二维码图片（base64格式）
                        String qrDataUrl = generateQRCodeImage(qrContent);
                        session.setQrCodeUrl(qrDataUrl);
                        session.setStatus("waiting");
                        
                        // 保存会话
                        sessions.put(sessionId, session);
                        
                        // 启动状态监控
                        new Thread(() -> monitorQRStatus(sessionId)).start();
                        
                        logger.info("二维码生成成功: {}", sessionId);
                        return new QRLoginResponse(true, sessionId, qrDataUrl, null);
                    } else {
                        return new QRLoginResponse(false, "获取登录二维码失败");
                    }
                }
            }
            
            return new QRLoginResponse(false, "生成二维码失败");
            
        } catch (Exception e) {
            logger.error("二维码生成过程中发生异常", e);
            return new QRLoginResponse(false, "生成二维码失败: " + e.getMessage());
        }
    }
    
    /**
     * 从HTML中提取参数（备用方法）
     */
    private Map<String, String> extractParamsFromHtml(String html) {
        Map<String, String> params = new HashMap<>();
        
        // 尝试提取常见的参数
        String[] paramNames = {"appName", "appEntrance", "hsiz", "rnd", "bizParams", 
                               "isMobile", "lang", "returnUrl", "fromSite", "umidToken"};
        
        for (String paramName : paramNames) {
            Pattern pattern = Pattern.compile("\"" + paramName + "\"\\s*:\\s*\"([^\"]+)\"");
            Matcher matcher = pattern.matcher(html);
            if (matcher.find()) {
                params.put(paramName, matcher.group(1));
                logger.debug("提取到参数 {}: {}", paramName, matcher.group(1));
            }
        }
        
        return params;
    }
    
    /**
     * 生成二维码图片（Base64格式）
     */
    private String generateQRCodeImage(String content) throws WriterException, IOException {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.MARGIN, 2);
        
        BitMatrix bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, 300, 300, hints);
        BufferedImage qrImage = MatrixToImageWriter.toBufferedImage(bitMatrix);
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(qrImage, "PNG", baos);
        byte[] imageBytes = baos.toByteArray();
        String base64 = Base64.getEncoder().encodeToString(imageBytes);
        
        return "data:image/png;base64," + base64;
    }

    
    /**
     * 监控二维码状态
     */
    private void monitorQRStatus(String sessionId) {
        try {
            QRLoginSession session = sessions.get(sessionId);
            if (session == null) {
                return;
            }
            
            logger.info("开始监控二维码状态: {}", sessionId);
            
            long maxWaitTime = 300000; // 5分钟
            long startTime = System.currentTimeMillis();
            
            while (System.currentTimeMillis() - startTime < maxWaitTime) {
                try {
                    // 检查会话是否还存在
                    if (!sessions.containsKey(sessionId)) {
                        break;
                    }
                    
                    // 轮询二维码状态
                    String qrCodeStatus = pollQRCodeStatus(session);
                    
                    if ("CONFIRMED".equals(qrCodeStatus)) {
                        // 登录确认
                        logger.info("扫码登录成功: {}, UNB: {}", sessionId, session.getUnb());
                        break;
                    } else if ("NEW".equals(qrCodeStatus)) {
                        // 二维码未被扫描，继续轮询
                    } else if ("EXPIRED".equals(qrCodeStatus)) {
                        // 二维码已过期
                        session.setStatus("expired");
                        logger.info("二维码已过期: {}", sessionId);
                        break;
                    } else if ("SCANED".equals(qrCodeStatus)) {
                        // 二维码已被扫描，等待确认
                        if ("waiting".equals(session.getStatus())) {
                            session.setStatus("scanned");
                            logger.info("二维码已扫描，等待确认: {}", sessionId);
                        }
                    } else {
                        // 用户取消确认
                        session.setStatus("cancelled");
                        logger.info("用户取消登录: {}", sessionId);
                        break;
                    }
                    
                    Thread.sleep(800); // 每0.8秒检查一次
                    
                } catch (Exception e) {
                    logger.error("监控二维码状态异常", e);
                    Thread.sleep(2000);
                }
            }
            
            // 超时处理
            if (session != null && !Arrays.asList("success", "expired", "cancelled", "verification_required").contains(session.getStatus())) {
                session.setStatus("expired");
                logger.info("二维码监控超时，标记为过期: {}", sessionId);
            }
            
        } catch (Exception e) {
            logger.error("监控二维码状态失败", e);
            QRLoginSession session = sessions.get(sessionId);
            if (session != null) {
                session.setStatus("expired");
            }
        }
    }
    
    /**
     * 轮询二维码状态
     */
    private String pollQRCodeStatus(QRLoginSession session) throws IOException {
        FormBody.Builder formBuilder = new FormBody.Builder();
        session.getParams().forEach(formBuilder::add);
        
        Request request = new Request.Builder()
                .url(API_SCAN_STATUS)
                .headers(generateApiHeaders())
                .header("Cookie", CookieUtils.formatCookies(session.getCookies()))
                .post(formBuilder.build())
                .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful()) {
                String responseBody = response.body().string();
                JsonObject results = gson.fromJson(responseBody, JsonObject.class);
                JsonObject content = results.getAsJsonObject("content");
                
                if (content != null) {
                    JsonObject data = content.getAsJsonObject("data");
                    if (data != null) {
                        String qrCodeStatus = data.get("qrCodeStatus").getAsString();
                        
                        if ("CONFIRMED".equals(qrCodeStatus)) {
                            // 检查是否需要风控验证
                            if (data.has("iframeRedirect") && data.get("iframeRedirect").getAsBoolean()) {
                                session.setStatus("verification_required");
                                String iframeUrl = data.get("iframeRedirectUrl").getAsString();
                                session.setVerificationUrl(iframeUrl);
                                logger.warn("账号被风控，需要手机验证: {}, URL: {}", session.getSessionId(), iframeUrl);
                            } else {
                                // 登录成功，保存Cookie
                                session.setStatus("success");
                                List<String> cookieHeaders = response.headers("Set-Cookie");
                                for (String cookie : cookieHeaders) {
                                    String[] parts = cookie.split(";")[0].split("=", 2);
                                    if (parts.length == 2) {
                                        session.getCookies().put(parts[0], parts[1]);
                                        if ("unb".equals(parts[0])) {
                                            session.setUnb(parts[1]);
                                        }
                                    }
                                }
                            }
                        }
                        
                        return qrCodeStatus;
                    }
                }
            }
        }
        
        return "NEW";
    }

    
    @Override
    public QRStatusResponse getSessionStatus(String sessionId) {
        QRStatusResponse response = new QRStatusResponse();
        QRLoginSession session = sessions.get(sessionId);
        
        if (session == null) {
            response.setStatus("not_found");
            return response;
        }
        
        if (session.isExpired() && !"success".equals(session.getStatus())) {
            session.setStatus("expired");
        }
        
        response.setStatus(session.getStatus());
        response.setSessionId(sessionId);
        
        // 如果需要验证，返回验证URL
        if ("verification_required".equals(session.getStatus()) && session.getVerificationUrl() != null) {
            response.setVerificationUrl(session.getVerificationUrl());
            response.setMessage("账号被风控，需要手机验证");
        }
        
        // 如果登录成功，返回Cookie信息
        if ("success".equals(session.getStatus()) && !session.getCookies().isEmpty() && session.getUnb() != null) {
            response.setCookies(CookieUtils.formatCookies(session.getCookies()));
            response.setUnb(session.getUnb());
        }
        
        return response;
    }
    
    @Override
    public Map<String, String> getSessionCookies(String sessionId) {
        QRLoginSession session = sessions.get(sessionId);
        if (session != null && "success".equals(session.getStatus())) {
            Map<String, String> result = new HashMap<>();
            result.put("cookies", CookieUtils.formatCookies(session.getCookies()));
            result.put("unb", session.getUnb());
            return result;
        }
        return null;
    }
    
    @Override
    public void cleanupExpiredSessions() {
        List<String> expiredSessions = new ArrayList<>();
        sessions.forEach((sessionId, session) -> {
            if (session.isExpired()) {
                expiredSessions.add(sessionId);
            }
        });
        
        expiredSessions.forEach(sessionId -> {
            sessions.remove(sessionId);
            logger.info("清理过期会话: {}", sessionId);
        });
    }
    
    /**
     * MD5加密
     */
    private String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(input.getBytes());
            StringBuilder hexString = new StringBuilder();
            
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return hexString.toString();
        } catch (Exception e) {
            logger.error("MD5加密失败", e);
            return "";
        }
    }
}
