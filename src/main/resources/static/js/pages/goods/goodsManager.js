// 商品管理业务逻辑
const GoodsManager = {
    currentXianyuAccountId: null,
    currentPage: 1,
    pageSize: 20,
    totalPage: 1,
    totalCount: 0,
    
    // 加载账号选择器
    async loadAccountSelect() {
        try {
            const response = await API.account.list();
            if (response.code === 200 && response.data && response.data.accounts) {
                const select = document.getElementById('goodsAccountSelect');
                if (select) {
                    select.innerHTML = '<option value="">选择账号</option>' +
                        response.data.accounts.map(account => 
                            `<option value="${account.id}">${account.accountNote || account.unb}</option>`
                        ).join('');
                    
                    // 默认选择第一个账号
                    if (response.data.accounts.length > 0) {
                        const firstAccount = response.data.accounts[0];
                        select.value = firstAccount.id;
                        this.loadGoods(firstAccount.id);
                    }
                    
                    select.onchange = () => {
                        const xianyuAccountId = select.value;
                        if (xianyuAccountId) {
                            this.loadGoods(xianyuAccountId);
                        }
                    };
                }
            }
        } catch (error) {
            console.error('加载账号列表失败:', error);
        }
    },
    
    // 加载商品数据
    async loadGoods(xianyuAccountId, status = null, pageNum = 1) {
        if (!xianyuAccountId) {
            console.error('账号ID不能为空');
            return;
        }
        
        this.currentXianyuAccountId = xianyuAccountId;
        this.currentPage = pageNum;
        
        try {
            const goodsListContainer = document.getElementById('goodsList');
            if (goodsListContainer) {
                goodsListContainer.innerHTML = '<div class="loading">加载中...</div>';
            }
            
            const requestData = { 
                xianyuAccountId,
                pageNum: this.currentPage,
                pageSize: this.pageSize
            };
            
            if (status !== null) {
                requestData.status = parseInt(status);
            }
            
            // 使用list方法而不是pagedList方法
            const response = await API.items.list(requestData);
            
            console.log('API响应数据:', response);
            
            if (response.code === 200 && response.data) {
                // 更新分页信息
                this.totalPage = response.data.totalPage || 1;
                this.totalCount = response.data.totalCount || 0;
                this.currentPage = response.data.pageNum || 1;
                
                // 添加调试信息
                console.log('分页信息:', {
                    totalPage: this.totalPage,
                    totalCount: this.totalCount,
                    currentPage: this.currentPage,
                    pageSize: this.pageSize
                });
                
                this.renderGoodsTable(response.data.itemsWithConfig || response.data.items || []);
                this.renderPagination();
            } else {
                throw new Error(response.msg || '获取商品列表失败');
            }
        } catch (error) {
            console.error('加载商品数据失败:', error);
            Utils.showMessage('加载商品数据失败: ' + error.message, 'error');
            
            const goodsListContainer = document.getElementById('goodsList');
            if (goodsListContainer) {
                goodsListContainer.innerHTML = `
                    <div class="empty-state">
                        <div class="empty-state-icon">❌</div>
                        <div class="empty-state-text">加载商品数据失败</div>
                        <div class="empty-state-text" style="font-size: 14px; margin-top: 8px;">${error.message}</div>
                    </div>
                `;
            }
        }
    },
    
    // 渲染商品表格
    renderGoodsTable(itemsWithConfig) {
        const goodsListContainer = document.getElementById('goodsList');
        if (!goodsListContainer) {
            console.error('找不到商品列表容器');
            return;
        }
        
        if (!itemsWithConfig || itemsWithConfig.length === 0) {
            goodsListContainer.innerHTML = `
                <div class="empty-state">
                    <div class="empty-state-icon">📦</div>
                    <div class="empty-state-text">暂无商品数据</div>
                </div>
            `;
            return;
        }
        
        const tableHtml = `
            <div class="goods-table-container">
                <table class="goods-table">
                    <thead>
                        <tr>
                            <th>序号</th>
                            <th>商品ID</th>
                            <th>商品图片</th>
                            <th>商品标题</th>
                            <th>价格</th>
                            <th>状态</th>
                            <th>自动发货</th>
                            <th>自动回复</th>
                            <th>操作</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${itemsWithConfig.map((item, index) => {
                            // 为每个项目添加行索引
                            const itemWithIndex = {...item, rowIndex: (this.currentPage - 1) * this.pageSize + index};
                            return this.renderGoodsRow(itemWithIndex);
                        }).join('')}
                    </tbody>
                </table>
            </div>
        `;
        
        goodsListContainer.innerHTML = tableHtml;
    },
    
    // 渲染分页控件
    renderPagination() {
        const paginationContainer = document.getElementById('goodsPagination');
        if (!paginationContainer) {
            console.error('找不到分页容器');
            return;
        }
        
        // 添加调试信息
        console.log('渲染分页控件:', {
            totalPage: this.totalPage,
            currentPage: this.currentPage
        });
        
        // 检查并确保分页容器有正确的类
        if (!paginationContainer.classList.contains('pagination-container')) {
            paginationContainer.classList.add('pagination-container');
        }
        if (!paginationContainer.classList.contains('card-pagination')) {
            paginationContainer.classList.add('card-pagination');
        }
        
        // 总是显示分页控件，即使只有一页
        let paginationHtml = '<div class="pagination">';
        
        // 上一页按钮
        if (this.currentPage > 1) {
            paginationHtml += `<button class="btn btn-outline btn-small" onclick="GoodsManager.loadGoods(GoodsManager.currentXianyuAccountId, null, ${this.currentPage - 1})">上一页</button>`;
        } else {
            paginationHtml += '<button class="btn btn-outline btn-small" disabled>上一页</button>';
        }
        
        // 页码按钮（只有一页时也显示当前页）
        if (this.totalPage === 1) {
            paginationHtml += `<button class="btn btn-primary btn-small" disabled>1</button>`;
        } else {
            // 多页情况下的页码按钮
            const startPage = Math.max(1, this.currentPage - 2);
            const endPage = Math.min(this.totalPage, this.currentPage + 2);
            
            // 第一页
            if (startPage > 1) {
                paginationHtml += `<button class="btn btn-outline btn-small" onclick="GoodsManager.loadGoods(GoodsManager.currentXianyuAccountId, null, 1)">1</button>`;
                if (startPage > 2) {
                    paginationHtml += '<span class="pagination-ellipsis">...</span>';
                }
            }
            
            // 中间页码
            for (let i = startPage; i <= endPage; i++) {
                if (i === this.currentPage) {
                    paginationHtml += `<button class="btn btn-primary btn-small" disabled>${i}</button>`;
                } else {
                    paginationHtml += `<button class="btn btn-outline btn-small" onclick="GoodsManager.loadGoods(GoodsManager.currentXianyuAccountId, null, ${i})">${i}</button>`;
                }
            }
            
            // 最后一页
            if (endPage < this.totalPage) {
                if (endPage < this.totalPage - 1) {
                    paginationHtml += '<span class="pagination-ellipsis">...</span>';
                }
                paginationHtml += `<button class="btn btn-outline btn-small" onclick="GoodsManager.loadGoods(GoodsManager.currentXianyuAccountId, null, ${this.totalPage})">${this.totalPage}</button>`;
            }
        }
        
        // 下一页按钮
        if (this.currentPage < this.totalPage) {
            paginationHtml += `<button class="btn btn-outline btn-small" onclick="GoodsManager.loadGoods(GoodsManager.currentXianyuAccountId, null, ${this.currentPage + 1})">下一页</button>`;
        } else {
            paginationHtml += '<button class="btn btn-outline btn-small" disabled>下一页</button>';
        }
        
        paginationHtml += `</div>`;
        
        // 显示分页信息
        paginationHtml += `<div class="pagination-info">
            共 ${this.totalCount} 条记录，第 ${this.currentPage} / ${this.totalPage} 页
        </div>`;
        
        paginationContainer.innerHTML = paginationHtml;
        console.log('渲染分页控件完成');
    },

    // 渲染商品行
    renderGoodsRow(itemWithConfig) {
        const item = itemWithConfig.item || itemWithConfig;
        const autoDeliveryOn = itemWithConfig.xianyuAutoDeliveryOn || 0;
        const autoReplyOn = itemWithConfig.xianyuAutoReplyOn || 0;
        const statusInfo = Utils.getItemStatusText(item.status);
        
        // 获取行索引（需要在调用此函数时传入索引参数）
        const rowIndex = itemWithConfig.rowIndex || 0;
        
        // 为每个开关生成唯一的ID，便于后续更新
        const deliverySwitchId = `delivery-switch-${item.xyGoodId}`;
        const replySwitchId = `reply-switch-${item.xyGoodId}`;
        
        return `
            <tr>
                <td>${rowIndex + 1}</td>
                <td>
                    <div class="goods-id-table">${item.xyGoodId || '-'}</div>
                </td>
                <td>
                    <div class="goods-image-table">
                        <img src="${item.coverPic || 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNjAiIGhlaWdodD0iNjAiIHZpZXdCb3g9IjAgMCA2MCA2MCIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4KPHJlY3Qgd2lkdGg9IjYwIiBoZWlnaHQ9IjYwIiBmaWxsPSIjRjBGMEYwIi8+CjxwYXRoIGQ9Ik0yOCAyMEgzMVYzMEgyOFYyMFpNMzQgMjBIMzdWMzBINDRWMzNIMzdWNDBIMzRWNDAgMzRWMzNIMzFWMzBIMzRWMjBaIiBmaWxsPSIjQjNCM0IzIi8+Cjwvc3ZnPgo='}" 
                             alt="${item.title}" 
                             onerror="this.src='data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNjAiIGhlaWdodD0iNjAiIHZpZXdCb3g9IjAgMCA2MCA2MCIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4KPHJlY3Qgd2lkdGg9IjYwIiBoZWlnaHQ9IjYwIiBmaWxsPSIjRjBGMEYwIi8+CjxwYXRoIGQ9Ik0yOCAyMEgzMVYzMEgyOFYyMFpNMzQgMjBIMzdWMzBINDRWMzNIMzdWNDBIMzRWNDAgMzRWMzNIMzFWMzBIMzRWMjBaIiBmaWxsPSIjQjNCM0IzIi8+Cjwvc3ZnPgo='">
                    </div>
                </td>
                <td>
                    <div class="goods-title-table" title="${item.title}">${item.title || '-'}</div>
                </td>
                <td>
                    <div class="goods-price-table">${Utils.formatPrice(item.soldPrice)}</div>
                </td>
                <td>
                    <span class="status-badge ${statusInfo.class}">${statusInfo.text}</span>
                </td>
                <td>
                    <div class="switch-container">
                        <label class="switch-toggle">
                            <input type="checkbox" id="${deliverySwitchId}" ${autoDeliveryOn ? 'checked' : ''} 
                                   onchange="GoodsManager.toggleAutoDelivery('${item.xyGoodId}', this.checked ? 1 : 0, '${deliverySwitchId}')">
                            <span class="switch-slider"></span>
                        </label>
                    </div>
                </td>
                <td>
                    <div class="switch-container">
                        <label class="switch-toggle">
                            <input type="checkbox" id="${replySwitchId}" ${autoReplyOn ? 'checked' : ''} 
                                   onchange="GoodsManager.toggleAutoReply('${item.xyGoodId}', this.checked ? 1 : 0, '${replySwitchId}')">
                            <span class="switch-slider"></span>
                        </label>
                    </div>
                </td>
                <td>
                    <div class="goods-actions">
                        <button class="btn btn-outline btn-small" onclick="GoodsManager.viewDetail('${item.xyGoodId}')">
                            查看详情
                        </button>
                        <button class="btn btn-success btn-small" onclick="GoodsManager.deliverItem('${item.xyGoodId}')">
                            ✓ 发货
                        </button>
                        <button class="btn btn-danger btn-small" onclick="GoodsManager.deleteItem('${item.xyGoodId}', '${item.xianyuAccountId}')">
                            删除
                        </button>
                    </div>
                </td>
            </tr>
        `;
    },
    
    // 刷新商品数据
    async refreshGoods() {
        if (!this.currentXianyuAccountId) {
            Utils.showMessage('请先选择账号', 'warning');
            return;
        }
        
        try {
            const goodsListContainer = document.getElementById('goodsList');
            if (goodsListContainer) {
                goodsListContainer.innerHTML = '<div class="loading">刷新中...</div>';
            }
            
            const response = await API.items.refresh(this.currentXianyuAccountId);
            
            if (response.code === 200) {
                Utils.showMessage('商品数据刷新成功', 'success');
                this.loadGoods(this.currentXianyuAccountId);
            } else {
                throw new Error(response.msg || '刷新商品数据失败');
            }
        } catch (error) {
            console.error('刷新商品数据失败:', error);
            Utils.showMessage('刷新商品数据失败: ' + error.message, 'error');
            this.loadGoods(this.currentXianyuAccountId);
        }
    },
    
    // 按状态筛选
    filterByStatus(status) {
        if (!this.currentXianyuAccountId) {
            Utils.showMessage('请先选择账号', 'warning');
            return;
        }
        this.loadGoods(this.currentXianyuAccountId, status, 1);
    },
    
    // 查看商品详情
    async viewDetail(goodId) {
        const modal = document.getElementById('itemDetailModal');
        const content = document.getElementById('itemDetailContent');
        
        if (!modal || !content) {
            console.error('找不到商品详情弹窗');
            return;
        }
        
        // 显示弹窗和加载状态
        modal.classList.add('show');
        content.innerHTML = '<div class="loading">加载中...</div>';
        
        try {
            // 调用详情接口
            const response = await API.items.detail(goodId);
            
            if (response.code === 200 && response.data) {
                const itemWithConfig = response.data.itemWithConfig || response.data;
                const item = itemWithConfig.item || itemWithConfig;
                const statusInfo = Utils.getItemStatusText(item.status);
                
                // 处理图片列表 - infoPic 是 JSON 字符串
                let images = [];
                try {
                    if (item.infoPic) {
                        const infoPicArray = JSON.parse(item.infoPic);
                        images = infoPicArray.map(pic => pic.url);
                    }
                } catch (e) {
                    console.error('解析图片列表失败:', e);
                }
                
                // 如果没有图片，使用封面图
                if (images.length === 0 && item.coverPic) {
                    images = [item.coverPic];
                }
                
                const mainImage = images.length > 0 ? images[0] : '';
                
                // 为详情页开关生成唯一的ID
                const detailDeliverySwitchId = `detail-delivery-switch-${item.xyGoodId}`;
                const detailReplySwitchId = `detail-reply-switch-${item.xyGoodId}`;
                
                content.innerHTML = `
                    <div class="goods-detail-layout">
                        <div class="goods-detail-left">
                            <div class="goods-detail-main-image">
                                <img id="mainDetailImage" src="${mainImage || ''}" alt="${item.title}" 
                                     onerror="this.src='data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNDAwIiBoZWlnaHQ9IjQwMCIgdmlld0JveD0iMCAwIDQwMCA0MDAiIGZpbGw9Im5vbmUiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyI+CjxyZWN0IHdpZHRoPSI0MDAiIGhlaWdodD0iNDAwIiBmaWxsPSIjRjBGMEYwIi8+Cjwvc3ZnPgo='">
                            </div>
                            ${images.length > 1 ? `
                            <div class="goods-detail-thumbnails">
                                ${images.map((img, index) => `
                                    <img src="${img}" alt="图片${index + 1}" 
                                         onclick="document.getElementById('mainDetailImage').src='${img}'"
                                         onerror="this.style.display='none'">
                                `).join('')}
                            </div>
                            ` : ''}
                        </div>
                        <div class="goods-detail-right">
                            <div class="detail-title-section">
                                <h3 class="detail-title">${item.title || '-'}</h3>
                                <span class="detail-id">(${item.xyGoodId || '-'})</span>
                            </div>
                            <div class="detail-price-section">
                                <span class="detail-price">${Utils.formatPrice(item.soldPrice)}</span>
                                <span class="status-badge ${statusInfo.class}">${statusInfo.text}</span>
                            </div>
                            ${item.detailInfo ? `
                            <div class="detail-description">
                                <p>${item.detailInfo.replace(/\n/g, '<br>')}</p>
                            </div>
                            ` : ''}
                            <div class="detail-info-section">
                                <div class="detail-switch-row">
                                    <span class="detail-info-label">自动发货</span>
                                    <div class="switch-container">
                                        <label class="switch-toggle">
                                            <input type="checkbox" id="${detailDeliverySwitchId}" ${itemWithConfig.xianyuAutoDeliveryOn ? 'checked' : ''} 
                                                   onchange="GoodsManager.toggleAutoDelivery('${item.xyGoodId}', this.checked ? 1 : 0, '${detailDeliverySwitchId}')">
                                            <span class="switch-slider"></span>
                                        </label>
                                    </div>
                                </div>
                                <div class="detail-switch-row">
                                    <span class="detail-info-label">自动回复</span>
                                    <div class="switch-container">
                                        <label class="switch-toggle">
                                            <input type="checkbox" id="${detailReplySwitchId}" ${itemWithConfig.xianyuAutoReplyOn ? 'checked' : ''} 
                                                   onchange="GoodsManager.toggleAutoReply('${item.xyGoodId}', this.checked ? 1 : 0, '${detailReplySwitchId}')">
                                            <span class="switch-slider"></span>
                                        </label>
                                    </div>
                                </div>
                                ${item.updatedTime ? `
                                <div class="detail-info-row">
                                    <span class="detail-info-label">最后同步时间</span>
                                    <span class="detail-info-value">${item.updatedTime}</span>
                                </div>
                                ` : ''}
                            </div>
                        </div>
                    </div>
                `;
            } else {
                throw new Error(response.msg || '获取商品详情失败');
            }
        } catch (error) {
            console.error('查看商品详情失败:', error);
            content.innerHTML = `<div class="empty-state"><div class="empty-state-text">加载失败: ${error.message}</div></div>`;
        }
    },
    
    // 发货操作
    deliverItem(goodId) {
        console.log('发货操作:', goodId);
        Utils.showMessage('发货功能待实现，商品ID: ' + goodId, 'info');
    },
    
    // 图片预览
    previewImage(imageUrl) {
        if (!imageUrl) return;
        const modal = document.getElementById('imagePreviewModal');
        const img = document.getElementById('previewImage');
        if (modal && img) {
            img.src = imageUrl;
            modal.classList.add('show');
        }
    },
    
    // 批量配置
    showBatchConfig() {
        Utils.showMessage('批量配置功能待实现', 'info');
    },
    
    // 导出数据
    exportGoods() {
        Utils.showMessage('导出数据功能待实现', 'info');
    },
    
    // 切换自动发货状态
    async toggleAutoDelivery(goodId, newStatus, switchId) {
        // 调试信息
        console.log('API对象:', API);
        console.log('API.items对象:', API.items);
        console.log('updateAutoDeliveryStatus函数是否存在:', typeof API.items.updateAutoDeliveryStatus);
        
        if (typeof API.items.updateAutoDeliveryStatus !== 'function') {
            Utils.showMessage('自动发货接口未定义，请刷新页面重试', 'error');
            // 恢复开关状态
            const checkbox = document.getElementById(switchId);
            if (checkbox) {
                checkbox.checked = !newStatus;
            }
            return;
        }
        
        if (!this.currentXianyuAccountId) {
            Utils.showMessage('请先选择账号', 'warning');
            // 恢复开关状态
            const checkbox = document.getElementById(switchId);
            if (checkbox) {
                checkbox.checked = !newStatus;
            }
            return;
        }
        
        try {
            const requestData = {
                xianyuAccountId: this.currentXianyuAccountId,
                xyGoodsId: goodId,
                xianyuAutoDeliveryOn: newStatus
            };
            
            const response = await API.items.updateAutoDeliveryStatus(requestData);
            
            if (response.code === 200 && response.data && response.data.success) {
                Utils.showMessage(`自动发货${newStatus ? '开启' : '关闭'}成功`, 'success');
                // 只更新当前开关状态，不刷新整个列表
                // 开关状态已经通过用户操作改变，无需额外处理
            } else {
                throw new Error(response.msg || response.data?.message || '切换自动发货状态失败');
            }
        } catch (error) {
            console.error('切换自动发货状态失败:', error);
            Utils.showMessage('切换自动发货状态失败: ' + error.message, 'error');
            // 恢复开关状态
            const checkbox = document.getElementById(switchId);
            if (checkbox) {
                checkbox.checked = !newStatus;
            }
        }
    },
    
    // 切换自动回复状态
    async toggleAutoReply(goodId, newStatus, switchId) {
        // 调试信息
        console.log('API对象:', API);
        console.log('API.items对象:', API.items);
        console.log('updateAutoReplyStatus函数是否存在:', typeof API.items.updateAutoReplyStatus);
        
        if (typeof API.items.updateAutoReplyStatus !== 'function') {
            Utils.showMessage('自动回复接口未定义，请刷新页面重试', 'error');
            // 恢复开关状态
            const checkbox = document.getElementById(switchId);
            if (checkbox) {
                checkbox.checked = !newStatus;
            }
            return;
        }
        
        if (!this.currentXianyuAccountId) {
            Utils.showMessage('请先选择账号', 'warning');
            // 恢复开关状态
            const checkbox = document.getElementById(switchId);
            if (checkbox) {
                checkbox.checked = !newStatus;
            }
            return;
        }
        
        try {
            const requestData = {
                xianyuAccountId: this.currentXianyuAccountId,
                xyGoodsId: goodId,
                xianyuAutoReplyOn: newStatus
            };
            
            const response = await API.items.updateAutoReplyStatus(requestData);
            
            if (response.code === 200 && response.data && response.data.success) {
                Utils.showMessage(`自动回复${newStatus ? '开启' : '关闭'}成功`, 'success');
                // 只更新当前开关状态，不刷新整个列表
                // 开关状态已经通过用户操作改变，无需额外处理
            } else {
                throw new Error(response.msg || response.data?.message || '切换自动回复状态失败');
            }
        } catch (error) {
            console.error('切换自动回复状态失败:', error);
            Utils.showMessage('切换自动回复状态失败: ' + error.message, 'error');
            // 恢复开关状态
            const checkbox = document.getElementById(switchId);
            if (checkbox) {
                checkbox.checked = !newStatus;
            }
        }
    },
    
    // 删除商品
    deleteItem: function(goodId, accountId) {
        // 确认删除
        if (!confirm('确定要删除这个商品吗？')) {
            return;
        }
        
        // 使用async函数处理异步操作
        (async () => {
            try {
                const requestData = {
                    xyGoodId: goodId,
                    xianyuAccountId: parseInt(accountId)
                };
                
                const response = await API.items.delete(requestData);
                
                if (response.code === 200) {
                    Utils.showMessage('商品删除成功', 'success');
                    // 重新加载商品列表
                    if (this.currentXianyuAccountId) {
                        this.loadGoods(this.currentXianyuAccountId);
                    }
                } else {
                    Utils.showMessage('商品删除失败: ' + response.msg, 'error');
                }
            } catch (error) {
                console.error('删除商品失败:', error);
                Utils.showMessage('删除商品失败: ' + error.message, 'error');
            }
        })();
    }
};