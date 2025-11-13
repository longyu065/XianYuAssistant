# Pages 文件夹结构说明

## 目录结构

```
js/
├── api.js              # 通用API接口和工具函数
├── pageManager.js      # 页面管理器（路由）
└── pages/              # 页面模块文件夹
    ├── accounts/       # 账号管理模块
    │   ├── accounts.js         # 页面渲染
    │   └── accountManager.js   # 业务逻辑
    ├── goods/          # 商品管理模块
    │   ├── goods.js            # 页面渲染
    │   └── goodsManager.js     # 业务逻辑
    ├── messages/       # 消息管理模块
    │   ├── messages.js         # 页面渲染
    │   └── messageManager.js   # 业务逻辑
    ├── autoDelivery/   # 自动发货模块
    │   ├── autoDelivery.js         # 页面渲染
    │   └── autoDeliveryManager.js  # 业务逻辑
    ├── autoReply/      # 自动回复模块
    │   ├── autoReply.js            # 页面渲染
    │   └── autoReplyManager.js     # 业务逻辑
    ├── records/        # 操作记录模块
    │   ├── records.js          # 页面渲染
    │   └── recordManager.js    # 业务逻辑
    ├── qrlogin/        # 二维码登录模块
    │   ├── qrlogin.js          # 页面渲染
    │   └── qrloginManager.js   # 业务逻辑
    └── dashboard/      # 仪表板模块
        └── dashboard.js        # 页面渲染和逻辑
```

## 文件说明

### 通用文件（js 外层）
- **api.js**: 包含所有API接口封装、通用请求方法和工具函数（Utils）
- **pageManager.js**: 页面路由管理器，负责页面切换和导航

### 模块文件（pages 子文件夹）
每个侧边栏模块都有独立的文件夹，包含：
- **[模块名].js**: 页面渲染逻辑，定义页面HTML结构
- **[模块名]Manager.js**: 业务逻辑，处理数据加载、表单提交等操作

## 命名规范

- 页面对象：`[模块名]Page`（如 `AccountsPage`、`GoodsPage`）
- 管理器对象：`[模块名]Manager`（如 `AccountManager`、`GoodsManager`）
- 文件名：使用驼峰命名法（如 `accountManager.js`）

## 优势

1. **模块化**: 每个功能模块独立管理，便于维护
2. **清晰的职责分离**: 页面渲染和业务逻辑分离
3. **易于扩展**: 添加新模块只需创建对应文件夹
4. **代码复用**: 通用功能集中在外层文件中
