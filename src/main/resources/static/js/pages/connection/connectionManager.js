// 连接管理业务逻辑
console.log('加载connectionManager.js文件');

const ConnectionManager = {
    currentAccountId: null,
    connectionStatusInterval: null,
    
    // 加载账号列表
    async loadAccountList() {
        console.log('加载账号列表');
        try {
            const response = await API.account.list();
            const accountListContainer = document.getElementById('accountList');
            
            if (!accountListContainer) {
                console.error('找不到账号列表容器');
                return;
            }
            
            if (response.code === 200 && response.data && response.data.accounts) {
                const accounts = response.data.accounts;
                
                if (accounts.length === 0) {
                    accountListContainer.innerHTML = `
                        <div class="empty-state">
                            <div class="empty-state-icon">👤</div>
                            <div class="empty-state-text">暂无账号数据</div>
                        </div>
                    `;
                    return;
                }
                
                accountListContainer.innerHTML = accounts.map(account => `
                    <div class="account-item" id="account-item-${account.id}" onclick="ConnectionManager.selectAccount(${account.id})">
                        <div class="account-avatar">${(account.accountNote || account.unb || '闲')[0]}</div>
                        <div class="account-info">
                            <div class="account-name">${account.accountNote || account.unb || '未命名账号'}</div>
                            <div class="account-id">ID: ${account.id}</div>
                        </div>
                    </div>
                `).join('');
            } else {
                accountListContainer.innerHTML = `
                    <div class="empty-state">
                        <div class="empty-state-icon">❌</div>
                        <div class="empty-state-text">加载账号列表失败</div>
                    </div>
                `;
            }
        } catch (error) {
            console.error('加载账号列表失败:', error);
            const accountListContainer = document.getElementById('accountList');
            if (accountListContainer) {
                accountListContainer.innerHTML = `
                    <div class="empty-state">
                        <div class="empty-state-icon">❌</div>
                        <div class="empty-state-text">加载账号列表失败: ${error.message}</div>
                    </div>
                `;
            }
        }
    },
    
    // 选择账号
    selectAccount(accountId) {
        console.log('选择账号:', accountId);
        // 更新选中状态
        document.querySelectorAll('.account-item').forEach(item => {
            item.classList.remove('active');
        });
        const selectedItem = document.getElementById(`account-item-${accountId}`);
        if (selectedItem) {
            selectedItem.classList.add('active');
        }
        
        this.currentAccountId = accountId;
        this.loadConnectionStatus(accountId);
        
        // 启动定时刷新状态
        if (this.connectionStatusInterval) {
            clearInterval(this.connectionStatusInterval);
        }
        this.connectionStatusInterval = setInterval(() => {
            if (this.currentAccountId) {
                this.loadConnectionStatus(this.currentAccountId);
            }
        }, 5000); // 每5秒刷新一次
    },
    
    // 加载连接状态
    async loadConnectionStatus(accountId) {
        console.log('加载连接状态，账号ID:', accountId);
        try {
            const response = await API.websocket.status(accountId);
            const statusContainer = document.getElementById('connectionStatusContainer');
            
            if (!statusContainer) {
                console.error('找不到状态容器');
                return;
            }
            
            if (response.code === 200 && response.data) {
                const status = response.data;
                statusContainer.innerHTML = `
                    <div class="connection-status-card">
                        <div class="connection-status-header">
                            <h3 class="connection-status-title">连接信息</h3>
                            <span class="status-indicator ${status.connected ? 'status-connected' : 'status-disconnected'}">
                                ${status.connected ? '已连接' : '未连接'}
                            </span>
                        </div>
                        <div class="connection-details">
                            <div class="detail-item">
                                <span class="detail-label">账号ID</span>
                                <span class="detail-value">${status.xianyuAccountId}</span>
                            </div>
                            <div class="detail-item">
                                <span class="detail-label">连接状态</span>
                                <span class="detail-value">${status.status}</span>
                            </div>
                        </div>
                    </div>
                    
                    <div class="connection-actions">
                        ${status.connected ? 
                            `<button class="btn-connection btn-connection-danger" onclick="ConnectionManager.stopConnection(${accountId})">断开连接</button>` :
                            `<button class="btn-connection btn-connection-primary" onclick="ConnectionManager.startConnection(${accountId})">启动连接</button>`
                        }
                        <button class="btn-connection btn-connection-outline" onclick="ConnectionManager.refreshStatus(${accountId})">刷新状态</button>
                    </div>
                    
                    <div class="connection-logs">
                        <div class="logs-header">操作日志</div>
                        <div class="logs-container" id="connectionLogs">
                            <div class="log-entry">
                                <span class="log-timestamp">[${new Date().toLocaleTimeString()}]</span>
                                <span class="log-message">当前状态: ${status.status}</span>
                            </div>
                        </div>
                    </div>
                `;
            } else {
                statusContainer.innerHTML = `
                    <div class="empty-state">
                        <div class="empty-state-icon">❌</div>
                        <div class="empty-state-text">加载连接状态失败</div>
                    </div>
                `;
            }
        } catch (error) {
            console.error('加载连接状态失败:', error);
            const statusContainer = document.getElementById('connectionStatusContainer');
            if (statusContainer) {
                statusContainer.innerHTML = `
                    <div class="empty-state">
                        <div class="empty-state-icon">❌</div>
                        <div class="empty-state-text">加载连接状态失败: ${error.message}</div>
                    </div>
                `;
            }
        }
    },
    
    // 启动连接
    async startConnection(accountId) {
        console.log('启动连接，账号ID:', accountId);
        try {
            // 显示加载状态
            const statusContainer = document.getElementById('connectionStatusContainer');
            if (statusContainer) {
                statusContainer.innerHTML = '<div class="loading">正在启动连接...</div>';
            }
            
            const response = await API.websocket.start(accountId);
            
            if (response.code === 200) {
                Utils.showMessage('连接启动成功', 'success');
                // 重新加载状态
                this.loadConnectionStatus(accountId);
            } else {
                Utils.showMessage(`连接启动失败: ${response.message}`, 'error');
                // 重新加载状态
                this.loadConnectionStatus(accountId);
            }
        } catch (error) {
            console.error('启动连接失败:', error);
            Utils.showMessage(`启动连接失败: ${error.message}`, 'error');
            // 重新加载状态
            this.loadConnectionStatus(accountId);
        }
    },
    
    // 停止连接
    async stopConnection(accountId) {
        console.log('停止连接，账号ID:', accountId);
        try {
            // 显示加载状态
            const statusContainer = document.getElementById('connectionStatusContainer');
            if (statusContainer) {
                statusContainer.innerHTML = '<div class="loading">正在断开连接...</div>';
            }
            
            const response = await API.websocket.stop(accountId);
            
            if (response.code === 200) {
                Utils.showMessage('连接已断开', 'success');
                // 重新加载状态
                this.loadConnectionStatus(accountId);
            } else {
                Utils.showMessage(`断开连接失败: ${response.message}`, 'error');
                // 重新加载状态
                this.loadConnectionStatus(accountId);
            }
        } catch (error) {
            console.error('断开连接失败:', error);
            Utils.showMessage(`断开连接失败: ${error.message}`, 'error');
            // 重新加载状态
            this.loadConnectionStatus(accountId);
        }
    },
    
    // 刷新状态
    refreshStatus(accountId) {
        console.log('刷新状态，账号ID:', accountId);
        this.loadConnectionStatus(accountId);
        Utils.showMessage('状态已刷新', 'info');
    },
    
    // 添加日志
    addLog(message, isError = false) {
        const logsContainer = document.getElementById('connectionLogs');
        if (logsContainer) {
            const logEntry = document.createElement('div');
            logEntry.className = 'log-entry';
            logEntry.innerHTML = `
                <span class="log-timestamp">[${new Date().toLocaleTimeString()}]</span>
                <span class="log-message ${isError ? 'log-error' : ''}">${message}</span>
            `;
            logsContainer.appendChild(logEntry);
            logsContainer.scrollTop = logsContainer.scrollHeight;
        }
    }
};

console.log('ConnectionManager对象已定义:', typeof ConnectionManager);