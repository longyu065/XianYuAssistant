// API基础配置
const API_BASE = '/api';

// 通用请求方法
async function request(url, options = {}) {
    try {
        const response = await fetch(url, {
            headers: {
                'Content-Type': 'application/json',
                ...options.headers
            },
            ...options
        });
        const data = await response.json();
        return data;
    } catch (error) {
        console.error('请求失败:', error);
        return { code: -1, message: '网络请求失败' };
    }
}

// API接口封装
const API = {
    // 账号相关
    account: {
        list: () => request(`${API_BASE}/account/list`, { method: 'POST', body: JSON.stringify({}) }),
        add: (data) => request(`${API_BASE}/account/add`, { method: 'POST', body: JSON.stringify(data) }),
        update: (data) => request(`${API_BASE}/account/update`, { method: 'POST', body: JSON.stringify(data) }),
        delete: (data) => request(`${API_BASE}/account/delete`, { method: 'POST', body: JSON.stringify(data) })
    },
    
    // 商品相关
    items: {
        refresh: (xianyuAccountId) => request(`${API_BASE}/items/refresh`, {
            method: 'POST',
            body: JSON.stringify({ xianyuAccountId })
        }),
        list: (data) => request(`${API_BASE}/items/list`, {
            method: 'POST',
            body: JSON.stringify(data)
        }),
        detail: (xyGoodId, cookieId = null) => request(`${API_BASE}/items/detail`, {
            method: 'POST',
            body: JSON.stringify({ xyGoodId, cookieId })
        })
    },
    
    // WebSocket相关
    websocket: {
        start: (xianyuAccountId, accessToken = null) => request(`${API_BASE}/websocket/start`, {
            method: 'POST',
            body: JSON.stringify({ xianyuAccountId, accessToken })
        }),
        stop: (xianyuAccountId) => request(`${API_BASE}/websocket/stop`, {
            method: 'POST',
            body: JSON.stringify({ xianyuAccountId })
        }),
        status: (xianyuAccountId) => request(`${API_BASE}/websocket/status`, {
            method: 'POST',
            body: JSON.stringify({ xianyuAccountId })
        }),
        sendMessage: (data) => request(`${API_BASE}/websocket/sendMessage`, {
            method: 'POST',
            body: JSON.stringify(data)
        }),
        clearCaptchaWait: (xianyuAccountId) => request(`${API_BASE}/websocket/clearCaptchaWait`, {
            method: 'POST',
            body: JSON.stringify({ xianyuAccountId })
        })
    },
    
    // 订单相关
    order: {
        confirmShipment: (xianyuAccountId, orderId) => request(`${API_BASE}/order/confirmShipment`, {
            method: 'POST',
            body: JSON.stringify({ xianyuAccountId, orderId })
        })
    },
    
    // 二维码登录
    qrlogin: {
        generate: () => request(`${API_BASE}/qrlogin/generate`, { method: 'POST' }),
        status: (sessionId) => request(`${API_BASE}/qrlogin/status/${sessionId}`, { method: 'POST' }),
        cookies: (sessionId) => request(`${API_BASE}/qrlogin/cookies/${sessionId}`, { method: 'POST' }),
        cleanup: () => request(`${API_BASE}/qrlogin/cleanup`, { method: 'POST' })
    }
};

// 工具函数
const Utils = {
    // 显示提示消息
    showMessage: (message, type = 'info') => {
        const colors = {
            success: '#34c759',
            error: '#ff3b30',
            warning: '#ff9500',
            info: '#007aff'
        };
        
        const div = document.createElement('div');
        div.style.cssText = `
            position: fixed;
            top: 20px;
            right: 20px;
            padding: 16px 24px;
            background: ${colors[type] || colors.info};
            color: white;
            border-radius: 8px;
            box-shadow: 0 4px 12px rgba(0,0,0,0.15);
            z-index: 10000;
            animation: slideIn 0.3s ease;
        `;
        div.textContent = message;
        document.body.appendChild(div);
        
        setTimeout(() => {
            div.style.animation = 'slideOut 0.3s ease';
            setTimeout(() => div.remove(), 300);
        }, 3000);
    },
    
    // 格式化时间
    formatTime: (timestamp) => {
        if (!timestamp) return '-';
        const date = new Date(timestamp);
        return date.toLocaleString('zh-CN');
    },
    
    // 格式化价格
    formatPrice: (price) => {
        if (!price) return '¥0';
        return `¥${parseFloat(price).toFixed(2)}`;
    },
    
    // 获取商品状态文本
    getItemStatusText: (status) => {
        const statusMap = {
            0: { text: '在售', class: 'status-online' },
            1: { text: '已下架', class: 'status-inactive' },
            2: { text: '已售出', class: 'status-offline' }
        };
        return statusMap[status] || { text: '未知', class: 'status-inactive' };
    }
};

// 添加动画样式
const style = document.createElement('style');
style.textContent = `
    @keyframes slideIn {
        from { transform: translateX(100%); opacity: 0; }
        to { transform: translateX(0); opacity: 1; }
    }
    @keyframes slideOut {
        from { transform: translateX(0); opacity: 1; }
        to { transform: translateX(100%); opacity: 0; }
    }
`;
document.head.appendChild(style);
