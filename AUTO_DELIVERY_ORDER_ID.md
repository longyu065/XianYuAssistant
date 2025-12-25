# 自动发货订单ID保存功能

## 功能说明

在自动发货流程中，从原始消息中解析订单ID并保存到 `xianyu_goods_auto_delivery_record` 表的 `order_id` 字段。

## 最新更新 (2025-12-23)

### 前端表格显示订单ID

在 `/auto-delivery` 页面的自动发货记录表格中添加了订单ID列，方便查看和追踪订单。

#### 修改文件

1. **前端TypeScript接口** - `vue-code/src/api/auto-delivery-record.ts`
   - 在 `AutoDeliveryRecord` 接口中添加 `orderId?: string` 字段

2. **前端Vue组件** - `vue-code/src/views/auto-delivery/index.vue`
   - 在表格中添加订单ID列
   - 列标题改为"序号"（原来是"ID"）
   - 订单ID列宽度设置为180px
   - 添加样式：使用等宽字体和蓝色显示订单ID

3. **后端DTO** - `src/main/java/com/feijimiao/xianyuassistant/controller/dto/AutoDeliveryRecordDTO.java`
   - 添加 `orderId` 字段

4. **后端Mapper** - `src/main/java/com/feijimiao/xianyuassistant/mapper/XianyuGoodsAutoDeliveryRecordMapper.java`
   - 在查询结果映射中添加 `@Result(property = "orderId", column = "order_id")`

5. **后端Service** - `src/main/java/com/feijimiao/xianyuassistant/service/impl/AutoDeliveryServiceImpl.java`
   - 在DTO转换时添加 `dto.setOrderId(record.getOrderId())`

#### 表格列顺序

| 序号 | 订单ID | 买家ID | 买家名称 | 发货内容 | 状态 | 发货时间 |
|------|--------|--------|----------|----------|------|----------|

#### 样式特点

- 订单ID使用等宽字体 `Courier New`
- 颜色为蓝色 `#409eff`
- 字体加粗，便于识别
- 如果订单ID为空，显示 `-`

## 订单ID提取逻辑

### 消息结构

从 `[已付款，待发货]` 消息中提取订单ID：

```json
{
  "1": {
    "6": {
      "10": {
        "reminderUrl": "fleamarket://order_detail?id=4963617867817533226&role=seller"
      }
    }
  }
}
```

### 提取路径

1. 访问 `1.6.10.reminderUrl` 字段
2. 从 URL 中提取 `id` 参数
3. 示例：`fleamarket://order_detail?id=4963617867817533226&role=seller`
4. 提取结果：`4963617867817533226`

### 实现代码

```java
private String extractOrderIdFromMessage(String completeMsg) {
    // 解析JSON
    Map<String, Object> data = objectMapper.readValue(completeMsg, Map.class);
    
    // 访问 1.6.10.reminderUrl
    Object level1 = data.get("1");
    if (level1 instanceof Map) {
        Object level6 = ((Map<?, ?>) level1).get("6");
        if (level6 instanceof Map) {
            Object level10 = ((Map<?, ?>) level6).get("10");
            if (level10 instanceof Map) {
                String reminderUrl = (String) ((Map<?, ?>) level10).get("reminderUrl");
                
                // 从URL中提取id参数
                if (reminderUrl != null && reminderUrl.contains("id=")) {
                    String[] parts = reminderUrl.split("[?&]");
                    for (String part : parts) {
                        if (part.startsWith("id=")) {
                            return part.substring(3);
                        }
                    }
                }
            }
        }
    }
    
    return null;
}
```

## 数据流程

### 1. 消息接收

```
WebSocket 接收消息
    ↓
SyncMessageHandler 解析消息
    ↓
提取订单ID (extractOrderIdFromMessage)
    ↓
设置到 ChatMessageData.orderId
    ↓
发布 ChatMessageReceivedEvent 事件
```

### 2. 自动发货处理

```
ChatMessageEventAutoDeliveryListener 监听事件
    ↓
检查是否需要自动发货
    ↓
创建发货记录
    ↓
设置 record.setOrderId(message.getOrderId())
    ↓
保存到数据库
```

### 3. 前端展示

```
用户访问 /auto-delivery 页面
    ↓
选择账号和商品
    ↓
调用 getAutoDeliveryRecords API
    ↓
后端查询数据库（包含order_id字段）
    ↓
转换为DTO（包含orderId）
    ↓
前端表格显示订单ID
```

