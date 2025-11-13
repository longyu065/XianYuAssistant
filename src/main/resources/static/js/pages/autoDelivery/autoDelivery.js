// 自动发货页面模块
const AutoDeliveryPage = {
    render(content) {
        content.innerHTML = `
            <div class="header">
                <h1 class="welcome">自动发货</h1>
                <div class="header-actions">
                    <select class="form-select" id="deliveryAccountSelect" onchange="AutoDeliveryManager.loadConfigs()">
                        <option value="">选择账号</option>
                    </select>
                    <button class="btn btn-primary" onclick="AutoDeliveryManager.showAddConfigModal()">+ 添加配置</button>
                </div>
            </div>

            <div class="card">
                <div class="card-header">
                    <h2 class="card-title">发货配置</h2>
                </div>
                <div id="deliveryConfigList" class="empty-state">
                    <div class="empty-state-icon">🤖</div>
                    <div class="empty-state-text">请先选择账号</div>
                </div>
            </div>

            <!-- 配置模态框 -->
            <div id="deliveryConfigModal" class="modal">
                <div class="modal-content">
                    <div class="modal-header">
                        <h3 class="modal-title" id="deliveryConfigModalTitle">添加配置</h3>
                        <button class="modal-close" onclick="AutoDeliveryManager.hideConfigModal()">&times;</button>
                    </div>
                    <form id="deliveryConfigForm">
                        <input type="hidden" id="configId">
                        <div class="form-group">
                            <label class="form-label">商品ID</label>
                            <input type="text" class="form-input" id="goodId" placeholder="请输入商品ID">
                        </div>
                        <div class="form-group">
                            <label class="form-label">发货内容</label>
                            <textarea class="form-textarea" id="deliveryContent" placeholder="请输入发货内容" rows="4"></textarea>
                        </div>
                        <div class="form-group">
                            <label class="form-label">
                                <input type="checkbox" id="enableDelivery" style="margin-right: 8px;"> 启用自动发货
                            </label>
                        </div>
                        <div class="modal-footer">
                            <button type="button" class="btn btn-outline" onclick="AutoDeliveryManager.hideConfigModal()">取消</button>
                            <button type="submit" class="btn btn-primary">保存</button>
                        </div>
                    </form>
                </div>
            </div>
        `;
        
        document.getElementById('deliveryConfigForm').addEventListener('submit', (e) => {
            e.preventDefault();
            AutoDeliveryManager.saveConfig();
        });
        
        AutoDeliveryManager.loadAccountSelect();
    }
};
