# 操作日志使用说明

## 功能概述

操作日志系统用于记录系统中的各种操作，如登录、发送消息、自动发货等。

## 数据库表结构

```sql
xianyu_operation_log
- id: 主键
- xianyu_account_id: 账号ID
- operation_type: 操作类型（LOGIN, SEND_MESSAGE等）
- operation_module: 操作模块（ACCOUNT, MESSAGE等）
- operation_desc: 操作描述
- operation_status: 操作状态（1-成功，0-失败，2-部分成功）
- target_type: 目标类型
- target_id: 目标ID
- request_params: 请求参数（JSON）
- response_result: 响应结果（JSON）
- error_message: 错误信息
- duration_ms: 操作耗时（毫秒）
- create_time: 创建时间
```

## 使用方法

### 1. 简单记录

```java
@Autowired
private OperationLogService operationLogService;

// 记录登录成功
operationLogService.log(accountId, "LOGIN", "扫码登录成功", 1);

// 记录发送消息失败
operationLogService.log(accountId, "SEND_MESSAGE", "发送消息失败", 0);
```

### 2. 完整记录

```java
operationLogService.log(
    accountId,              // 账号ID
    "AUTO_DELIVERY",        // 操作类型
    "ORDER",                // 操作模块
    "自动发货成功",          // 操作描述
    1,                      // 状态：1-成功
    "ORDER",                // 目标类型
    orderId,                // 目标ID
    requestJson,            // 请求参数
    responseJson,           // 响应结果
    null,                   // 错误信息
    durationMs              // 耗时
);
```

### 3. 使用实体类

```java
XianyuOperationLog log = new XianyuOperationLog();
log.setXianyuAccountId(accountId);
log.setOperationType("TOKEN_REFRESH");
log.setOperationModule("SYSTEM");
log.setOperationDesc("Token刷新成功");
log.setOperationStatus(1);
log.setDurationMs(1500);
operationLogService.log(log);
```

## 操作类型定义

- LOGIN: 扫码登录
- WEBSOCKET_CONNECT: WebSocket连接
- WEBSOCKET_DISCONNECT: WebSocket断开
- SEND_MESSAGE: 发送消息
- RECEIVE_MESSAGE: 接收消息
- AUTO_DELIVERY: 自动发货
- AUTO_REPLY: 自动回复
- CONFIRM_SHIPMENT: 确认收货
- TOKEN_REFRESH: Token刷新
- COOKIE_UPDATE: Cookie更新

## 前端页面

访问 `/operation-log` 查看操作日志，支持：
- 按账号筛选
- 按操作类型筛选
- 按操作模块筛选
- 按操作状态筛选
- 分页查看
- 查看详情
- 删除旧日志
