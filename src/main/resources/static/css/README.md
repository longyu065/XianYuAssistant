# CSS 模块化结构说明

## 目录结构

```
css/
├── common/                 # 通用样式
│   ├── base.css           # 基础样式（重置、布局）
│   ├── buttons.css        # 按钮样式
│   ├── cards.css          # 卡片样式
│   ├── forms.css          # 表单样式（输入框、开关等）
│   ├── modals.css         # 模态框样式
│   ├── sidebar.css        # 侧边栏样式
│   ├── status.css         # 状态标签样式
│   ├── tables.css         # 表格基础样式
│   └── utilities.css      # 工具类（加载动画、空状态等）
│
├── pages/                 # 页面特定样式
│   ├── accounts/          # 账号管理页面
│   │   └── accounts.css
│   ├── dashboard/         # 仪表盘页面
│   │   └── dashboard.css
│   ├── goods/             # 商品管理页面
│   │   └── goods.css
│   ├── messages/          # 消息页面
│   │   └── messages.css
│   └── qrlogin/           # 二维码登录页面
│       └── qrlogin.css
│
├── style.css              # 原始样式文件（保留兼容）
├── goods.css              # 原始商品样式（保留兼容）
└── style-new.css          # 新的主样式文件（导入所有模块）
```

## 使用方式

### 方式一：使用新的模块化结构（推荐）

在 HTML 中引入主样式文件：

```html
<link rel="stylesheet" href="css/style-new.css">
```

### 方式二：按需引入

如果只需要特定模块，可以单独引入：

```html
<!-- 通用样式 -->
<link rel="stylesheet" href="css/common/base.css">
<link rel="stylesheet" href="css/common/buttons.css">
<link rel="stylesheet" href="css/common/forms.css">

<!-- 页面样式 -->
<link rel="stylesheet" href="css/pages/dashboard/dashboard.css">
```

### 方式三：使用原始文件（兼容旧版）

```html
<link rel="stylesheet" href="css/style.css">
<link rel="stylesheet" href="css/goods.css">
```

## 模块说明

### common/ - 通用样式模块

- **base.css**: 全局重置、基础布局、容器样式
- **buttons.css**: 所有按钮样式（primary、success、danger、outline等）
- **cards.css**: 卡片容器样式
- **forms.css**: 表单元素（输入框、文本域、选择框、开关按钮）
- **modals.css**: 模态框弹窗样式
- **sidebar.css**: 侧边栏导航样式
- **status.css**: 状态标签（在线/离线、激活/未激活）
- **tables.css**: 表格基础样式
- **utilities.css**: 工具类（加载动画、空状态提示）

### pages/ - 页面特定样式

- **accounts/accounts.css**: 账号管理表格、操作按钮
- **dashboard/dashboard.css**: 仪表盘统计卡片、头部样式
- **goods/goods.css**: 商品表格、筛选栏、详情弹窗、图片预览
- **messages/messages.css**: 消息列表样式
- **qrlogin/qrlogin.css**: 二维码显示样式

## 迁移指南

如果要从旧的 `style.css` 迁移到新的模块化结构：

1. 将 HTML 中的引用改为：
   ```html
   <link rel="stylesheet" href="css/style-new.css">
   ```

2. 如果有自定义样式，建议创建新的模块文件，例如：
   - `css/pages/yourpage/yourpage.css`
   - 然后在 `style-new.css` 中添加导入

3. 旧文件 `style.css` 和 `goods.css` 保留作为备份

## 优势

1. **模块化**: 每个功能独立文件，易于维护
2. **可复用**: 通用样式可在多个页面使用
3. **按需加载**: 可以只加载需要的模块
4. **清晰结构**: 与 JS 模块结构保持一致
5. **易于扩展**: 添加新页面只需创建对应的 CSS 文件
