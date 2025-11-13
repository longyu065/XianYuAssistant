@echo off
chcp 65001 >nul
echo ========================================
echo 重新启动WebSocket连接
echo ========================================
echo.

echo 步骤1：停止现有连接
curl -X POST http://localhost:8080/api/websocket/stop ^
  -H "Content-Type: application/json" ^
  -d "{\"xianyuAccountId\": 1}"
echo.
echo.

echo 等待2秒...
timeout /t 2 /nobreak >nul
echo.

echo 步骤2：启动新连接（自动刷新Token）
curl -X POST http://localhost:8080/api/websocket/start ^
  -H "Content-Type: application/json" ^
  -d "{\"xianyuAccountId\": 1}"
echo.
echo.

echo 等待3秒...
timeout /t 3 /nobreak >nul
echo.

echo 步骤3：检查连接状态
curl -X POST http://localhost:8080/api/websocket/status ^
  -H "Content-Type: application/json" ^
  -d "{\"xianyuAccountId\": 1}"
echo.
echo.

echo ========================================
echo 完成
echo ========================================
echo.
echo 如果仍然失败，可能需要：
echo 1. 检查Cookie是否有效
echo 2. 重新扫码登录
echo 3. 查看应用日志
echo.
pause
