# 查询消息参数SQL示例

## 数据库字段对应关系

| 参数名 | 数据库字段 | 说明 |
|--------|-----------|------|
| cid | `session_id` | 会话ID（不带@goofish后缀） |
| toId | `sender_user_id` 或 `receiver_user_id` | 接收方用户ID（不带@goofish后缀） |
| accountId | `xianyu_account_id` | 账号ID |

## 常用查询SQL

### 1. 查询最近的聊天消息

```sql
SELECT 
    id,
    session_id as cid,
    sender_user_id,
    receiver_user_id,
    content_text,
    direction,
    datetime(message_time/1000, 'unixepoch', 'localtime') as message_time
FROM xianyu_chat_message
WHERE xianyu_account_id = 1
ORDER BY message_time DESC
LIMIT 20;
```

**字段说明**：
- `session_id` = cid（会话ID）
- `sender_user_id` = 发送者用户ID
- `receiver_user_id` = 接收者用户ID
- `direction`: 1=发送（我发的）2=接收（收到的）

### 2. 查询某个会话的所有消息

```sql
SELECT 
    session_id as cid,
    sender_user_id,
    receiver_user_id,
    content_text,
    direction,
    datetime(message_time/1000, 'unixepoch', 'localtime') as message_time
FROM xianyu_chat_message
WHERE xianyu_account_id = 1
  AND session_id = '3812882055015'  -- 替换为实际的会话ID
ORDER BY message_time ASC;
```

### 3. 查询与某个用户的对话

```sql
SELECT 
    session_id as cid,
    sender_user_id,
    receiver_user_id,
    content_text,
    direction,
    datetime(message_time/1000, 'unixepoch', 'localtime') as message_time
FROM xianyu_chat_message
WHERE xianyu_account_id = 1
  AND (sender_user_id = '3553532632' OR receiver_user_id = '3553532632')  -- 替换为实际的用户ID
ORDER BY message_time ASC;
```

### 4. 查询收到的消息（用于回复）

```sql
-- 查询收到的消息，获取发送者ID用于回复
SELECT 
    session_id as cid,
    sender_user_id as toId,  -- 回复时使用发送者ID
    content_text,
    datetime(message_time/1000, 'unixepoch', 'localtime') as message_time
FROM xianyu_chat_message
WHERE xianyu_account_id = 1
  AND direction = 2  -- 2=接收（收到的消息）
ORDER BY message_time DESC
LIMIT 10;
```

### 5. 按会话分组统计消息数量

```sql
SELECT 
    session_id as cid,
    COUNT(*) as message_count,
    MAX(sender_user_id) as sender_user_id,
    MAX(receiver_user_id) as receiver_user_id,
    MAX(content_text) as last_message,
    datetime(MAX(message_time)/1000, 'unixepoch', 'localtime') as last_message_time
FROM xianyu_chat_message
WHERE xianyu_account_id = 1
GROUP BY session_id
ORDER BY MAX(message_time) DESC;
```

### 6. 查询未读消息

```sql
SELECT 
    session_id as cid,
    sender_user_id as toId,
    content_text,
    datetime(message_time/1000, 'unixepoch', 'localtime') as message_time
FROM xianyu_chat_message
WHERE xianyu_account_id = 1
  AND is_read = 0  -- 0=未读
  AND direction = 2  -- 2=接收（收到的消息）
ORDER BY message_time DESC;
```

## 如何确定toId

### 场景1：回复收到的消息

如果你收到了一条消息（direction=2），想要回复：
- **toId** = `sender_user_id`（对方发来的消息的发送者）
- **cid** = `session_id`（会话ID）

```sql
-- 查询收到的消息，准备回复
SELECT 
    session_id as cid,
    sender_user_id as toId,  -- 使用发送者ID作为接收方
    content_text
FROM xianyu_chat_message
WHERE xianyu_account_id = 1
  AND direction = 2
ORDER BY message_time DESC
LIMIT 1;
```

### 场景2：继续之前的对话

如果你之前发送过消息（direction=1），想要继续对话：
- **toId** = `receiver_user_id`（之前发送消息的接收者）
- **cid** = `session_id`（会话ID）

```sql
-- 查询发送的消息，继续对话
SELECT 
    session_id as cid,
    receiver_user_id as toId,  -- 使用接收者ID
    content_text
FROM xianyu_chat_message
WHERE xianyu_account_id = 1
  AND direction = 1
ORDER BY message_time DESC
LIMIT 1;
```

## 完整示例

### 示例1：回复最近收到的消息

```sql
-- 步骤1：查询最近收到的消息
SELECT 
    session_id as cid,
    sender_user_id as toId,
    content_text as received_message
FROM xianyu_chat_message
WHERE xianyu_account_id = 1
  AND direction = 2
ORDER BY message_time DESC
LIMIT 1;

-- 假设查询结果：
-- cid = "3812882055015"
-- toId = "3553532632"
-- received_message = "商品还在吗？"

-- 步骤2：使用API发送回复
-- POST /api/websocket/sendMessage
-- {
--   "xianyuAccountId": 1,
--   "cid": "3812882055015",
--   "toId": "3553532632",
--   "text": "在的，欢迎咨询"
-- }
```

### 示例2：查询某个商品的对话

```sql
-- 查询与某个商品相关的对话
SELECT 
    session_id as cid,
    sender_user_id,
    receiver_user_id,
    content_text,
    item_id,
    item_title,
    direction,
    datetime(message_time/1000, 'unixepoch', 'localtime') as message_time
FROM xianyu_chat_message
WHERE xianyu_account_id = 1
  AND item_id = '123456789'  -- 替换为实际的商品ID
ORDER BY message_time ASC;
```

## 注意事项

1. **用户ID格式**：
   - 数据库中存储的用户ID可能包含 `@goofish` 后缀
   - 发送消息时不需要带后缀，代码会自动添加
   - 如果数据库中是 `3553532632@goofish`，使用时只需 `3553532632`

2. **会话ID格式**：
   - 会话ID通常是纯数字
   - 不需要带 `@goofish` 后缀

3. **消息方向**：
   - direction=1：我发送的消息
   - direction=2：我接收的消息

4. **时间戳转换**：
   - 数据库中的 `message_time` 是毫秒时间戳
   - 使用 `datetime(message_time/1000, 'unixepoch', 'localtime')` 转换为可读时间

## 测试工具

### 使用SQLite命令行

```bash
# Windows
sqlite3 xianyu_assistant.db

# 查询示例
sqlite> SELECT session_id, sender_user_id, receiver_user_id, content_text 
        FROM xianyu_chat_message 
        WHERE xianyu_account_id = 1 
        ORDER BY message_time DESC 
        LIMIT 5;
```

### 使用数据库管理工具

推荐使用以下工具查看数据库：
- DB Browser for SQLite
- DBeaver
- DataGrip
- VS Code SQLite扩展
