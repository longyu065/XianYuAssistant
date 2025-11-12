# 商品详情API调试指南

## 问题描述

`detail_info` 字段一直为空，需要确认API调用是否正确执行。

## 调试步骤

### 步骤1：确认商品存在且有 detail_url

```sql
-- 查询商品信息
SELECT xy_good_id, title, detail_url, detail_info, updated_time 
FROM xianyu_goods_info 
LIMIT 5;
```

**检查点：**
- `xy_good_id` 是否存在
- `detail_url` 是否有值
- `detail_info` 是否为空

### 步骤2：测试API调用

使用测试脚本或curl命令：

```bash
# 方法1：使用测试脚本
测试商品详情API.bat

# 方法2：使用curl（替换商品ID）
curl -X POST "http://localhost:8080/api/items/detail" \
  -H "Content-Type: application/json" \
  -d "{\"xyGoodId\":\"YOUR_ITEM_ID\",\"cookieId\":\"1\"}"
```

**注意：**
- 必须提供 `cookieId` 参数
- `xyGoodId` 必须是数据库中存在的商品ID

### 步骤3：查看日志

启动应用后，查看日志输出：

```bash
# Windows
type logs\application.log | findstr "商品详情"

# 或者实时查看
tail -f logs/application.log | grep "商品详情"
```

**关键日志：**

1. **开始获取详情**
```
开始获取商品详情: itemId=xxx, cookieId=xxx
```

2. **缓存检查**
```
数据库中没有商品详情缓存，需要调用API获取: itemId=xxx
```
或
```
使用缓存的商品详情: itemId=xxx, 缓存时间=xxx
```

3. **Cookie获取**
```
Cookie获取成功，准备调用API: itemId=xxx
```

4. **API调用**
```
调用闲鱼API获取商品详情: itemId=xxx
```

5. **API响应**
```
API响应成功，响应长度: xxx, itemId=xxx
API响应状态检查通过，开始提取data字段: itemId=xxx
data字段提取成功，包含 xx 个字段, itemId=xxx
API获取商品详情成功: itemId=xxx, 详情长度=xxx
```

6. **更新数据库**
```
商品详情已更新: xyGoodId=xxx
```

### 步骤4：检查可能的错误

#### 错误1：商品不存在
```
商品不存在
```
**解决：** 确认 `xyGoodId` 是否正确

#### 错误2：未提供cookieId
```
detail_info为空但未提供cookieId，无法获取详情: xyGoodId=xxx
```
**解决：** 在请求中添加 `cookieId` 参数

#### 错误3：Cookie未找到
```
未找到账号Cookie: cookieId=xxx
```
**解决：** 
- 检查账号是否存在
- 检查Cookie是否已保存
- 尝试重新登录

#### 错误4：API调用失败
```
API调用失败：响应为空, itemId=xxx
```
**解决：**
- 检查网络连接
- 检查Cookie是否有效
- 查看完整错误日志

#### 错误5：API返回失败
```
API返回失败: FAIL_SYS_TOKEN_EMPTY::令牌为空, itemId=xxx
```
**解决：**
- Cookie中的 `_m_h5_tk` 可能失效
- 需要重新登录获取新Cookie

#### 错误6：无法提取data字段
```
无法提取data字段, itemId=xxx
```
**解决：**
- 查看完整响应内容
- 可能是API返回格式变化

## 完整测试流程

### 1. 准备测试数据

```bash
# 1.1 刷新商品列表（获取商品基本信息）
curl -X POST "http://localhost:8080/api/items/refresh" \
  -H "Content-Type: application/json" \
  -d "{\"cookieId\":\"1\",\"pageSize\":5,\"maxPages\":1}"

# 1.2 查询数据库，获取商品ID
# 在数据库工具中执行：
# SELECT xy_good_id FROM xianyu_goods_info LIMIT 1;
```

### 2. 测试获取详情

```bash
# 2.1 不提供cookieId（应该返回错误）
curl -X POST "http://localhost:8080/api/items/detail" \
  -H "Content-Type: application/json" \
  -d "{\"xyGoodId\":\"YOUR_ITEM_ID\"}"

# 预期结果：
# {
#   "code": 500,
#   "msg": "商品详情为空，请提供cookieId参数以获取详情"
# }

# 2.2 提供cookieId（应该成功获取）
curl -X POST "http://localhost:8080/api/items/detail" \
  -H "Content-Type: application/json" \
  -d "{\"xyGoodId\":\"YOUR_ITEM_ID\",\"cookieId\":\"1\"}"

# 预期结果：
# {
#   "code": 200,
#   "msg": "操作成功",
#   "data": {
#     "item": {
#       "id": xxx,
#       "xyGoodId": "xxx",
#       "title": "xxx",
#       "detailInfo": "{...}",  // 应该有内容
#       ...
#     }
#   }
# }
```

### 3. 验证数据库

```sql
-- 查询商品详情是否已保存
SELECT 
    xy_good_id,
    title,
    LENGTH(detail_info) as detail_length,
    updated_time
FROM xianyu_goods_info
WHERE xy_good_id = 'YOUR_ITEM_ID';
```

**检查点：**
- `detail_length` 应该大于0
- `updated_time` 应该是最新时间

### 4. 测试缓存机制

