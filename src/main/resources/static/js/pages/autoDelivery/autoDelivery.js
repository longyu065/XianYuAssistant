// 自动发货页面模块
const AutoDeliveryPage = {
    render(content) {
        content.innerHTML = `
            <div class="header" id="delivery-header-id-1">
                <h1 class="welcome" id="delivery-welcome-id-2">自动发货</h1>
                <div class="header-actions" id="delivery-header-actions-id-3">
                    <select class="form-select" id="deliveryAccountSelect" onchange="AutoDeliveryManager.loadConfigs()">
                        <option value="" id="delivery-option-default-id-4">选择账号</option>
                    </select>
                    <button class="btn btn-primary" id="delivery-add-btn-id-5" onclick="AutoDeliveryManager.showAddConfigModal()">+ 添加配置</button>
                </div>
            </div>

            <div class="card" id="delivery-card-id-6">
                <div class="card-header" id="delivery-card-header-id-7">
                    <h2 class="card-title" id="delivery-card-title-id-8">发货配置</h2>
                </div>
                <div id="deliveryConfigList" class="empty-state" id="delivery-config-list-id-9">
                    <div class="empty-state-icon" id="delivery-empty-icon-id-10">🤖</div>
                    <div class="empty-state-text" id="delivery-empty-text-id-11">请先选择账号</div>
                </div>
            </div>

            <!-- 配置模态框 -->
            <div id="deliveryConfigModal" class="modal">
                <div class="modal-content" id="delivery-modal-content-id-12">
                    <div class="modal-header" id="delivery-modal-header-id-13">
                        <h3 class="modal-title" id="deliveryConfigModalTitle">添加配置</h3>
                        <button class="modal-close" id="delivery-modal-close-id-14" onclick="AutoDeliveryManager.hideConfigModal()">&times;</button>
                    </div>
                    <form id="deliveryConfigForm">
                        <input type="hidden" id="configId">
                        <div class="form-group" id="delivery-goodid-group-id-15">
                            <label class="form-label" id="delivery-goodid-label-id-16">商品ID</label>
                            <input type="text" class="form-input" id="goodId" placeholder="请输入商品ID">
                        </div>
                        <div class="form-group" id="delivery-content-group-id-17">
                            <label class="form-label" id="delivery-content-label-id-18">发货内容</label>
                            <textarea class="form-textarea" id="deliveryContent" placeholder="请输入发货内容" rows="4"></textarea>
                        </div>
                        <div class="form-group" id="delivery-enable-group-id-19">
                            <label class="form-label" id="delivery-enable-label-id-20">
                                <input type="checkbox" id="enableDelivery" style="margin-right: 8px;"> 启用自动发货
                            </label>
                        </div>
                        <div class="modal-footer" id="delivery-modal-footer-id-21">
                            <button type="button" class="btn btn-outline" id="delivery-cancel-btn-id-22" onclick="AutoDeliveryManager.hideConfigModal()">取消</button>
                            <button type="submit" class="btn btn-primary" id="delivery-save-btn-id-23">保存</button>
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
