// 二维码登录业务逻辑
const QRLoginManager = {
    sessionId: null,
    checkInterval: null,
    
    // 生成二维码
    async generateQRCode() {
        try {
            const container = document.getElementById('qrCodeContainer');
            container.innerHTML = '<div class="loading">生成中...</div>';
            
            const response = await API.qrlogin.generate();
            if (response.code === 200 && response.data) {
                this.sessionId = response.data.sessionId;
                const qrCodeUrl = response.data.qrCodeUrl;
                
                container.innerHTML = `
                    <div style="text-align: center; padding: 20px;">
                        <img src="${qrCodeUrl}" alt="登录二维码" style="max-width: 300px;">
                        <p style="margin-top: 16px; color: #666;">请使用闲鱼APP扫描二维码登录</p>
                    </div>
                `;
                
                this.startCheckStatus();
            } else {
                throw new Error(response.message || '生成二维码失败');
            }
        } catch (error) {
            console.error('生成二维码失败:', error);
            Utils.showMessage('生成二维码失败: ' + error.message, 'error');
        }
    },
    
    // 开始检查登录状态
    startCheckStatus() {
        if (this.checkInterval) {
            clearInterval(this.checkInterval);
        }
        
        this.checkInterval = setInterval(() => {
            this.checkLoginStatus();
        }, 2000);
    },
    
    // 检查登录状态
    async checkLoginStatus() {
        if (!this.sessionId) return;
        
        try {
            const response = await API.qrlogin.status(this.sessionId);
            const statusContainer = document.getElementById('loginStatus');
            
            if (response.code === 200 && response.data) {
                const status = response.data.status;
                
                if (status === 'SCANNED') {
                    statusContainer.innerHTML = `
                        <div class="empty-state">
                            <div class="empty-state-icon">✓</div>
                            <div class="empty-state-text">已扫描，等待确认...</div>
                        </div>
                    `;
                } else if (status === 'CONFIRMED') {
                    clearInterval(this.checkInterval);
                    statusContainer.innerHTML = `
                        <div class="empty-state">
                            <div class="empty-state-icon">✓</div>
                            <div class="empty-state-text" style="color: #34c759;">登录成功！</div>
                        </div>
                    `;
                    Utils.showMessage('登录成功', 'success');
                    this.getCookies();
                }
            }
        } catch (error) {
            console.error('检查登录状态失败:', error);
        }
    },
    
    // 获取Cookie
    async getCookies() {
        try {
            const response = await API.qrlogin.cookies(this.sessionId);
            if (response.code === 200 && response.data) {
                console.log('获取到Cookie:', response.data);
                // TODO: 保存Cookie到账号
            }
        } catch (error) {
            console.error('获取Cookie失败:', error);
        }
    }
};