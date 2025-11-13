// 仪表板页面模块
const DashboardPage = {
    render(content) {
        content.innerHTML = `
            <div class="header">
                <h1 class="welcome">欢迎使用闲鱼自动化管理系统</h1>
                <div class="header-actions">
                    <button class="btn btn-outline" onclick="location.reload()">刷新</button>
                </div>
            </div>

            <div class="stats-grid">
                <div class="stat-card">
                    <div class="stat-header">
                        <div class="stat-title">账号总数</div>
                        <div class="stat-icon" style="background: #d1f4e0; color: #34c759;">👤</div>
                    </div>
                    <div class="stat-value" id="accountCount">-</div>
                    <div class="stat-change">点击账号管理查看详情</div>
                </div>

                <div class="stat-card">
                    <div class="stat-header">
                        <div class="stat-title">在售商品</div>
                        <div class="stat-icon" style="background: #e5e0ff; color: #667eea;">📦</div>
                    </div>
                    <div class="stat-value" id="goodsCount">-</div>
                    <div class="stat-change">点击商品管理查看详情</div>
                </div>

                <div class="stat-card">
                    <div class="stat-header">
                        <div class="stat-title">WebSocket连接</div>
                        <div class="stat-icon" style="background: #fff4e5; color: #ff9500;">🔌</div>
                    </div>
                    <div class="stat-value" id="wsCount">-</div>
                    <div class="stat-change">实时消息监听</div>
                </div>

                <div class="stat-card">
                    <div class="stat-header">
                        <div class="stat-title">今日自动发货</div>
                        <div class="stat-icon" style="background: #e5f5ff; color: #007aff;">🚚</div>
                    </div>
                    <div class="stat-value" id="deliveryCount">-</div>
                    <div class="stat-change">成功率 98.5%</div>
                </div>
            </div>

            <div class="content-grid">
                <div class="card">
                    <div class="card-header">
                        <h2 class="card-title">系统概览</h2>
                    </div>
                    <div class="empty-state">
                        <div class="empty-state-icon">📊</div>
                        <div class="empty-state-text">数据统计功能开发中</div>
                    </div>
                </div>

                <div class="card">
                    <div class="card-header">
                        <h2 class="card-title">最近活动</h2>
                    </div>
                    <div class="empty-state">
                        <div class="empty-state-icon">📝</div>
                        <div class="empty-state-text">活动记录功能开发中</div>
                    </div>
                </div>
            </div>
        `;
        
        this.loadStats();
    },
    
    async loadStats() {
        try {
            setTimeout(() => {
                document.getElementById('accountCount').textContent = '8';
                document.getElementById('goodsCount').textContent = '156';
                document.getElementById('wsCount').textContent = '6';
                document.getElementById('deliveryCount').textContent = '47';
            }, 500);
        } catch (error) {
            console.error('加载统计数据失败:', error);
        }
    }
};
