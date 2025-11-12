# 闲鱼商品API使用说明

## 功能概述

本系统实现了从闲鱼平台获取商品信息的功能，包括：

1. **获取指定页的商品信息** - 支持分页查询
2. **获取所有商品信息** - 自动分页获取全部商品
3. **从数据库获取商品** - 查询已保存的商品信息

## API接口

### 1. 获取指定页商品列表

**接口地址：** `POST /api/items/list`

**请求体：**
```json
{
  "cookieId": "default",
  "pageNumber": 1,
  "pageSize": 20
}
```

**请求参数说明：**
- `cookieId` (必填): 账号ID
- `pageNumber` (可选): 页码，默认为1
- `pageSize` (可选): 每页数量，默认为20

**响应示例：**
```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "success": true,
    "pageNumber": 1,
    "pageSize": 20,
    "currentCount": 15,
    "savedCount": 15,
    "items": [
      {
        "id": "123456789",
        "title": "商品标题",
        "price": "99.00",
        "price_text": "¥99.00",
        "category_id": "50025969",
        "auction_type": "b",
        "item_status": 1,
        "detail_url": "https://...",
        "pic_info": {
          "picUrl": "//gw.alicdn.com/...",
          "width": 800,
          "height": 800
        }
      }
    ]
  }
}
```

### 2. 获取所有商品（自动分页）

**接口地址：** `POST /api/items/all`

**请求体：**
```json
{
  "cookieId": "default",
  "pageSize": 20,
  "maxPages": 5
}
```

**请求参数说明：**
- `cookieId` (必填): 账号ID
- `pageSize` (可选): 每页数量，默认为20
- `maxPages` (可选): 最大页数限制，不填表示无限制

**响应示例：**
```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "success": true,
    "totalPages": 3,
    "totalCount": 45,
    "totalSaved": 45,
    "items": [...]
  }
}
```

### 3. 从数据库获取商品

**接口地址：** `POST /api/items/db`

**请求体：**
```json
{
  "cookieId": "default"
}
```

**请求参数说明：**
- `cookieId` (必填): 账号ID

**响应示例：**
```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "success": true,
    "count": 45,
    "items": [
      {
        "item_id": "123456789",
        "item_title": "商品标题",
        "item_price": "¥99.00",
        "item_category": "50025969",
        "item_description": "",
        "item_detail": "{...}"
      }
    ]
  }
}
```

## 使用方式

### 方式1：通过Web界面

1. 启动应用后，访问：`http://localhost:8080/items.html`
2. 输入账号ID（Cookie ID）
3. 选择操作：
   - 点击"获取指定页商品"：获取单页商品
   - 点击"获取所有商品"：自动分页获取全部商品
   - 点击"从数据库获取"：查询已保存的商品

### 方式2：通过API调用

使用任何HTTP客户端（如Postman、curl等）调用上述API接口。

**curl示例：**
```bash
# 获取第1页商品
curl -X POST "http://localhost:8080/api/items/list" \
  -H "Content-Type: application/json" \
  -d '{"cookieId":"default","pageNumber":1,"pageSize":20}'

# 获取所有商品
curl -X POST "http://localhost:8080/api/items/all" \
  -H "Content-Type: application/json" \
  -d '{"cookieId":"default","pageSize":20,"maxPages":5}'

# 从数据库获取
curl -X POST "http://localhost:8080/api/items/db" \
  -H "Content-Type: application/json" \
  -d '{"cookieId":"default"}'
```

## 实现原理

### 后端架构

```
Controller (ItemController)
    ↓
Service (ItemService)
    ↓
XianyuSignUtils (签名工具)
    ↓
HttpClientUtils (HTTP客户端)
    ↓
闲鱼API
```

### 核心功能

1. **getItemList()** - Java方法
   - 从数据库获取Cookie
   - 生成签名和时间戳
   - 调用闲鱼API获取指定页的商品列表
   - 解析商品信息并返回

2. **getAllItems()** - Java方法
   - 自动分页循环调用getItemList()
   - 直到获取所有商品或达到最大页数限制
   - 返回所有商品的汇总信息

3. **XianyuSignUtils** - 签名工具类
   - Cookie解析
   - Token提取
   - MD5签名生成

## 数据库表结构

商品信息保存在 `item_info` 表中：

```sql
CREATE TABLE item_info (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    cookie_id TEXT NOT NULL,
    item_id TEXT NOT NULL,
    item_title TEXT,
    item_description TEXT,
    item_category TEXT,
    item_price TEXT,
    item_detail TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(cookie_id, item_id)
);
```

## 注意事项

1. **Cookie有效性**：确保账号的Cookie是有效的，否则无法获取商品信息
2. **请求频率**：获取所有商品时会自动添加延迟（1秒），避免请求过快
3. **签名算法**：使用MD5生成签名，需要正确的Token和时间戳
4. **数据保存**：商品信息会自动保存到数据库，避免重复获取
5. **纯Java实现**：无需安装Python环境，所有逻辑都在Java中完成

## 错误处理

常见错误及解决方案：

1. **"未找到账号Cookie"** - 检查cookieId是否正确，需要先添加账号Cookie
2. **"请求闲鱼API失败"** - 检查网络连接，确认Cookie是否有效
3. **"Token失效"** - 需要更新Cookie，重新登录获取新的Cookie
4. **"签名错误"** - 检查时间戳和Token是否正确

## 配置说明

在 `application.properties` 中可以配置：

```properties
# 闲鱼API配置
xianyu.api.url=https://h5api.m.goofish.com/h5/mtop.idle.web.xyh.item.list/1.0/
xianyu.api.appKey=34839810
xianyu.api.timeout=20000
```

## 开发说明

### 添加新功能

1. 在 `ItemService` 中添加新的服务方法
2. 在 `ItemController` 中添加新的API接口
3. 定义对应的ReqDTO和RespDTO
4. 更新前端items.html添加新功能按钮

### 测试

1. 单元测试：测试各个Java方法
2. 集成测试：通过API接口测试完整流程
3. 界面测试：通过items.html测试用户交互

## 更新日志

- 2024-11-12: 初始版本，纯Java实现商品获取功能
