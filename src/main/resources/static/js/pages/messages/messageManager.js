// 消息管理业务逻辑
const MessageManager = {
    currentXianyuAccountId: null,
    currentPage: 1,
    pageSize: 20,
    totalPage: 1,
    totalCount: 0,
    currentXyGoodsId: null,
    
    // 加载账号选择器
    async loadAccountSelect() {
        try {
            const response = await API.account.list();
            if (response.code === 200 && response.data && response.data.accounts) {
                const select = document.getElementById('messageAccountSelect');
                if (select) {
                    select.innerHTML = '<option value="">选择账号</option>' +
                        response.data.accounts.map(account => 
                            `<option value="${account.id}">${account.accountNote || account.unb || '未命名账号'}</option>`
                        ).join('');
                }
            }
        } catch (error) {
            console.error('加载账号列表失败:', error);
        }
    },
    
    // 加载消息列表
    async loadMessages(pageNum = 1, xyGoodsId = null) {
        const select = document.getElementById('messageAccountSelect');
        const xianyuAccountId = select ? select.value : null;
        
        if (!xianyuAccountId) {
            Utils.showMessage('请先选择账号', 'warning');
            return;
        }
        
        this.currentXianyuAccountId = xianyuAccountId;
        this.currentPage = pageNum;
        this.currentXyGoodsId = xyGoodsId;
        
        const container = document.getElementById('messageList');
        if (container) {
            container.innerHTML = '<div class="loading">加载中...</div>';
        }
        
        try {
            const requestData = {
                xianyuAccountId: parseInt(xianyuAccountId),
                pageNum: this.currentPage,
                pageSize: this.pageSize
            };
            
            if (xyGoodsId) {
                requestData.xyGoodsId = xyGoodsId;
            }
            
            const response = await API.msg.list(requestData);
            
            console.log('消息列表API响应:', response);
            
            if (response.code === 200 && response.data) {
                // 更新分页信息
                this.totalPage = response.data.totalPage || 1;
                this.totalCount = response.data.totalCount || 0;
                this.currentPage = response.data.pageNum || 1;
                
                // 渲染消息列表
                this.renderMessageList(response.data.list || []);
                this.renderPagination();
            } else {
                throw new Error(response.msg || '获取消息列表失败');
            }
        } catch (error) {
            console.error('加载消息列表失败:', error);
            Utils.showMessage('加载消息列表失败: ' + error.message, 'error');
            
            if (container) {
                container.classList.remove('message-list-scroll');
                container.innerHTML = `
                    <div class="empty-state">
                        <div class="empty-state-icon">❌</div>
                        <div class="empty-state-text">加载消息列表失败</div>
                    </div>
                `;
            }
        }
    },
    
    // 渲染消息列表
    renderMessageList(messages) {
        const container = document.getElementById('messageList');
        if (!container) return;
        
        // 移除之前的滚动样式类
        container.classList.remove('message-list-scroll');
        
        if (!messages || messages.length === 0) {
            container.innerHTML = `
                <div class="empty-state">
                    <div class="empty-state-icon">💬</div>
                    <div class="empty-state-text">暂无消息数据</div>
                </div>
            `;
            return;
        }
        
        const tableHtml = `
            <table class="message-table">
                <thead>
                    <tr>
                        <th>序号</th>
                        <th>消息ID</th>
                        <th>消息类型</th>
                        <th>发送者</th>
                        <th>消息内容</th>
                        <th>商品ID</th>
                        <th>时间</th>
                        <th>操作</th>
                    </tr>
                </thead>
                <tbody>
                    ${messages.map((msg, index) => {
                        const rowIndex = (this.currentPage - 1) * this.pageSize + index;
                        return this.renderMessageRow(msg, rowIndex);
                    }).join('')}
                </tbody>
            </table>
        `;
        
        container.innerHTML = tableHtml;
        // 添加滚动样式类
        container.classList.add('message-list-scroll');
    },
    
    // 渲染消息表格行
    renderMessageRow(msg, rowIndex) {
        const contentTypeText = this.getContentTypeText(msg.contentType);
        const timeText = this.formatMessageTime(msg.messageTime);
        const senderName = msg.senderUserName || '未知用户';
        const content = msg.msgContent || '无内容';
        const goodsId = msg.xyGoodsId || '-';
        const messageId = msg.id || '-';
        
        // 截断过长的内容
        const truncatedContent = content.length > 50 ? content.substring(0, 50) + '...' : content;
        
        return `
            <tr>
                <td>${rowIndex + 1}</td>
                <td>
                    <div class="message-id-cell">${messageId}</div>
                </td>
                <td>
                    <span class="message-type-badge">${this.escapeHtml(contentTypeText)}</span>
                </td>
                <td>
                    <div class="message-sender-cell" title="${this.escapeHtml(senderName)}">${this.escapeHtml(senderName)}</div>
                </td>
                <td>
                    <div class="message-content-cell" title="${this.escapeHtml(content)}">${this.escapeHtml(truncatedContent)}</div>
                </td>
                <td>
                    <div class="message-goods-id-cell">${goodsId}</div>
                </td>
                <td>
                    <div class="message-time-cell">${timeText}</div>
                </td>
                <td>
                    <div class="message-actions">
                        ${msg.reminderUrl ? `<a href="${msg.reminderUrl}" target="_blank" class="btn btn-outline btn-small">查看链接</a>` : '-'}
                    </div>
                </td>
            </tr>
        `;
    },
    
    // 获取消息类型文本
    getContentTypeText(contentType) {
        if (!contentType) return '其他';
        
        const typeMap = {
            1: '用户消息',
            2: '图片',
            32: '已付款待发货'
        };
        
        return typeMap[contentType] || `其他(${contentType})`;
    },
    
    // 格式化消息时间
    formatMessageTime(timestamp) {
        if (!timestamp) return '-';
        
        const date = new Date(timestamp);
        const now = new Date();
        const diff = now - date;
        
        // 小于1分钟
        if (diff < 60000) {
            return '刚刚';
        }
        
        // 小于1小时
        if (diff < 3600000) {
            return `${Math.floor(diff / 60000)}分钟前`;
        }
        
        // 小于24小时
        if (diff < 86400000) {
            return `${Math.floor(diff / 3600000)}小时前`;
        }
        
        // 超过24小时，显示具体日期时间
        return date.toLocaleString('zh-CN', {
            year: 'numeric',
            month: '2-digit',
            day: '2-digit',
            hour: '2-digit',
            minute: '2-digit'
        });
    },
    
    // HTML转义
    escapeHtml(text) {
        if (!text) return '';
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    },
    
    // 渲染分页控件
    renderPagination() {
        // 尝试查找分页容器
        let paginationContainer = document.getElementById('messagePagination');
        
        if (!paginationContainer) {
            console.warn('找不到分页容器 messagePagination，尝试查找父容器...');
            
            // 尝试通过父容器查找
            const messageList = document.getElementById('messageList');
            if (messageList && messageList.parentElement) {
                const card = messageList.closest('.card');
                if (card) {
                    // 如果容器不存在，创建一个
                    paginationContainer = document.createElement('div');
                    paginationContainer.id = 'messagePagination';
                    paginationContainer.className = 'pagination-container card-pagination';
                    card.appendChild(paginationContainer);
                    console.log('已创建分页容器');
                }
            }
            
            // 如果还是找不到，延迟重试
            if (!paginationContainer) {
                console.warn('仍然找不到分页容器，延迟100ms后重试...');
                setTimeout(() => {
                    paginationContainer = document.getElementById('messagePagination');
                    if (paginationContainer) {
                        this.renderPaginationContent(paginationContainer);
                    } else {
                        console.error('延迟后仍然找不到分页容器 messagePagination，请检查HTML结构');
                    }
                }, 100);
                return;
            }
        }
        
        this.renderPaginationContent(paginationContainer);
    },
    
    // 渲染分页内容
    renderPaginationContent(paginationContainer) {
        if (!paginationContainer) {
            console.error('renderPaginationContent: paginationContainer 为空');
            return;
        }
        
        // 确保分页容器可见
        paginationContainer.style.display = 'flex';
        paginationContainer.style.visibility = 'visible';
        paginationContainer.style.opacity = '1';
        
        // 总是显示分页控件（即使只有一页也显示，方便用户了解数据情况）
        // 生成分页按钮的onclick参数
        const goodsIdParam = this.currentXyGoodsId ? `'${this.currentXyGoodsId}'` : 'null';
        
        let paginationHtml = '<div class="pagination">';
        paginationHtml += '<div class="pagination-buttons">';
        
        // 上一页按钮
        if (this.currentPage > 1) {
            paginationHtml += `<button class="btn btn-outline btn-small" onclick="MessageManager.loadMessages(${this.currentPage - 1}, ${goodsIdParam})">上一页</button>`;
        } else {
            paginationHtml += '<button class="btn btn-outline btn-small" disabled>上一页</button>';
        }
        
        // 页码按钮
        if (this.totalPage === 1) {
            // 只有一页时也显示当前页
            paginationHtml += `<button class="btn btn-primary btn-small" disabled>1</button>`;
        } else {
            const maxVisiblePages = 5;
            let startPage = Math.max(1, this.currentPage - Math.floor(maxVisiblePages / 2));
            let endPage = Math.min(this.totalPage, startPage + maxVisiblePages - 1);
            
            if (endPage - startPage < maxVisiblePages - 1) {
                startPage = Math.max(1, endPage - maxVisiblePages + 1);
            }
            
            // 第一页
            if (startPage > 1) {
                paginationHtml += `<button class="btn btn-outline btn-small" onclick="MessageManager.loadMessages(1, ${goodsIdParam})">1</button>`;
                if (startPage > 2) {
                    paginationHtml += '<span class="pagination-ellipsis">...</span>';
                }
            }
            
            // 中间页码
            for (let i = startPage; i <= endPage; i++) {
                if (i === this.currentPage) {
                    paginationHtml += `<button class="btn btn-primary btn-small" disabled>${i}</button>`;
                } else {
                    paginationHtml += `<button class="btn btn-outline btn-small" onclick="MessageManager.loadMessages(${i}, ${goodsIdParam})">${i}</button>`;
                }
            }
            
            // 最后一页
            if (endPage < this.totalPage) {
                if (endPage < this.totalPage - 1) {
                    paginationHtml += '<span class="pagination-ellipsis">...</span>';
                }
                paginationHtml += `<button class="btn btn-outline btn-small" onclick="MessageManager.loadMessages(${this.totalPage}, ${goodsIdParam})">${this.totalPage}</button>`;
            }
        }
        
        // 下一页按钮
        if (this.currentPage < this.totalPage) {
            paginationHtml += `<button class="btn btn-outline btn-small" onclick="MessageManager.loadMessages(${this.currentPage + 1}, ${goodsIdParam})">下一页</button>`;
        } else {
            paginationHtml += '<button class="btn btn-outline btn-small" disabled>下一页</button>';
        }
        
        paginationHtml += '</div>'; // 关闭 pagination-buttons
        
        // 显示分页信息
        paginationHtml += `<div class="pagination-info">
            共 ${this.totalCount} 条消息，第 ${this.currentPage} / ${this.totalPage} 页
        </div>`;
        
        paginationHtml += `</div>`; // 关闭 pagination
        
        paginationContainer.innerHTML = paginationHtml;
        
        // 添加调试日志
        console.log('分页控件已渲染:', {
            totalCount: this.totalCount,
            currentPage: this.currentPage,
            totalPage: this.totalPage,
            containerExists: !!paginationContainer,
            htmlLength: paginationHtml.length
        });
    }
