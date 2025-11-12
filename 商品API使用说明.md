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

### 3. 从数据库获取商品列表

**接口地址：** `POST /api/items/list`

**请求体：**
```json
{
  "status": 0
}
```

**请求参数说明：**
- `status` (必填): 商品状态（0=在售, 1=已下架, 2=已售出）

**响应示例：**
```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "totalCount": 45,
    "items": [
      {
        "id": 123456789,
        "xyGoodId": "123456789",
        "title": "商品标题",
        "coverPic": "//gw.alicdn.com/...",
        "infoPic": "[...]",
        "detailInfo": null,
        "soldPrice": "99.00",
        "status": 0,
        "createdTime": "2024-11-12 10:00:00",
        "updatedTime": "2024-11-12 10:00:00"
      }
    ]
  }
}
```

### 4. 获取商品详情（含详情信息填充）

**接口地址：** `POST /api/items/detail`

**请求体：**
```json
{
  "xyGoodId": "123456789",
  "cookieId": "default"
}
```

**请求参数说明：**
- `xyGoodId` (必填): 闲鱼商品ID
- `cookieId` (可选): Cookie ID（账号ID、账号备注或UNB）。如果提供，会调用API获取并更新商品详情

**响应示例：**
```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "item": {
      "id": 123456789,
      "xyGoodId": "123456789",
      "title": "商品标题",
      "coverPic": "//gw.alicdn.com/...",
      "infoPic": "[...]",
      "detailInfo": "{\"itemId\":\"123456789\",\"title\":\"商品标题\",\"description\":\"商品描述\",\"images\":[...]}",
      "soldPrice": "99.00",
      "status": 0,
      "createdTime": "2024-11-12 10:00:00",
      "updatedTime": "2024-11-12 10:00:00"
    }
  }
}
```

**功能说明：**
- 如果不提供 `cookieId`，只返回数据库中已有的商品信息
- 如果提供 `cookieId`，会调用闲鱼API `mtop.taobao.idle.pc.detail` 获取最新的商品详情，并更新到数据库的 `detail_info` 字段
- 这样可以避免在刷新商品列表时频繁请求详情接口，防止被风控
- 只在需要查看详情时才调用详情接口

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
# 刷新商品数据（从闲鱼API获取并保存到数据库）
curl -X POST "http://localhost:8080/api/items/refresh" \
  -H "Content-Type: application/json" \
  -d '{"cookieId":"default","pageSize":20,"maxPages":5}'

# 从数据库获取商品列表
curl -X POST "http://localhost:8080/api/items/list" \
  -H "Content-Type: application/json" \
  -d '{"status":0}'

# 获取商品详情（不更新详情信息）
curl -X POST "http://localhost:8080/api/items/detail" \
  -H "Content-Type: application/json" \
  -d '{"xyGoodId":"123456789"}'

# 获取商品详情（同时更新详情信息）
curl -X POST "http://localhost:8080/api/items/detail" \
  -H "Content-Type: application/json" \
  -d '{"xyGoodId":"123456789","cookieId":"default"}'
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

1. **refreshItems()** - 刷新商品数据
   - 从数据库获取Cookie
   - 生成签名和时间戳
   - 调用闲鱼API `mtop.idle.web.xyh.item.list` 获取商品列表
   - 自动分页获取所有商品
   - 保存商品基本信息到数据库（不包含详情）

2. **getItemsFromDb()** - 从数据库获取商品列表
   - 根据商品状态查询数据库
   - 返回已保存的商品列表

3. **getItemDetail()** - 获取商品详情
   - 从数据库获取商品基本信息
   - 如果提供cookieId，调用闲鱼API `mtop.taobao.idle.pc.detail` 获取详情
   - 更新数据库中的 `detail_info` 字段
   - 返回完整的商品信息

4. **XianyuApiUtils** - API调用工具类
   - 统一封装闲鱼API调用逻辑
   - 自动处理签名、时间戳、请求头等
   - 提供响应解析和错误处理

5. **XianyuSignUtils** - 签名工具类
   - Cookie解析
   - Token提取
   - MD5签名生成

## 数据库表结构

商品信息保存在 `xianyu_goods_info` 表中：

```sql
CREATE TABLE IF NOT EXISTS xianyu_goods_info (
    id BIGINT PRIMARY KEY,                        -- 表ID（使用雪花ID）
    xy_good_id VARCHAR(100) NOT NULL,             -- 闲鱼商品ID
    title VARCHAR(500),                           -- 商品标题
    cover_pic TEXT,                               -- 封面图片URL
    info_pic TEXT,                                -- 商品详情图片（JSON数组）
    detail_info TEXT,                             -- 商品详情信息（通过detail接口填充）
    detail_url TEXT,                              -- 商品详情页URL
    sold_price VARCHAR(50),                       -- 商品价格
    status TINYINT DEFAULT 0,                     -- 商品状态 0:在售 1:已下架 2:已售出
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,  -- 创建时间
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP   -- 更新时间
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_goods_xy_good_id ON xianyu_goods_info(xy_good_id);
CREATE INDEX IF NOT EXISTS idx_goods_status ON xianyu_goods_info(status);
```

**字段说明：**
- `detail_info`: 商品详情信息，初始为空，只有在调用 `/api/items/detail` 接口并提供 `cookieId` 时才会填充
- `detail_url`: 商品详情页URL，在刷新商品列表时自动保存，可用于直接访问商品详情页
- 这样设计可以避免在刷新商品列表时频繁请求详情接口，防止被风控

## 注意事项

1. **Cookie有效性**：确保账号的Cookie是有效的，否则无法获取商品信息
2. **请求频率**：刷新商品列表时会自动添加延迟（1秒），避免请求过快
3. **签名算法**：使用MD5生成签名，需要正确的Token和时间戳
4. **数据保存**：商品基本信息会在刷新时自动保存到数据库
5. **详情获取策略**：
   - 刷新商品列表时不获取详情，只保存基本信息（标题、价格、图片等）
   - 只有在调用 `/api/items/detail` 接口并提供 `cookieId` 时才获取详情
   - 这样可以避免请求过快被风控
   - 详情信息会缓存在数据库的 `detail_info` 字段中
6. **纯Java实现**：无需安装Python环境，所有逻辑都在Java中完成

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
- 2024-11-12: 新增商品详情获取功能，支持按需获取详情，避免风控
- 2024-11-12: 新增 `detail_url` 字段，在刷新商品列表时自动保存商品详情页URL
