# 聊天消息商品ID关联功能说明

## 功能概述
为 `xianyu_chat_message` 表新增 `xy_goods_id` 字段，用于存储与聊天消息关联的商品ID。该ID从消息的 `reminder_url` 字段中解析获得。

## 数据来源
`reminder_url` 示例：
```
leamarket://message_chat?itemId=926462531165&peerUserId=2218021801256
```

从URL中提取 `itemId` 参数值（如：`926462531165`）作为 `xy_goods_id`。

## 实现内容

### 1. 数据库变更
- **表名**：`xianyu_chat_message`
- **新增字段**：`xy_goods_id VARCHAR(100)` - 闲鱼商品ID
- **新增索引**：`idx_chat_message_goods_id` - 提高按商品ID查询的性能

### 2. 代码变更

#### 实体类（XianyuChatMessage.java）
```java
private String xyGoodsId;  // 闲鱼商品ID，从reminder_url中的itemId参数解析
```

#### Mapper（XianyuChatMessageMapper.java）
- INSERT语句中添加 `xy_goods_id` 字段

#### 服务实现（ChatMessageServiceImpl.java）
- 新增 `extractItemIdFromUrl()` 方法，用于从URL中解析itemId
- 在保存消息时自动解析并设置 `xy_goods_id`
- 支持两个保存方法：
  - `saveChatMessageFromMap()` - 不带lwp字段
  - `saveChatMessageFromMapWithLwp()` - 带lwp字段

### 3. URL解析逻辑
```java
private String extractItemIdFromUrl(String url) {
    // 查找 itemId= 参数
    // 提取参数值直到遇到 & 或字符串结束
    // 返回商品ID或null
}
```

解析规则：
1. 查找 `itemId=` 在URL中的位置
2. 从 `itemId=` 后开始提取
3. 遇到 `&` 或字符串结束时停止
4. 返回提取的商品ID

## 数据库迁移

### 迁移脚本
文件：`src/main/resources/sql/migration_add_xy_goods_id.sql`

执行步骤：
1. 添加 `xy_goods_id` 字段
2. 创建索引
3. 更新现有数据（从 `reminder_url` 中提取 `itemId`）

### 手动执行迁移
```bash
# 使用SQLite命令行工具
sqlite3 xianyu_assistant.db < src/main/resources/sql/migration_add_xy_goods_id.sql
```

## 使用场景
1. **商品关联查询**：根据商品ID查询相关的聊天消息
2. **数据分析**：统计某个商品的咨询量
3. **消息分类**：按商品维度组织聊天记录
4. **业务关联**：将聊天消息与商品信息表（xianyu_goods）关联

## 注意事项
1. 如果 `reminder_url` 为空或不包含 `itemId` 参数，`xy_goods_id` 将为 `NULL`
2. 解析失败不会影响消息的正常保存
3. 现有数据需要执行迁移脚本才能填充 `xy_goods_id`
4. 该字段可用于与 `xianyu_goods` 表的 `xy_good_id` 字段关联

## 测试建议
1. 测试包含 `itemId` 的URL解析
2. 测试不包含 `itemId` 的URL处理
3. 测试 `reminder_url` 为空的情况
4. 验证数据库索引是否生效
5. 测试现有数据的迁移更新
