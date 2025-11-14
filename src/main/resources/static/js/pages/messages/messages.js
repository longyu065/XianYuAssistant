// 消息管理页面模块
const MessagesPage = {
    render(content) {
        content.innerHTML = `
            <div class="header">
                <h1 class="welcome">消息管理</h1>
                <div class="header-actions">
                    <select class="form-select" id="messageAccountSelect" onchange="MessageManager.loadMessages()">
                        <option value="">选择账号</option>
                    </select>
                    <button class="btn btn-outline" onclick="MessageManager.loadMessages()">刷新消息</button>
                </div>
            </div>

            <div class="card">
                <div class="card-header">
                    <h2 class="card-title">消息列表</h2>
                </div>
                <div id="messageList" class="empty-state">
                    <div class="empty-state-icon">💬</div>
                    <div class="empty-state-text">请先选择账号</div>
                </div>
                <!-- 分页控件容器 -->
                <div id="messagePagination" class="pagination-container card-pagination"></div>
            </div>
        `;
        
        // 确保 MessageManager 已加载后再调用
        if (typeof MessageManager !== 'undefined') {
            MessageManager.loadAccountSelect();
        } else {
            console.warn('MessageManager 未加载，延迟调用...');
            // 延迟调用，等待脚本加载完成
            setTimeout(() => {
                if (typeof MessageManager !== 'undefined') {
                    MessageManager.loadAccountSelect();
                } else {
                    console.error('MessageManager 仍未加载，请检查脚本加载顺序');
                }
            }, 100);
        }
    }
};
