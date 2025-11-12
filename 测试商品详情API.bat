@echo off
chcp 65001 >nul
echo ========================================
echo 测试商品详情API
echo ========================================
echo.

REM 设置变量
set BASE_URL=http://localhost:8080
set COOKIE_ID=default
set XY_GOOD_ID=

REM 提示用户输入商品ID
set /p XY_GOOD_ID=请输入商品ID (xy_good_id): 

if "%XY_GOOD_ID%"=="" (
    echo 错误：商品ID不能为空！
    pause
    exit /b 1
)

echo.
echo ========================================
echo 测试1: 获取商品详情（不更新详情信息）
echo ========================================
echo.

curl -X POST "%BASE_URL%/api/items/detail" ^
  -H "Content-Type: application/json" ^
  -d "{\"xyGoodId\":\"%XY_GOOD_ID%\"}"

echo.
echo.
echo ========================================
echo 测试2: 获取商品详情（同时更新详情信息）
echo ========================================
echo.

curl -X POST "%BASE_URL%/api/items/detail" ^
  -H "Content-Type: application/json" ^
  -d "{\"xyGoodId\":\"%XY_GOOD_ID%\",\"cookieId\":\"%COOKIE_ID%\"}"

echo.
echo.
echo ========================================
echo 测试完成
echo ========================================
pause
