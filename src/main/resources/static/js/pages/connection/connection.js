// 连接管理页面模块
console.log('加载connection.js文件');

const ConnectionPage = {
    render(content) {
        console.log('渲染连接管理页面');
        content.innerHTML = `
            <div class="header" id="connection-header-id-1">
                <h1 class="welcome" id="connection-welcome-id-2">连接管理</h1>
            </div>

            <div class="connection-container" id="connection-container-id-3">
                <div class="connection-left-panel" id="connection-left-panel-id-4">
                    <div class="connection-header" id="connection-left-header-id-5">
                        <h2 class="connection-title" id="connection-left-title-id-6">闲鱼账号</h2>
                    </div>
                    <div class="account-list" id="accountList">
                        <div class="loading" id="account-list-loading-id-7">加载中...</div>
                    </div>
                </div>
                
                <div class="connection-right-panel" id="connection-right-panel-id-8">
                    <div class="connection-header" id="connection-right-header-id-9">
                        <h2 class="connection-title" id="connection-right-title-id-10">连接状态</h2>
                    </div>
                    <div id="connectionStatusContainer" class="empty-state">
                        <div class="empty-state-icon" id="connection-empty-icon-id-12">🔗</div>
                        <div class="empty-state-text" id="connection-empty-text-id-13">请选择一个账号查看连接状态</div>
                    </div>
                </div>
            </div>
        `;
        
        ConnectionManager.loadAccountList();
    }
};

console.log('ConnectionPage对象已定义:', typeof ConnectionPage);