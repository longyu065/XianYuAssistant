// 自动回复页面模块
const AutoReplyPage = {
    render(content) {
        content.innerHTML = `
            <div class="header" id="reply-header-id-1">
                <h1 class="welcome" id="reply-welcome-id-2">自动回复</h1>
                <div class="header-actions" id="reply-header-actions-id-3">
                    <select class="form-select" id="replyAccountSelect" onchange="AutoReplyManager.loadConfigs()">
                        <option value="" id="reply-option-default-id-4">选择账号</option>
                    </select>
                    <button class="btn btn-primary" id="reply-add-btn-id-5" onclick="AutoReplyManager.showAddConfigModal()">+ 添加配置</button>
                </div>
            </div>

            <div class="card" id="reply-card-id-6">
                <div class="card-header" id="reply-card-header-id-7">
                    <h2 class="card-title" id="reply-card-title-id-8">回复配置</h2>
                </div>
                <div id="replyConfigList" class="empty-state" id="reply-config-list-id-9">
                    <div class="empty-state-icon" id="reply-empty-icon-id-10">💭</div>
                    <div class="empty-state-text" id="reply-empty-text-id-11">请先选择账号</div>
                </div>
            </div>

            <!-- 配置模态框 -->
            <div id="replyConfigModal" class="modal">
                <div class="modal-content" id="reply-modal-content-id-12">
                    <div class="modal-header" id="reply-modal-header-id-13">
                        <h3 class="modal-title" id="replyConfigModalTitle">添加配置</h3>
                        <button class="modal-close" id="reply-modal-close-id-14" onclick="AutoReplyManager.hideConfigModal()">&times;</button>
                    </div>
                    <form id="replyConfigForm">
                        <input type="hidden" id="replyConfigId">
                        <div class="form-group" id="reply-keyword-group-id-15">
                            <label class="form-label" id="reply-keyword-label-id-16">关键词</label>
                            <input type="text" class="form-input" id="keyword" placeholder="请输入关键词">
                        </div>
                        <div class="form-group" id="reply-content-group-id-17">
                            <label class="form-label" id="reply-content-label-id-18">回复内容</label>
                            <textarea class="form-textarea" id="replyContent" placeholder="请输入回复内容" rows="4"></textarea>
                        </div>
                        <div class="form-group" id="reply-enable-group-id-19">
                            <label class="form-label" id="reply-enable-label-id-20">
                                <input type="checkbox" id="enableReply" style="margin-right: 8px;"> 启用自动回复
                            </label>
                        </div>
                        <div class="modal-footer" id="reply-modal-footer-id-21">
                            <button type="button" class="btn btn-outline" id="reply-cancel-btn-id-22" onclick="AutoReplyManager.hideConfigModal()">取消</button>
                            <button type="submit" class="btn btn-primary" id="reply-save-btn-id-23">保存</button>
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