## 数据库字段

### 表名
`xianyu_goods_auto_delivery_record`

### 字段定义
```sql
order_id VARCHAR(100)
```

### 索引
```sql
CREATE INDEX IF NOT EXISTS idx_auto_delivery_record_order_id 
ON xianyu_goods_auto_delivery_record(order_id)
```

## 代码改进

### 改进前

```java
// 创建发货记录
XianyuGoodsAutoDeliveryRecord record = new XianyuGoodsAutoDeliveryRecord();
record.setXianyuAccountId(message.getXianyuAccountId());
record.setXyGoodsId(message.getXyGoodsId());
record.setPnmId(message.getPnmId());
record.setBuyerUserId(message.getSenderUserId());
record.setBuyerUserName(buyerUserName);
// ❌ 缺少订单ID
record.setContent(null);
record.setState(0);
```

### 改进后

```java
// 创建发货记录
XianyuGoodsAutoDeliveryRecord record = new XianyuGoodsAutoDeliveryRecord();
record.setXianyuAccountId(message.getXianyuAccountId());
record.setXyGoodsId(message.getXyGoodsId());
record.setPnmId(message.getPnmId());
record.setBuyerUserId(message.getSenderUserId());
record.setBuyerUserName(buyerUserName);
record.setOrderId(message.getOrderId()); // ✅ 设置订单ID
record.setContent(null);
record.setState(0);

log.info("【账号{}】准备创建发货记录: pnmId={}, xyGoodsId={}, buyerUserName={}, orderId={}", 
        message.getXianyuAccountId(), message.getPnmId(), message.getXyGoodsId(), 
        buyerUserName, message.getOrderId());
```

## 日志输出

### 消息解析日志

```
【账号5】准备发布ChatMessageReceivedEvent事件，完整消息对象: 
  pnmId=3883956134969.PNM
  sId=55637234522@goofish
  contentType=32
  msgContent=[已付款，待发货]
  xyGoodsId=994585081620
  senderUserId=3553532632
  senderUserName=肥极喵喵喵
  orderId=4963617867817533226
```

### 发货记录创建日志

```
【账号5】准备创建发货记录: pnmId=3883956134969.PNM, xyGoodsId=994585081620, buyerUserName=肥极喵喵喵, orderId=4963617867817533226

【账号5】✅ 创建发货记录成功: recordId=1, pnmId=3883956134969.PNM, xyGoodsId=994585081620, buyerUserName=肥极喵喵喵, orderId=4963617867817533226, state=0（待发货）
```

## 使用场景

### 1. 订单追踪

通过订单ID可以：
- 查询订单详情
- 追踪订单状态
- 关联订单和发货记录

### 2. 数据分析

- 统计每个订单的发货情况
- 分析订单转化率
- 监控异常订单

### 3. 订单管理

- 根据订单ID查询发货记录
- 批量处理订单
- 订单状态同步

## 数据示例

### 发货记录表数据

| id | xianyu_account_id | xy_goods_id | order_id | buyer_user_id | buyer_user_name | state | content | created_time |
|----|-------------------|-------------|----------|---------------|-----------------|-------|---------|--------------|
| 1 | 5 | 994585081620 | 4963617867817533226 | 3553532632 | 肥极喵喵喵 | 1 | 您好，商品已发货... | 2025-12-23 16:00:00 |

## 注意事项

### 1. 订单ID可能为空

- 某些消息类型可能不包含订单ID
- 需要处理 `null` 值的情况
- 不影响其他功能的正常运行

### 2. URL格式变化

- 闲鱼可能更改URL格式
- 需要定期检查提取逻辑
- 建议添加日志记录提取失败的情况

### 3. 数据一致性

- 订单ID应该是唯一的
- 同一订单可能有多条消息
- 使用 `pnm_id` 作为唯一标识防止重复

## 测试建议

1. ✅ 测试正常的已付款消息
2. ✅ 测试订单ID提取是否正确
3. ✅ 测试数据库保存是否成功
4. ✅ 测试日志输出是否完整
5. ✅ 测试订单ID为空的情况
6. ✅ 测试重复消息的处理
7. ✅ 测试前端表格显示订单ID
8. ✅ 测试订单ID样式是否正确

## 文件修改

