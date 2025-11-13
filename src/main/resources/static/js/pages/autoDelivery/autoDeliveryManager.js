// 自动发货业务逻辑
const AutoDeliveryManager = {
    currentXianyuAccountId: null,
    
    // 加载账号选择器
    async loadAccountSelect() {
        try {
            const response = await API.account.list();
            if (response.code === 200 && response.data) {
                const select = document.getElementById('deliveryAccountSelect');
                if (select) {
                    select.innerHTML = '<option value="">选择账号</option>' +
                        response.data.map(account => 
                            `<option value="${account.id}">${account.accountName || account.userId}</option>`
                        ).join('');
                }
            }
        } catch (error) {
            console.error('加载账号列表失败:', error);
        }
    },
    
    // 加载配置列表
    async loadConfigs() {
        const select = document.getElementById('deliveryAccountSelect');
        const xianyuAccountId = select ? select.value : null;
        
        if (!xianyuAccountId) {
            Utils.showMessage('请先选择账号', 'warning');
            return;
        }
        
        this.currentXianyuAccountId = xianyuAccountId;
        
        const container = document.getElementById('deliveryConfigList');
        container.innerHTML = '<div class="loading">加载中...</div>';
        
        // TODO: 实现配置加载逻辑
        setTimeout(() => {
            container.innerHTML = `
                <div class="empty-state">
                    <div class="empty-state-icon">🤖</div>
                    <div class="empty-state-text">暂无配置数据</div>
                </div>
            `;
        }, 500);
    },
    
    // 显示添加配置模态框
    showAddConfigModal() {
        if (!this.currentXianyuAccountId) {
            Utils.showMessage('请先选择账号', 'warning');
            return;
        }
        document.getElementById('deliveryConfigModalTitle').textContent = '添加配置';
        document.getElementById('configId').value = '';
        document.getElementById('goodId').value = '';
        document.getElementById('deliveryContent').value = '';
        document.getElementById('enableDelivery').checked = false;
        document.getElementById('deliveryConfigModal').classList.add('show');
    },
    
    // 保存配置
    async saveConfig() {
        const data = {
            xianyuAccountId: this.currentXianyuAccountId,
            goodId: document.getElementById('goodId').value,
            content: document.getElementById('deliveryContent').value,
            enabled: document.getElementById('enableDelivery').checked
        };
        
        // TODO: 实现保存逻辑
        Utils.showMessage('保存成功', 'success');
        this.hideConfigModal();
        this.loadConfigs();
    },
    
    // 隐藏配置模态框
    hideConfigModal() {
        document.getElementById('deliveryConfigModal').classList.remove('show');
    }
};
