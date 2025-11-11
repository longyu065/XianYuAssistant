# API测试示例

## 使用curl测试

### 1. 生成二维码

```bash
curl -X POST http://localhost:8080/api/qrlogin/generate
```

响应示例：
```json
{
  "success": true,
  "sessionId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "qrCodeUrl": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAA...",
  "message": null
}
```

### 2. 查询登录状态

```bash
curl http://localhost:8080/api/qrlogin/status/a1b2c3d4-e5f6-7890-abcd-ef1234567890
```

响应示例（等待扫码）：
```json
{
  "status": "waiting",
  "sessionId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "cookies": null,
  "unb": null,
  "verificationUrl": null,
  "message": null
}
```

响应示例（登录成功）：
```json
{
  "status": "success",
  "sessionId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "cookies": "cookie_name1=value1; cookie_name2=value2; ...",
  "unb": "123456789",
  "verificationUrl": null,
  "message": null
}
```

### 3. 获取Cookie

```bash
curl http://localhost:8080/api/qrlogin/cookies/a1b2c3d4-e5f6-7890-abcd-ef1234567890
```

响应示例：
```json
{
  "cookies": "cookie_name1=value1; cookie_name2=value2; ...",
  "unb": "123456789"
}
```

### 4. 清理过期会话

```bash
curl -X POST http://localhost:8080/api/qrlogin/cleanup
```

## 使用Postman测试

### 生成二维码
- Method: POST
- URL: http://localhost:8080/api/qrlogin/generate
- Headers: Content-Type: application/json

### 查询状态
- Method: GET
- URL: http://localhost:8080/api/qrlogin/status/{sessionId}

### 获取Cookie
- Method: GET
- URL: http://localhost:8080/api/qrlogin/cookies/{sessionId}

## 完整测试流程

1. 调用生成二维码接口，获取sessionId和qrCodeUrl
2. 将qrCodeUrl（base64图片）在浏览器中打开或保存为图片
3. 使用闲鱼APP扫描二维码
4. 每隔1秒调用状态查询接口，直到status变为"success"
5. 调用获取Cookie接口，获取完整的Cookie信息
6. 使用获取到的Cookie进行后续的闲鱼API调用

## JavaScript示例

```javascript
// 生成二维码
async function testQRLogin() {
    // 1. 生成二维码
    const generateResponse = await fetch('http://localhost:8080/api/qrlogin/generate', {
        method: 'POST'
    });
    const generateData = await generateResponse.json();
    
    if (!generateData.success) {
        console.error('生成二维码失败:', generateData.message);
        return;
    }
    
    const sessionId = generateData.sessionId;
    console.log('SessionId:', sessionId);
    console.log('二维码URL:', generateData.qrCodeUrl);
    
    // 2. 轮询状态
    const checkStatus = async () => {
        const statusResponse = await fetch(`http://localhost:8080/api/qrlogin/status/${sessionId}`);
        const statusData = await statusResponse.json();
        
        console.log('当前状态:', statusData.status);
        
        if (statusData.status === 'success') {
            console.log('登录成功！');
            console.log('Cookie:', statusData.cookies);
            console.log('UNB:', statusData.unb);
            clearInterval(interval);
        } else if (statusData.status === 'expired' || statusData.status === 'cancelled') {
            console.log('登录失败:', statusData.status);
            clearInterval(interval);
        }
    };
    
    const interval = setInterval(checkStatus, 1000);
}

// 执行测试
testQRLogin();
```

## Java示例

```java
@Autowired
private QRLoginService qrLoginService;

public void testQRLogin() throws InterruptedException {
    // 1. 生成二维码
    QRLoginResponse response = qrLoginService.generateQRCode();
    
    if (!response.isSuccess()) {
        System.out.println("生成二维码失败: " + response.getMessage());
        return;
    }
    
    String sessionId = response.getSessionId();
    System.out.println("SessionId: " + sessionId);
    System.out.println("二维码URL: " + response.getQrCodeUrl());
    
    // 2. 轮询状态
    while (true) {
        QRStatusResponse status = qrLoginService.getSessionStatus(sessionId);
        System.out.println("当前状态: " + status.getStatus());
        
        if ("success".equals(status.getStatus())) {
            System.out.println("登录成功！");
            System.out.println("Cookie: " + status.getCookies());
            System.out.println("UNB: " + status.getUnb());
            break;
        } else if ("expired".equals(status.getStatus()) || 
                   "cancelled".equals(status.getStatus())) {
            System.out.println("登录失败: " + status.getStatus());
            break;
        }
        
        Thread.sleep(1000);
    }
}
```
