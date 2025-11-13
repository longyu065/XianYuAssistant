// 页面管理器
const PageManager = {
    currentPage: 'dashboard',

    // 初始化
    init() {
        this.bindEvents();
        this.loadPage('dashboard');
    },

    // 绑定事件
    bindEvents() {
        document.querySelectorAll('.nav-item').forEach(item => {
            item.addEventListener('click', (e) => {
                const page = e.currentTarget.dataset.page;
                if (page) {
                    this.loadPage(page);
                    document.querySelectorAll('.nav-item').forEach(i => i.classList.remove('active'));
                    e.currentTarget.classList.add('active');
                }
            });
        });
    },

    // 加载页面
    loadPage(page) {
        this.currentPage = page;
        const content = document.getElementById('mainContent');
        
        // 根据页面类型加载对应模块
        switch(page) {
            case 'dashboard':
                if (typeof DashboardPage !== 'undefined') {
                    DashboardPage.render(content);
                } else {
                    content.innerHTML = '<div class="empty-state"><div class="empty-state-icon">⚠️</div><div class="empty-state-text">页面模块未加载</div></div>';
                }
                break;
            case 'accounts':
                if (typeof AccountsPage !== 'undefined') {
                    AccountsPage.render(content);
                } else {
                    content.innerHTML = '<div class="empty-state"><div class="empty-state-icon">⚠️</div><div class="empty-state-text">页面模块未加载</div></div>';
                }
                break;
            case 'goods':
                if (typeof GoodsPage !== 'undefined') {
                    GoodsPage.render(content);
                } else {
                    content.innerHTML = '<div class="empty-state"><div class="empty-state-icon">⚠️</div><div class="empty-state-text">页面模块未加载</div></div>';
                }
                break;
            case 'messages':
                if (typeof MessagesPage !== 'undefined') {
                    MessagesPage.render(content);
                } else {
                    content.innerHTML = '<div class="empty-state"><div class="empty-state-icon">⚠️</div><div class="empty-state-text">页面模块未加载</div></div>';
                }
                break;
            case 'auto-delivery':
                if (typeof AutoDeliveryPage !== 'undefined') {
                    AutoDeliveryPage.render(content);
                } else {
                    content.innerHTML = '<div class="empty-state"><div class="empty-state-icon">⚠️</div><div class="empty-state-text">页面模块未加载</div></div>';
                }
                break;
            case 'auto-reply':
                if (typeof AutoReplyPage !== 'undefined') {
                    AutoReplyPage.render(content);
                } else {
                    content.innerHTML = '<div class="empty-state"><div class="empty-state-icon">⚠️</div><div class="empty-state-text">页面模块未加载</div></div>';
                }
                break;
            case 'records':
                if (typeof RecordsPage !== 'undefined') {
                    RecordsPage.render(content);
                } else {
                    content.innerHTML = '<div class="empty-state"><div class="empty-state-icon">⚠️</div><div class="empty-state-text">页面模块未加载</div></div>';
                }
                break;
            case 'qrlogin':
                if (typeof QRLoginPage !== 'undefined') {
                    QRLoginPage.render(content);
                } else {
                    content.innerHTML = '<div class="empty-state"><div class="empty-state-icon">⚠️</div><div class="empty-state-text">页面模块未加载</div></div>';
                }
                break;
            default:
                content.innerHTML = '<div class="empty-state"><div class="empty-state-icon">🚧</div><div class="empty-state-text">页面开发中...</div></div>';
        }
    }
};