```bash
# 4.1 第一次调用（应该调用API）
curl -X POST "http://localhost:8080/api/items/detail" \
  -H "Content-Type: application/json" \
  -d "{\"xyGoodId\":\"YOUR_ITEM_ID\",\"cookieId\":\"1\"}"

# 查看日志，应该看到：
# 调用闲鱼API获取商品详情: itemId=xxx

# 4.2 第二次调用（应该使用缓存）
curl -X POST "http://localhost:8080/api/items/detail" \
  -H "Content-Type: application/json" \
  -d "{\"xyGoodId\":\"YOUR_ITEM_ID\",\"cookieId\":\"1\"}"

# 查看日志，应该看到：
# 使用缓存的商品详情: itemId=xxx, 缓存时间=xxx
```

## 常见问题排查

### Q1: 为什么 detail_info 一直为空？

**可能原因：**
1. 没有提供 `cookieId` 参数
2. Cookie失效或不存在
3. API调用失败
4. 网络问题

**排查步骤：**
1. 检查请求参数是否包含 `cookieId`
2. 查看日志中的错误信息
3. 确认Cookie是否有效
4. 测试网络连接

### Q2: 如何确认API是否被调用？

**方法：**
1. 查看日志中是否有 "调用闲鱼API获取商品详情"
2. 查看日志中是否有 "API响应成功"
3. 检查数据库的 `updated_time` 是否更新

### Q3: 如何强制刷新详情？

**方法1：清空数据库中的 detail_info**
```sql
UPDATE xianyu_goods_info 
SET detail_info = NULL 
WHERE xy_good_id = 'YOUR_ITEM_ID';
```

**方法2：修改 updated_time 为25小时前**
```sql
UPDATE xianyu_goods_info 
SET updated_time = datetime('now', '-25 hours') 
WHERE xy_good_id = 'YOUR_ITEM_ID';
```

然后重新调用API。

### Q4: 如何查看API的完整响应？

在日志中搜索 "完整响应内容" 或 "响应内容"，会打印出API的原始响应。

## 日志级别配置

如果需要更详细的日志，可以修改 `application.properties`：

```properties
# 设置日志级别为DEBUG
logging.level.com.feijimiao.xianyuassistant=DEBUG

# 或者只针对ItemServiceImpl
logging.level.com.feijimiao.xianyuassistant.service.impl.ItemServiceImpl=DEBUG
```

## 成功的日志示例

```
2024-11-12 15:00:00.123 [http-nio-8080-exec-1] INFO  ItemServiceImpl - 获取商品详情: xyGoodId=123456789, cookieId=1
2024-11-12 15:00:00.124 [http-nio-8080-exec-1] INFO  ItemServiceImpl - 商品详情为空，需要获取: xyGoodId=123456789
2024-11-12 15:00:00.125 [http-nio-8080-exec-1] INFO  ItemServiceImpl - 开始获取商品详情: itemId=123456789, cookieId=1
2024-11-12 15:00:00.126 [http-nio-8080-exec-1] INFO  ItemServiceImpl - 数据库中没有商品详情缓存，需要调用API获取: itemId=123456789
2024-11-12 15:00:00.127 [http-nio-8080-exec-1] INFO  ItemServiceImpl - 从数据库查询Cookie: cookieId=1
2024-11-12 15:00:00.128 [http-nio-8080-exec-1] INFO  ItemServiceImpl - 通过账号ID获取Cookie成功: accountId=1
2024-11-12 15:00:00.129 [http-nio-8080-exec-1] INFO  ItemServiceImpl - Cookie获取成功，准备调用API: itemId=123456789
2024-11-12 15:00:00.130 [http-nio-8080-exec-1] INFO  ItemServiceImpl - 调用闲鱼API获取商品详情: itemId=123456789
2024-11-12 15:00:00.500 [http-nio-8080-exec-1] INFO  ItemServiceImpl - API响应成功，响应长度: 5432, itemId=123456789
2024-11-12 15:00:00.501 [http-nio-8080-exec-1] INFO  ItemServiceImpl - API响应状态检查通过，开始提取data字段: itemId=123456789
2024-11-12 15:00:00.502 [http-nio-8080-exec-1] INFO  ItemServiceImpl - data字段提取成功，包含 15 个字段, itemId=123456789
2024-11-12 15:00:00.503 [http-nio-8080-exec-1] INFO  ItemServiceImpl - API获取商品详情成功: itemId=123456789, 详情长度=5432
2024-11-12 15:00:00.504 [http-nio-8080-exec-1] INFO  ItemServiceImpl - 通过API获取商品详情成功: itemId=123456789, 详情长度=5432
2024-11-12 15:00:00.505 [http-nio-8080-exec-1] INFO  GoodsInfoServiceImpl - 更新商品详情成功: xyGoodId=123456789
2024-11-12 15:00:00.506 [http-nio-8080-exec-1] INFO  ItemServiceImpl - 商品详情已更新: xyGoodId=123456789
2024-11-12 15:00:00.507 [http-nio-8080-exec-1] INFO  ItemServiceImpl - 获取商品详情成功: xyGoodId=123456789
```

## 总结

通过以上步骤，你应该能够：
1. 确认API是否被正确调用
2. 定位问题所在
3. 验证详情是否成功保存

如果仍然有问题，请提供完整的日志输出以便进一步分析。
