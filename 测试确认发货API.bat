@echo off
chcp 65001 >nul
echo ========================================
echo 测试确认发货功能
echo ========================================
echo.

echo 请输入订单ID（例如：1234567890）：
set /p ORDER_ID=

if "%ORDER_ID%"=="" (
    echo 错误：订单ID不能为空
    pause
    exit /b
)

echo.
echo 正在确认发货，订单ID: %ORDER_ID%
echo.

curl -X POST http://localhost:8080/api/order/confirmShipment ^
  -H "Content-Type: application/json" ^
  -d "{\"xianyuAccountId\": 1, \"orderId\": \"%ORDER_ID%\"}"

echo.
echo.
echo ========================================
echo 测试完成
echo ========================================
echo.
pause
