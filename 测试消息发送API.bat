@echo off
chcp 65001 >nul
echo ========================================
echo 测试WebSocket消息发送功能
echo ========================================
echo.

echo 1. 检查WebSocket连接状态
curl -X POST http://localhost:8080/api/websocket/status ^
  -H "Content-Type: application/json" ^
  -d "{\"xianyuAccountId\": 1}"
echo.
echo.

echo 2. 启动WebSocket连接（如果未连接）
curl -X POST http://localhost:8080/api/websocket/start ^
  -H "Content-Type: application/json" ^
  -d "{\"xianyuAccountId\": 1}"
echo.
echo.

echo 等待3秒...
timeout /t 3 /nobreak >nul
echo.

echo 3. 发送测试消息
echo 请根据实际情况修改cid和toId参数
curl -X POST http://localhost:8080/api/websocket/sendMessage ^
  -H "Content-Type: application/json" ^
  -d "{\"xianyuAccountId\": 1, \"cid\": \"55435931514\", \"toId\": \"3553532632\", \"text\": \"你好，这是一条测试消息\"}"
echo.
echo.

echo ========================================
echo 测试完成
echo ========================================
echo.
echo 提示：
echo 1. 请确保已经启动了WebSocket连接
echo 2. 请将cid和toId替换为实际的会话ID和用户ID
echo 3. 可以从数据库xianyu_chat_message表中查询获取
echo.
pause