### 后端
✅ `src/main/java/com/feijimiao/xianyuassistant/event/chatMessageEvent/ChatMessageEventAutoDeliveryListener.java`
- 添加 `record.setOrderId(message.getOrderId())`
- 改进日志输出，显示订单ID

✅ `src/main/java/com/feijimiao/xianyuassistant/controller/dto/AutoDeliveryRecordDTO.java`
- 添加 `orderId` 字段

✅ `src/main/java/com/feijimiao/xianyuassistant/mapper/XianyuGoodsAutoDeliveryRecordMapper.java`
- 在查询结果映射中添加 `orderId` 字段映射

✅ `src/main/java/com/feijimiao/xianyuassistant/service/impl/AutoDeliveryServiceImpl.java`
- 在DTO转换时添加订单ID映射

### 前端
✅ `vue-code/src/api/auto-delivery-record.ts`
- 在 `AutoDeliveryRecord` 接口中添加 `orderId` 字段

✅ `vue-code/src/views/auto-delivery/index.vue`
- 在表格中添加订单ID列
- 添加订单ID样式

## 相关文件

- `src/main/java/com/feijimiao/xianyuassistant/websocket/handler/SyncMessageHandler.java` - 订单ID提取逻辑
- `src/main/java/com/feijimiao/xianyuassistant/entity/XianyuGoodsAutoDeliveryRecord.java` - 实体类定义
- `src/main/java/com/feijimiao/xianyuassistant/event/chatMessageEvent/ChatMessageData.java` - 事件数据类
- `src/main/java/com/feijimiao/xianyuassistant/config/DatabaseInitListener.java` - 数据库字段定义

## 总结

现在自动发货功能会自动从消息中提取订单ID并保存到数据库，并在前端表格中显示，方便后续的订单追踪和管理！

</content>
</file>

### 消息结构

从 `[已付款，待发货]` 消息中提取订单ID：

```json
{
  "1": {
    "6": {
      "10": {
        "reminderUrl": "fleamarket://order_detail?id=4963617867817533226&role=seller"
      }
    }
  }
}
```

### 提取路径

1. 访问 `1.6.10.reminderUrl` 字段
2. 从 URL 中提取 `id` 参数
3. 示例：`fleamarket://order_detail?id=4963617867817533226&role=seller`
4. 提取结果：`4963617867817533226`

### 实现代码

```java
private String extractOrderIdFromMessage(String completeMsg) {
    // 解析JSON
    Map<String, Object> data = objectMapper.readValue(completeMsg, Map.class);
    
    // 访问 1.6.10.reminderUrl
    Object level1 = data.get("1");
    if (level1 instanceof Map) {
        Object level6 = ((Map<?, ?>) level1).get("6");
        if (level6 instanceof Map) {
            Object level10 = ((Map<?, ?>) level6).get("10");
            if (level10 instanceof Map) {
                String reminderUrl = (String) ((Map<?, ?>) level10).get("reminderUrl");
                
                // 从URL中提取id参数
                if (reminderUrl != null && reminderUrl.contains("id=")) {
                    String[] parts = reminderUrl.split("[?&]");
                    for (String part : parts) {
                        if (part.startsWith("id=")) {
                            return part.substring(3);
                        }
                    }
                }
            }
        }
    }
    
    return null;
}
```

## 数据流程

### 1. 消息接收

```
WebSocket 接收消息
    ↓
SyncMessageHandler 解析消息
    ↓
提取订单ID (extractOrderIdFromMessage)
    ↓
设置到 ChatMessageData.orderId
    ↓
发布 ChatMessageReceivedEvent 事件
```

### 2. 自动发货处理

```
ChatMessageEventAutoDeliveryListener 监听事件
    ↓
检查是否需要自动发货
    ↓
创建发货记录
    ↓
设置 record.setOrderId(message.getOrderId())
    ↓
保存到数据库
```

## 数据库字段

### 表名
`xianyu_goods_auto_delivery_record`

### 字段定义
```sql
order_id VARCHAR(100)
```

### 索引
```sql
CREATE INDEX IF NOT EXISTS idx_auto_delivery_record_order_id 
ON xianyu_goods_auto_delivery_record(order_id)
```

## 代码改进

### 改进前

```java
// 创建发货记录
XianyuGoodsAutoDeliveryRecord record = new XianyuGoodsAutoDeliveryRecord();
record.setXianyuAccountId(message.getXianyuAccountId());
record.setXyGoodsId(message.getXyGoodsId());
record.setPnmId(message.getPnmId());
record.setBuyerUserId(message.getSenderUserId());
record.setBuyerUserName(buyerUserName);
// ❌ 缺少订单ID
record.setContent(null);
record.setState(0);
```

