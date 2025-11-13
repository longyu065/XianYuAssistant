// 账号管理页面模块
const AccountsPage = {
    render(content) {
        content.innerHTML = `
            <div class="header">
                <h1 class="welcome">闲鱼账号</h1>
                <div class="header-actions">
                    <button class="btn btn-primary" onclick="AccountManager.showQRLoginModal()">📱 扫码添加闲鱼账号</button>
                    <button class="btn btn-outline" onclick="AccountManager.showManualAddModal()">+ 手动添加</button>
                </div>
            </div>

            <div class="card">
                <div class="card-header">
                    <h2 class="card-title">闲鱼账号列表</h2>
                </div>
                <div id="accountList" class="empty-state">
                    <div class="loading">加载中</div>
                </div>
            </div>

            <!-- 添加/编辑账号模态框 -->
            <div id="accountModal" class="modal">
                <div class="modal-content">
                    <div class="modal-header">
                        <h3 class="modal-title" id="accountModalTitle">添加闲鱼账号</h3>
                        <button class="modal-close" onclick="AccountManager.hideModal()">&times;</button>
                    </div>
                    <form id="accountForm">
                        <input type="hidden" id="accountId">
                        <div class="form-group">
                            <label class="form-label">账号备注</label>
                            <input type="text" class="form-input" id="accountName" placeholder="请输入账号备注">
                        </div>
                        <div class="modal-footer">
                            <button type="button" class="btn btn-outline" onclick="AccountManager.hideModal()">取消</button>
                            <button type="submit" class="btn btn-primary">保存</button>
                        </div>
                    </form>
                </div>
            </div>
            
            <!-- 手动添加账号模态框 -->
            <div id="manualAddAccountModal" class="modal">
                <div class="modal-content">
                    <div class="modal-header">
                        <h3 class="modal-title">手动添加闲鱼账号</h3>
                        <button class="modal-close" onclick="AccountManager.hideManualAddModal()">&times;</button>
                    </div>
                    <form id="manualAddAccountForm">
                        <div class="form-group">
                            <label class="form-label">账号备注</label>
                            <input type="text" class="form-input" id="manualAccountNote" placeholder="请输入账号备注">
                        </div>
                        <div class="form-group">
                            <label class="form-label">Cookie</label>
                            <textarea class="form-input" id="manualAccountCookie" placeholder="请输入Cookie" rows="6"></textarea>
                        </div>
                        <div class="modal-footer">
                            <button type="button" class="btn btn-outline" onclick="AccountManager.hideManualAddModal()">取消</button>
                            <button type="submit" class="btn btn-primary">添加账号</button>
                        </div>
                    </form>
                </div>
            </div>
            
            <!-- 扫码登录模态框 -->
            <div id="qrLoginModal" class="modal">
                <div class="modal-content" style="max-width: 400px;">
                    <div class="modal-header">
                        <h3 class="modal-title">扫码添加闲鱼账号</h3>
                        <button class="modal-close" onclick="AccountManager.hideQRLoginModal()">&times;</button>
                    </div>
                    <div class="modal-body" style="text-align: center; padding: 20px;">
                        <div id="qrCodeContainer" style="margin: 20px 0;">
                            <div class="loading">正在生成二维码...</div>
                        </div>
                        <p style="margin: 10px 0; color: #666;">请使用闲鱼APP扫描二维码登录</p>
                        <div id="qrLoginStatus" style="margin: 10px 0; min-height: 20px;"></div>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-outline" onclick="AccountManager.hideQRLoginModal()">取消</button>
                    </div>
                </div>
            </div>
            
            <!-- 删除确认模态框 -->
            <div id="deleteConfirmModal" class="modal">
                <div class="modal-content" style="max-width: 500px;">
                    <div class="modal-header">
                        <h3 class="modal-title">删除账号确认</h3>
                        <button class="modal-close" onclick="AccountManager.hideDeleteConfirmModal()">&times;</button>
                    </div>
                    <div class="modal-body" style="padding: 20px 0;">
                        <p style="margin: 0 0 20px 0; color: #1a1a1a; font-size: 16px;">
                            确定要删除这个账号吗？
                        </p>
                        <div style="background: #fff3f3; border: 1px solid #ffcccc; border-radius: 8px; padding: 16px; margin: 20px 0;">
                            <p style="margin: 0 0 10px 0; font-weight: 600; color: #ff3b30;">重要提醒</p>
                            <p style="margin: 0; color: #333; font-size: 14px; line-height: 1.5;">
                                删除账号将会同时删除该账号下的所有相关数据，包括：
                            </p>
                            <ul style="margin: 10px 0 0 20px; padding: 0; color: #333; font-size: 14px;">
                                <li>聊天消息记录</li>
                                <li>商品信息</li>
                                <li>自动发货配置和记录</li>
                                <li>自动回复配置和记录</li>
                                <li>Cookie信息</li>
                            </ul>
                            <p style="margin: 10px 0 0 0; color: #333; font-size: 14px; font-weight: 600;">
                                此操作不可恢复，请谨慎操作！
                            </p>
                        </div>
                        <input type="hidden" id="deleteAccountId">
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-outline" onclick="AccountManager.hideDeleteConfirmModal()">取消</button>
                        <button type="button" class="btn btn-danger" onclick="AccountManager.confirmDeleteAccount()">确定删除</button>
                    </div>
                </div>
            </div>
        `;
        
        // 绑定表单提交事件
        document.getElementById('accountForm').addEventListener('submit', (e) => {
            e.preventDefault();
            AccountManager.saveAccount();
        });
        
        // 绑定手动添加账号表单提交事件
        document.getElementById('manualAddAccountForm').addEventListener('submit', (e) => {
            e.preventDefault();
            AccountManager.saveManualAddAccount();
        });
        
        // 加载账号列表
        AccountManager.loadAccounts();
    }
};