// 操作记录业务逻辑
const RecordManager = {
    currentXianyuAccountId: null,
    
    // 加载账号选择器
    async loadAccountSelect() {
        try {
            const response = await API.account.list();
            if (response.code === 200 && response.data) {
                const select = document.getElementById('recordAccountSelect');
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
    
    // 加载记录列表
    async loadRecords() {
        const select = document.getElementById('recordAccountSelect');
        const xianyuAccountId = select ? select.value : null;
        
        if (!xianyuAccountId) {
            Utils.showMessage('请先选择账号', 'warning');
            return;
        }
        
        this.currentXianyuAccountId = xianyuAccountId;
        
        const container = document.getElementById('recordList');
        container.innerHTML = '<div class="loading">加载中...</div>';
        
        // TODO: 实现记录加载逻辑
        setTimeout(() => {
            container.innerHTML = `
                <div class="empty-state">
                    <div class="empty-state-icon">📝</div>
                    <div class="empty-state-text">暂无记录数据</div>
                </div>
            `;
        }, 500);
    }
};
