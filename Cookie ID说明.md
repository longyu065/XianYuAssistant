# Cookie ID 说明文档

## 什么是 Cookie ID？

**Cookie ID** 是系统中用于标识和管理不同闲鱼账号的唯一标识符。

## 作用

1. **账号区分**：每个闲鱼账号都有一个唯一的Cookie ID
2. **数据隔离**：不同账号的数据通过Cookie ID进行隔离
3. **快速查询**：通过Cookie ID可以快速从数据库中查询账号信息

## Cookie ID 格式

Cookie ID可以是任意字符串，建议使用有意义的命名：

### 推荐格式：
- `default` - 默认账号
- `account1`, `account2` - 按序号命名
- `user_张三`, `user_李四` - 按用户名命名
- `shop_旗舰店` - 按店铺名命名
- `test_001` - 测试账号

### 命名规则：
- ✅ 支持中文、英文、数字、下划线
- ✅ 建议长度：3-50个字符
- ✅ 建议使用有意义的名称，方便管理
- ❌ 不建议使用特殊符号（如：@#$%等）

## 使用场景

### 1. 扫码登录
```
用户扫码登录后，系统会：
1. 获取闲鱼账号的Cookie信息
2. 自动创建账号记录，生成账号ID
3. 将Cookie保存到数据库，关联到账号ID
```

### 2. 商品管理
```
获取商品信息时可以使用以下任一方式：
1. 账号ID（推荐）：数字ID，如 1、2、3
2. 账号备注：如 账号_12345678
3. UNB：闲鱼账号的唯一标识

系统会按以下顺序查询：
- 先尝试作为账号ID查询
- 再尝试作为账号备注查询
- 最后尝试作为UNB查询
```

### 3. 多账号管理
```
可以同时管理多个闲鱼账号：
- Cookie ID: account1 → 账号A的Cookie
- Cookie ID: account2 → 账号B的Cookie
- Cookie ID: account3 → 账号C的Cookie
```

## 数据库存储

Cookie信息在数据库中的存储结构：

```sql
CREATE TABLE cookies (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    cookie_id TEXT UNIQUE NOT NULL,     -- Cookie ID（唯一标识）
    cookie TEXT NOT NULL,                -- Cookie字符串
    user_id INTEGER,                     -- 用户ID（可选）
    nickname TEXT,                       -- 账号昵称
    created_at TIMESTAMP,                -- 创建时间
    updated_at TIMESTAMP                 -- 更新时间
);
```

## 示例

### 示例1：单账号使用
```
Cookie ID: default
用途：个人使用，只有一个闲鱼账号
```

### 示例2：多账号管理
```
Cookie ID: shop_main     → 主店铺账号
Cookie ID: shop_backup   → 备用店铺账号
Cookie ID: personal      → 个人账号
```

### 示例3：团队协作
```
Cookie ID: team_张三     → 张三的账号
Cookie ID: team_李四     → 李四的账号
Cookie ID: team_王五     → 王五的账号
```

## API使用示例

### 获取商品列表（使用账号ID）
```bash
curl -X POST "http://localhost:8080/api/items/list" \
  -H "Content-Type: application/json" \
  -d '{
    "cookieId": "1",
    "pageNumber": 1,
    "pageSize": 20
  }'
```

### 获取商品列表（使用账号备注）
```bash
curl -X POST "http://localhost:8080/api/items/list" \
  -H "Content-Type: application/json" \
  -d '{
    "cookieId": "账号_12345678",
    "pageNumber": 1,
    "pageSize": 20
  }'
```

### 获取所有商品（使用UNB）
```bash
curl -X POST "http://localhost:8080/api/items/all" \
  -H "Content-Type: application/json" \
  -d '{
    "cookieId": "2202640918079",
    "pageSize": 20
  }'
```

## 常见问题

### Q1: Cookie ID可以修改吗？
A: 可以，但需要同时更新数据库中的记录。建议在创建时就使用合适的Cookie ID。

### Q2: 忘记了Cookie ID怎么办？
A: 可以通过数据库查询所有的Cookie ID：
```sql
SELECT cookie_id, nickname, created_at FROM cookies;
```

### Q3: Cookie ID重复会怎样？
A: 数据库中Cookie ID是唯一的，如果尝试插入重复的Cookie ID会失败。

### Q4: 如何删除某个账号？
A: 通过Cookie ID删除对应的记录：
```sql
DELETE FROM cookies WHERE cookie_id = 'account1';
```

### Q5: Cookie ID区分大小写吗？
A: 是的，`Account1` 和 `account1` 是不同的Cookie ID。

## 最佳实践

1. **统一命名规范**：团队使用时建议制定统一的命名规范
2. **定期清理**：删除不再使用的账号Cookie
3. **备份重要账号**：对重要账号的Cookie ID做好记录
4. **安全管理**：Cookie ID不要包含敏感信息
5. **文档记录**：维护一份Cookie ID与账号的对应关系文档

## 安全提示

⚠️ **重要提示**：
- Cookie信息包含账号登录凭证，请妥善保管
- 不要将Cookie ID和Cookie信息泄露给他人
- 定期更新Cookie，保持账号安全
- 使用完毕后及时退出登录

## 技术支持

如有问题，请查看：
- [闲鱼扫码登录使用说明.md](闲鱼扫码登录使用说明.md)
- [商品API使用说明.md](商品API使用说明.md)
- [API测试示例.md](API测试示例.md)
