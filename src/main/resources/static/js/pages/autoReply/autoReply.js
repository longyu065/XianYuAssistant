// 自动回复页面模块
const AutoReplyPage = {
    render(content) {
        content.innerHTML = `
            <div class="header">
                <h1 class="welcome">自动回复</h1>
                <div class="header-actions">
                    <select class="form-select" id="replyAccountSelect" onchange="AutoReplyManager.loadConfigs()">
                        <option value="">选择账号</option>
                    </select>
                    <button class="btn btn-primary" onclick="AutoReplyManager.showAddConfigModal()">+ 添加配置</button>
                </div>
            </div>

            <div class="card">
                <div class="card-header">
                    <h2 class="card-title">回复配置</h2>
                </div>
                <div id="replyConfigList" class="empty-state">
                    <div class="empty-state-icon">💭</div>
                    <div class="empty-state-text">请先选择账号</div>
                </div>
            </div>

            <!-- 配置模态框 -->
            <div id="replyConfigModal" class="modal">
                <div class="modal-content">
                    <div class="modal-header">
                        <h3 class="modal-title" id="replyConfigModalTitle">添加配置</h3>
                        <button class="modal-close" onclick="AutoReplyManager.hideConfigModal()">&times;</button>
                    </div>
                    <form id="replyConfigForm">
                        <input type="hidden" id="replyConfigId">
                        <div class="form-group">
                            <label class="form-label">关键词</label>
                            <input type="text" class="form-input" id="keyword" placeholder="请输入关键词">
                        </div>
                        <div class="form-group">
                            <label class="form-label">回复内容</label>
                            <textarea class="form-textarea" id="replyContent" placeholder="请输入回复内容" rows="4"></textarea>
                        </div>
                        <div class="form-group">
                            <label class="form-label">
                                <input type="checkbox" id="enableReply" style="margin-right: 8px;"> 启用自动回复
                            </label>
                        </div>
                        <div class="modal-footer">
                            <button type="button" class="btn btn-outline" onclick="AutoReplyManager.hideConfigModal()">取消</button>
                            <button type="submit" class="btn btn-primary">保存</button>
                        </div>
                    </form>
                </div>
            </div>
        `;
        
        document.getElementById('replyConfigForm').addEventListener('submit', (e) => {
            e.preventDefault();
            AutoReplyManager.saveConfig();
        });
        
        AutoReplyManager.loadAccountSelect();
    }
};
