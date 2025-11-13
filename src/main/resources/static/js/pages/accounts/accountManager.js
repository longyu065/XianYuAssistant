// 账号管理业务逻辑
const AccountManager = {
    currentAccounts: [],
    
    // 加载账号列表
    async loadAccounts() {
        try {
            console.log('开始加载账号列表');
            const response = await API.account.list();
            console.log('账号列表API响应:', response);
            
            if (response.code === 200 && response.data && response.data.accounts) {
                console.log('账号列表数据:', response.data.accounts);
                this.currentAccounts = response.data.accounts;
                this.renderAccountList(response.data.accounts);
            } else {
                console.error('账号列表响应格式不正确:', response);
                throw new Error(response.msg || '获取账号列表失败');
            }
        } catch (error) {
            console.error('加载账号列表失败:', error);
            Utils.showMessage('加载账号列表失败: ' + error.message, 'error');
        }
    },
    
    // 渲染账号列表
    renderAccountList(accounts) {
        const container = document.getElementById('accountList');
        container.className = 'account-list-container'; // 添加容器类名
        
        // 确保accounts是一个数组
        if (!Array.isArray(accounts) || accounts.length === 0) {
            container.innerHTML = `
                <div class="empty-state">
                    <div class="empty-state-icon">👤</div>
                    <div class="empty-state-text">暂无账号数据</div>
                </div>
            `;
            return;
        }
        
        // 使用表格形式展示账号列表
        container.innerHTML = `
            <div class="account-table-wrapper">
                <table class="account-table">
                    <thead>
                        <tr>
                            <th>ID</th>
                            <th>UNB</th>
                            <th>账号备注</th>
                            <th>状态</th>
                            <th>创建时间</th>
                            <th>更新时间</th>
                            <th>操作</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${accounts.map(account => `
                            <tr>
                                <td>${account.id}</td>
                                <td>${account.unb || '-'}</td>
                                <td>${account.accountNote || '未命名账号'}</td>
                                <td><span class="${account.status === 1 ? 'account-status-active' : 'account-status-inactive'}">${account.status === 1 ? '正常' : '异常'}</span></td>
                                <td>${account.createdTime || '-'}</td>
                                <td>${account.updatedTime || '-'}</td>
                                <td>
                                    <button class="btn btn-outline btn-small" onclick="AccountManager.editAccount(${account.id})">编辑</button>
                                    <button class="btn btn-danger btn-small" onclick="AccountManager.showDeleteConfirmModal(${account.id})">删除</button>
                                </td>
                            </tr>
                        `).join('')}
                    </tbody>
                </table>
            </div>
        `;
    },
    
    // 显示添加模态框
    showAddModal() {
        document.getElementById('accountModalTitle').textContent = '添加账号';
        document.getElementById('accountId').value = '';
        document.getElementById('accountName').value = '';
        document.getElementById('accountModal').classList.add('show');
    },
    
    // 显示手动添加账号模态框
    showManualAddModal() {
        document.getElementById('manualAccountNote').value = '';
        document.getElementById('manualAccountCookie').value = '';
        document.getElementById('manualAddAccountModal').classList.add('show');
    },
    
    // 隐藏手动添加账号模态框
    hideManualAddModal() {
        document.getElementById('manualAddAccountModal').classList.remove('show');
    },
    
    // 编辑账号
    editAccount(id) {
        const account = this.currentAccounts.find(a => a.id === id);
        if (!account) return;
        
        document.getElementById('accountModalTitle').textContent = '编辑账号';
        document.getElementById('accountId').value = account.id;
        document.getElementById('accountName').value = account.accountNote || '';
        document.getElementById('accountModal').classList.add('show');
    },
    
    // 保存账号
    async saveAccount() {
        const id = document.getElementById('accountId').value;
        const accountId = document.getElementById('accountId').value;
        const accountNote = document.getElementById('accountName').value;
        
        // 只更新账号备注
        const data = {
            accountId: parseInt(accountId),
            accountNote: accountNote
        };
        
        try {
            const response = id ? 
                await API.account.update(data) :
                await API.account.add(data);
                
            if (response.code === 200) {
                Utils.showMessage(id ? '更新成功' : '添加成功', 'success');
                this.hideModal();
                this.loadAccounts();
            } else {
                throw new Error(response.msg || '保存失败');
            }
        } catch (error) {
            console.error('保存账号失败:', error);
            Utils.showMessage('保存失败: ' + error.message, 'error');
        }
    },
    
    // 保存手动添加的账号
    async saveManualAddAccount() {
        const accountNote = document.getElementById('manualAccountNote').value.trim();
        const cookie = document.getElementById('manualAccountCookie').value.trim();
        
        // 验证输入
        if (!accountNote) {
            Utils.showMessage('请输入账号备注', 'error');
            return;
        }
        
        if (!cookie) {
            Utils.showMessage('请输入Cookie', 'error');
            return;
        }
        
        try {
            // 调用后端接口保存手动添加的账号
            const response = await fetch('/api/account/manualAdd', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    accountNote: accountNote,
                    cookie: cookie
                })
            });
            
            const result = await response.json();
            
            if (result.code === 200) {
                Utils.showMessage('账号添加成功', 'success');
                this.hideManualAddModal();
                this.loadAccounts();
            } else {
                Utils.showMessage(result.msg || '添加失败', 'error');
            }
        } catch (error) {
            console.error('保存手动添加账号失败:', error);
            Utils.showMessage('保存失败: ' + error.message, 'error');
        }
    },
    
    // 显示删除确认模态框
    showDeleteConfirmModal(id) {
        document.getElementById('deleteAccountId').value = id;
        document.getElementById('deleteConfirmModal').classList.add('show');
    },
    
    // 隐藏删除确认模态框
    hideDeleteConfirmModal() {
        document.getElementById('deleteConfirmModal').classList.remove('show');
    },
    
    // 确认删除账号
    async confirmDeleteAccount() {
        const id = document.getElementById('deleteAccountId').value;
        if (!id) return;
        
        try {
            const response = await API.account.delete({ accountId: parseInt(id) });
            if (response.code === 200) {
                Utils.showMessage('账号删除成功', 'success');
                this.hideDeleteConfirmModal();
                this.loadAccounts();
            } else {
                throw new Error(response.msg || '删除失败');
            }
        } catch (error) {
            console.error('删除账号失败:', error);
            Utils.showMessage('删除失败: ' + error.message, 'error');
        }
    },
    
    // 删除账号（保持兼容性）
    async deleteAccount(id) {
        // 直接显示删除确认模态框
        this.showDeleteConfirmModal(id);
    },
    
    // 隐藏模态框
    hideModal() {
        document.getElementById('accountModal').classList.remove('show');
    },
    
    // 显示扫码登录模态框
    showQRLoginModal() {
        document.getElementById('qrLoginModal').classList.add('show');
        this.generateQRCode();
    },
    
    // 隐藏扫码登录模态框
    hideQRLoginModal() {
        document.getElementById('qrLoginModal').classList.remove('show');
        // 清理定时器
        if (this.qrLoginInterval) {
            clearInterval(this.qrLoginInterval);
            this.qrLoginInterval = null;
        }
    },
    
    // 生成二维码
    async generateQRCode() {
        try {
            const response = await API.qrlogin.generate();
            if (response.code === 200 && response.data) {
                const qrData = response.data;
                const qrContainer = document.getElementById('qrCodeContainer');
                qrContainer.innerHTML = `
                    <img src="${qrData.qrCodeUrl}" 
                         alt="二维码登录" 
                         style="max-width: 200px; border: 1px solid #ddd; border-radius: 8px;">
                    <p style="margin: 10px 0; font-size: 12px; color: #999;">会话ID: ${qrData.sessionId}</p>
                `;
                
                // 开始轮询检查登录状态
                this.pollQRLoginStatus(qrData.sessionId);
            } else {
                throw new Error(response.msg || '生成二维码失败');
            }
        } catch (error) {
            console.error('生成二维码失败:', error);
            const qrContainer = document.getElementById('qrCodeContainer');
            qrContainer.innerHTML = `<div class="error">生成二维码失败: ${error.message}</div>`;
        }
    },
    
    // 轮询检查登录状态
    pollQRLoginStatus(sessionId) {
        // 清理之前的定时器
        if (this.qrLoginInterval) {
            clearInterval(this.qrLoginInterval);
        }
        
        // 设置新的定时器，每2秒检查一次
        this.qrLoginInterval = setInterval(async () => {
            try {
                const response = await API.qrlogin.status(sessionId);
                const statusDiv = document.getElementById('qrLoginStatus');
                
                if (response.code === 200 && response.data) {
                    const statusData = response.data;
                    
                    switch (statusData.status) {
                        case 'waiting':
                            statusDiv.innerHTML = '<span style="color: #ff9500;">等待扫码...</span>';
                            break;
                        case 'scanned':
                            statusDiv.innerHTML = '<span style="color: #007aff;">已扫码，等待确认...</span>';
                            break;
                        case 'success':
                            statusDiv.innerHTML = '<span style="color: #34c759;">登录成功！正在获取信息...</span>';
                            // 登录成功，获取Cookie并保存账号
                            await this.handleLoginSuccess(sessionId);
                            break;
                        case 'cancelled':
                            statusDiv.innerHTML = '<span style="color: #ff3b30;">登录已取消</span>';
                            clearInterval(this.qrLoginInterval);
                            this.qrLoginInterval = null;
                            break;
                        case 'expired':
                            statusDiv.innerHTML = '<span style="color: #ff3b30;">二维码已过期</span>';
                            clearInterval(this.qrLoginInterval);
                            this.qrLoginInterval = null;
                            break;
                        default:
                            statusDiv.innerHTML = '<span style="color: #666;">未知状态: ' + statusData.status + '</span>';
                    }
                } else {
                    throw new Error(response.msg || '检查登录状态失败');
                }
            } catch (error) {
                console.error('检查登录状态失败:', error);
                const statusDiv = document.getElementById('qrLoginStatus');
                statusDiv.innerHTML = `<span style="color: #ff3b30;">检查状态失败: ${error.message}</span>`;
            }
        }, 2000);
    },
    
    // 处理登录成功
    async handleLoginSuccess(sessionId) {
        try {
            // 获取Cookie信息
            const cookieResponse = await API.qrlogin.cookies(sessionId);
            if (cookieResponse.code === 200 && cookieResponse.data) {
                const cookies = cookieResponse.data;
                
                // 提取必要的信息
                const unb = cookies.unb || '';
                const cookieText = Object.entries(cookies)
                    .map(([key, value]) => `${key}=${value}`)
                    .join('; ');
                
                // 生成默认账号备注
                const accountNote = `账号_${unb || new Date().getTime()}`;
                
                // 调用添加账号接口
                const addResponse = await API.account.add({
                    accountNote: accountNote,
                    unb: unb,
                    cookie: cookieText
                });
                
                if (addResponse.code === 200) {
                    Utils.showMessage('账号添加成功', 'success');
                    this.hideQRLoginModal();
                    this.loadAccounts();
                } else {
                    throw new Error(addResponse.msg || '添加账号失败');
                }
            } else {
                throw new Error(cookieResponse.msg || '获取Cookie信息失败');
            }
        } catch (error) {
            console.error('处理登录成功失败:', error);
            Utils.showMessage('处理登录成功失败: ' + error.message, 'error');
        } finally {
            // 清理定时器
            if (this.qrLoginInterval) {
                clearInterval(this.qrLoginInterval);
                this.qrLoginInterval = null;
            }
        }
    }
};