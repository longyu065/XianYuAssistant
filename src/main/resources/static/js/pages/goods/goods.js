// 商品管理页面模块
const GoodsPage = {
    render(content) {
        content.innerHTML = `
            <div class="header">
                <h1 class="welcome">商品管理</h1>
                <div class="header-actions">
                    <select class="form-select" id="goodsAccountSelect">
                        <option value="">选择账号</option>
                    </select>
                    <select class="form-select" id="goodsStatusFilter" onchange="GoodsManager.filterByStatus(this.value)">
                        <option value="">全部状态</option>
                        <option value="0">在售商品</option>
                        <option value="1">已下架</option>
                        <option value="2">已售出</option>
                    </select>
                    <button class="btn btn-outline" onclick="GoodsManager.loadGoods(GoodsManager.currentXianyuAccountId)">刷新列表</button>
                    <button class="btn btn-primary" onclick="GoodsManager.refreshGoods()">同步商品</button>
                </div>
            </div>

            <div class="card">
                <div class="card-header">
                    <h2 class="card-title">商品列表</h2>
                    <div style="display: flex; gap: 8px;">
                        <button class="btn btn-outline btn-small" onclick="GoodsManager.showBatchConfig()">批量配置</button>
                        <button class="btn btn-outline btn-small" onclick="GoodsManager.exportGoods()">导出数据</button>
                    </div>
                </div>
                <div id="goodsList" class="empty-state">
                    <div class="empty-state-icon">📦</div>
                    <div class="empty-state-text">请先选择账号</div>
                </div>
            </div>

            <!-- 图片预览模态框 -->
            <div id="imagePreviewModal" class="modal" onclick="this.classList.remove('show')">
                <div class="modal-content" style="max-width: 90vw; max-height: 90vh; padding: 0; background: transparent; border: none;">
                    <img id="previewImage" style="max-width: 100%; max-height: 90vh; border-radius: 8px; box-shadow: 0 8px 32px rgba(0,0,0,0.3);">
                </div>
            </div>
        `;
        GoodsManager.loadAccountSelect();
    }
};