### 改进后

```java
// 创建发货记录
XianyuGoodsAutoDeliveryRecord record = new XianyuGoodsAutoDeliveryRecord();
record.setXianyuAccountId(message.getXianyuAccountId());
record.setXyGoodsId(message.getXyGoodsId());
record.setPnmId(message.getPnmId());
record.setBuyerUserId(message.getSenderUserId());
record.setBuyerUserName(buyerUserName);
record.setOrderId(message.getOrderId()); // ✅ 设置订单ID
record.setContent(null);
record.setState(0);

log.info("【账号{}】准备创建发货记录: pnmId={}, xyGoodsId={}, buyerUserName={}, orderId={}", 
        message.getXianyuAccountId(), message.getPnmId(), message.getXyGoodsId(), 
        buyerUserName, message.getOrderId());
```

## 日志输出

### 消息解析日志

```
【账号5】准备发布ChatMessageReceivedEvent事件，完整消息对象: 
  pnmId=3883956134969.PNM
  sId=55637234522@goofish
  contentType=32
  msgContent=[已付款，待发货]
  xyGoodsId=994585081620
  senderUserId=3553532632
  senderUserName=肥极喵喵喵
  orderId=4963617867817533226
```

### 发货记录创建日志

```
【账号5】准备创建发货记录: pnmId=3883956134969.PNM, xyGoodsId=994585081620, buyerUserName=肥极喵喵喵, orderId=4963617867817533226

【账号5】✅ 创建发货记录成功: recordId=1, pnmId=3883956134969.PNM, xyGoodsId=994585081620, buyerUserName=肥极喵喵喵, orderId=4963617867817533226, state=0（待发货）
```

## 使用场景

### 1. 订单追踪

通过订单ID可以：
- 查询订单详情
- 追踪订单状态
- 关联订单和发货记录

### 2. 数据分析

- 统计每个订单的发货情况
- 分析订单转化率
- 监控异常订单

### 3. 订单管理

- 根据订单ID查询发货记录
- 批量处理订单
- 订单状态同步

## 数据示例

### 发货记录表数据

| id | xianyu_account_id | xy_goods_id | order_id | buyer_user_id | buyer_user_name | state | content | created_time |
|----|-------------------|-------------|----------|---------------|-----------------|-------|---------|--------------|
| 1 | 5 | 994585081620 | 4963617867817533226 | 3553532632 | 肥极喵喵喵 | 1 | 您好，商品已发货... | 2025-12-23 16:00:00 |

## 注意事项

### 1. 订单ID可能为空

- 某些消息类型可能不包含订单ID
- 需要处理 `null` 值的情况
- 不影响其他功能的正常运行

### 2. URL格式变化

- 闲鱼可能更改URL格式
- 需要定期检查提取逻辑
- 建议添加日志记录提取失败的情况

### 3. 数据一致性

- 订单ID应该是唯一的
- 同一订单可能有多条消息
- 使用 `pnm_id` 作为唯一标识防止重复

## 测试建议

1. ✅ 测试正常的已付款消息
2. ✅ 测试订单ID提取是否正确
3. ✅ 测试数据库保存是否成功
4. ✅ 测试日志输出是否完整
5. ✅ 测试订单ID为空的情况
6. ✅ 测试重复消息的处理

## 文件修改

✅ `src/main/java/com/feijimiao/xianyuassistant/event/chatMessageEvent/ChatMessageEventAutoDeliveryListener.java`
- 添加 `record.setOrderId(message.getOrderId())`
- 改进日志输出，显示订单ID

## 相关文件

- `src/main/java/com/feijimiao/xianyuassistant/websocket/handler/SyncMessageHandler.java` - 订单ID提取逻辑
- `src/main/java/com/feijimiao/xianyuassistant/entity/XianyuGoodsAutoDeliveryRecord.java` - 实体类定义
- `src/main/java/com/feijimiao/xianyuassistant/event/chatMessageEvent/ChatMessageData.java` - 事件数据类
- `src/main/java/com/feijimiao/xianyuassistant/config/DatabaseInitListener.java` - 数据库字段定义

## 总结

现在自动发货功能会自动从消息中提取订单ID并保存到数据库，方便后续的订单追踪和管理！
