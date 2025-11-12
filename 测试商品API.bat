@echo off
chcp 65001 >nul
echo ========================================
echo 闲鱼商品API测试脚本
echo ========================================
echo.

set BASE_URL=http://localhost:8080/api/items
set COOKIE_ID=default

echo 请选择测试操作：
echo 1. 获取第1页商品（20条）
echo 2. 获取所有商品
echo 3. 从数据库获取商品
echo 4. 打开测试导航页面
echo 5. 打开商品管理页面
echo.

set /p choice=请输入选项 (1-5): 

if "%choice%"=="1" goto test1
if "%choice%"=="2" goto test2
if "%choice%"=="3" goto test3
if "%choice%"=="4" goto test4
if "%choice%"=="5" goto test5
goto end

:test1
echo.
echo 正在获取第1页商品...
curl -X POST "%BASE_URL%/list" -H "Content-Type: application/json" -d "{\"cookieId\":\"%COOKIE_ID%\",\"pageNumber\":1,\"pageSize\":20}"
goto end

:test2
echo.
echo 正在获取所有商品（最多5页）...
curl -X POST "%BASE_URL%/all" -H "Content-Type: application/json" -d "{\"cookieId\":\"%COOKIE_ID%\",\"pageSize\":20,\"maxPages\":5}"
goto end

:test3
echo.
echo 正在从数据库获取商品...
curl -X POST "%BASE_URL%/db" -H "Content-Type: application/json" -d "{\"cookieId\":\"%COOKIE_ID%\"}"
goto end

:test4
echo.
echo 正在打开测试导航页面...
start http://localhost:8080/index.html
goto end

:test5
echo.
echo 正在打开商品管理页面...
start http://localhost:8080/items.html
goto end

:end
echo.
echo ========================================
echo 测试完成
echo ========================================
pause
