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
            </div>
        `;
        
        MessageManager.loadAccountSelect();
    }
};
