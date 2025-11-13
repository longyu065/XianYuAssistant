// 二维码登录页面模块
const QRLoginPage = {
    render(content) {
        content.innerHTML = `
            <div class="header">
                <h1 class="welcome">扫码添加</h1>
                <div class="header-actions">
                    <button class="btn btn-primary" onclick="QRLoginManager.generateQRCode()">生成二维码</button>
                </div>
            </div>

            <div class="card">
                <div class="card-header">
                    <h2 class="card-title">登录二维码</h2>
                </div>
                <div id="qrCodeContainer" class="empty-state">
                    <div class="empty-state-icon">📱</div>
                    <div class="empty-state-text">点击上方按钮生成二维码</div>
                </div>
            </div>

            <div class="card">
                <div class="card-header">
                    <h2 class="card-title">登录状态</h2>
                </div>
                <div id="loginStatus" class="empty-state">
                    <div class="empty-state-icon">⏳</div>
                    <div class="empty-state-text">等待扫码...</div>
                </div>
            </div>
        `;
    }
};
