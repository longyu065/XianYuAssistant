// 消息管理业务逻辑
const MessageManager = {
    currentXianyuAccountId: null,
    
    // 加载账号选择器
    async loadAccountSelect() {
        try {
            const response = await API.account.list();
            if (response.code === 200 && response.data) {
                const select = document.getElementById('messageAccountSelect');
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
    
    // 加载消息列表
    async loadMessages() {
        const select = document.getElementById('messageAccountSelect');
        const xianyuAccountId = select ? select.value : null;
        
        if (!xianyuAccountId) {
            Utils.showMessage('请先选择账号', 'warning');
            return;
        }
        
        this.currentXianyuAccountId = xianyuAccountId;
        
        const container = document.getElementById('messageList');
        container.innerHTML = '<div class="loading">加载中...</div>';
        
        // TODO: 实现消息加载逻辑
        setTimeout(() => {
            container.innerHTML = `
                <div class="empty-state">
                    <div class="empty-state-icon">💬</div>
                    <div class="empty-state-text">暂无消息数据</div>
                </div>
            `;
        }, 500);
    }
};
