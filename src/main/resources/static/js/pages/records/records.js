// 操作记录页面模块
const RecordsPage = {
    render(content) {
        content.innerHTML = `
            <div class="header">
                <h1 class="welcome">操作记录</h1>
                <div class="header-actions">
                    <select class="form-select" id="recordAccountSelect" onchange="RecordManager.loadRecords()">
                        <option value="">选择账号</option>
                    </select>
                    <button class="btn btn-outline" onclick="RecordManager.loadRecords()">刷新记录</button>
                </div>
            </div>

            <div class="card">
                <div class="card-header">
                    <h2 class="card-title">记录列表</h2>
                </div>
                <div id="recordList" class="empty-state">
                    <div class="empty-state-icon">📝</div>
                    <div class="empty-state-text">请先选择账号</div>
                </div>
            </div>
        `;
        
        RecordManager.loadAccountSelect();
    }
};
