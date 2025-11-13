# 消息发送lwp结构说明

## 概述

官方的发送消息使用 `/r/MessageSend/sendByReceiverScope` 这个lwp路径，支持请求和响应两种消息格式。

## 请求格式

发送消息的请求结构：

```json
{
  "lwp": "/r/MessageSend/sendByReceiverScope",
  "headers": {
    "mid": "5681763000909053 0"
  },
  "body": [
    {
      "uuid": "-17630009090522",
      "cid": "50660220958@goofish",
      "conversationType": 1,
      "content": {
        "contentType": 101,
        "custom": {
          "type": 1,
          "data": "eyJjb250ZW50VHlwZSI6MSwidGV4dCI6eyJ0ZXh0Ijoi5aSa5aSnIn19"
        }
      },
      "redPointPolicy": 0,
      "extension": {
        "extJson": "{}"
      },
      "ctx": {
        "appVersion": "1.0",
        "platform": "web"
      },
      "mtags": {},
      "msgReadStatusSetting": 1
    },
    {
      "actualReceivers": [
        "2218021801256@goofish",
        "3553532632@goofish"
      ]
    }
  ]
}
```

### 字段说明

#### headers
- `mid`: 消息ID

#### body[0] - 消息内容
- `uuid`: 消息唯一标识（客户端生成）
- `cid`: 会话ID（格式：用户ID@goofish）
- `conversationType`: 会话类型（1=单聊）
- `content.contentType`: 内容类型（101=自定义消息）
- `content.custom.type`: 自定义类型（1=文本消息）
- `content.custom.data`: Base64编码的消息内容
  - 解码后格式：`{"contentType":1,"text":{"text":"消息文本"}}`
- `extension.extJson`: 扩展信息（JSON字符串）
- `ctx.appVersion`: 应用版本
- `ctx.platform`: 平台（web/ios/android）
- `redPointPolicy`: 红点策略
- `msgReadStatusSetting`: 消息已读状态设置

#### body[1] - 接收者信息
- `actualReceivers`: 实际接收者列表（用户ID@goofish格式）

## 响应格式

发送成功后的响应结构：

```json
{
  "lwp": "/r/MessageSend/sendByReceiverScope",
  "code": 200,
  "headers": {
    "mid": "5681763000909053 0",
    "sid": "session_id"
  },
  "body": {
    "messageId": "msg_123456",
    "uuid": "-17630009090522",
    "createAt": 1699999999999,
    "extension": {
      "reminderContent": "消息文本",
      "reminderTitle": "发送者昵称",
      "reminderUrl": "闲鱼://...",
      "senderUserId": "123456@goofish",
      "_appVersion": "1.0",
      "_platform": "web"
    },
    "content": {
      "contentType": 101,
      "custom": {
        "type": 1,
        "data": "eyJjb250ZW50VHlwZSI6MSwidGV4dCI6eyJ0ZXh0Ijoi5aSa5aSnIn19"
      }
    }
  }
}
```

### 响应字段说明

- `code`: 响应码（200=成功）
- `body.messageId`: 服务器生成的消息ID
- `body.createAt`: 消息创建时间戳
- `body.extension.reminderContent`: 消息内容（用于通知显示）
- `body.extension.reminderTitle`: 发送者昵称
- `body.extension.reminderUrl`: 消息跳转链接
- `body.extension.senderUserId`: 发送者用户ID

## 实现说明

### MessageSendHandler

`MessageSendHandler` 现在支持处理两种格式：

1. **请求格式**（body是List）
   - 记录发送请求日志
   - 提取消息文本、接收者等信息

2. **响应格式**（body是Map）
   - 检查响应码
   - 保存发送成功的消息到数据库

### 消息保存

只在收到成功响应（code=200）时保存消息到数据库：
- 使用 `messageId` 作为 `pnm_id`
- 从 `reminderUrl` 中提取会话ID（s_id）
- 保存消息内容、发送者信息等

## 使用示例

### 发送文本消息

```javascript
// 1. 准备消息内容
const messageContent = {
  contentType: 1,
  text: {
    text: "你好"
  }
};

// 2. Base64编码
const encodedData = btoa(JSON.stringify(messageContent));

// 3. 构造发送请求
const sendRequest = {
  lwp: "/r/MessageSend/sendByReceiverScope",
  headers: {
    mid: "your_mid"
  },
  body: [
    {
      uuid: "-" + Date.now(),
      cid: "receiver_id@goofish",
      conversationType: 1,
      content: {
        contentType: 101,
        custom: {
          type: 1,
          data: encodedData
        }
      },
      redPointPolicy: 0,
      extension: {
        extJson: "{}"
      },
      ctx: {
        appVersion: "1.0",
        platform: "web"
      },
      mtags: {},
      msgReadStatusSetting: 1
    },
    {
      actualReceivers: ["receiver_id@goofish"]
    }
  ]
};

// 4. 通过WebSocket发送
websocket.send(JSON.stringify(sendRequest));
```

## 注意事项

1. **UUID生成**：客户端需要生成唯一的uuid，通常使用负数时间戳
2. **CID格式**：会话ID格式为 `用户ID@goofish`
3. **接收者格式**：actualReceivers中的用户ID也需要 `@goofish` 后缀
4. **Base64编码**：custom.data需要Base64编码
5. **响应匹配**：响应中的uuid与请求中的uuid相同，可用于匹配请求和响应

## 相关文件

- `MessageSendHandler.java`: 消息发送处理器
- `ChatMessageServiceImpl.java`: 聊天消息服务
- `XianyuChatMessage.java`: 聊天消息实体
- `websocket-send.html`: 消息发送测试页面
