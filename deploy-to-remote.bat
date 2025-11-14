@echo off
setlocal enabledelayedexpansion

echo ========================================
echo 闲鱼助手 - 远程Docker部署脚本
echo ========================================
echo.

REM ===== 配置区域 - 请修改为你的服务器信息 =====
set REMOTE_HOST=your-server-ip
set REMOTE_USER=root
set REMOTE_PORT=22
set REMOTE_PATH=/opt/xianyu-assistant
set SSH_KEY=
REM 如果使用SSH密钥，设置为: -i "C:\path\to\your\key.pem"
REM ============================================

echo [配置信息]
echo 远程主机: %REMOTE_HOST%
echo 远程用户: %REMOTE_USER%
echo 远程路径: %REMOTE_PATH%
echo.

REM 检查是否配置了服务器信息
if "%REMOTE_HOST%"=="your-server-ip" (
    echo ❌ 错误: 请先在脚本中配置远程服务器信息！
    echo.
    echo 请编辑 deploy-to-remote.bat 文件，修改以下变量:
    echo   - REMOTE_HOST: 你的服务器IP地址
    echo   - REMOTE_USER: SSH用户名
    echo   - REMOTE_PORT: SSH端口 ^(默认22^)
    echo   - REMOTE_PATH: 部署目录路径
    echo   - SSH_KEY: SSH密钥路径 ^(可选^)
    echo.
    pause
    exit /b 1
)

echo [1/6] 检查本地环境...
where docker >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ 错误: 未找到 Docker，请先安装 Docker Desktop
    pause
    exit /b 1
)

where ssh >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ 错误: 未找到 SSH 客户端
    echo 请安装 OpenSSH 客户端或使用 Git Bash
    pause
    exit /b 1
)
echo ✅ 环境检查通过

echo.
echo [2/6] 测试SSH连接...
ssh %SSH_KEY% -p %REMOTE_PORT% -o ConnectTimeout=5 %REMOTE_USER%@%REMOTE_HOST% "echo '连接成功'" >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ 错误: 无法连接到远程服务器
    echo 请检查:
    echo   1. 服务器IP地址是否正确
    echo   2. SSH端口是否正确
    echo   3. 用户名和密码/密钥是否正确
    echo   4. 服务器防火墙是否开放SSH端口
    pause
    exit /b 1
)
echo ✅ SSH连接成功

echo.
echo [3/6] 创建部署包...
if exist deploy-package.tar.gz del deploy-package.tar.gz

REM 使用tar命令创建压缩包（Windows 10 1803+自带tar命令）
tar -czf deploy-package.tar.gz ^
    --exclude=node_modules ^
    --exclude=target ^
    --exclude=dist ^
    --exclude=.git ^
    --exclude=data ^
    --exclude=logs ^
    --exclude=*.log ^
    Dockerfile ^
    docker-compose.yml ^
    .dockerignore ^
    pom.xml ^
    src ^
    vue-code

if %errorlevel% neq 0 (
    echo ❌ 错误: 创建部署包失败
    pause
    exit /b 1
)
echo ✅ 部署包创建成功

echo.
echo [4/6] 上传文件到远程服务器...
REM 创建远程目录
ssh %SSH_KEY% -p %REMOTE_PORT% %REMOTE_USER%@%REMOTE_HOST% "mkdir -p %REMOTE_PATH%"

REM 上传部署包
scp %SSH_KEY% -P %REMOTE_PORT% deploy-package.tar.gz %REMOTE_USER%@%REMOTE_HOST%:%REMOTE_PATH%/
if %errorlevel% neq 0 (
    echo ❌ 错误: 文件上传失败
    pause
    exit /b 1
)
echo ✅ 文件上传成功

echo.
echo [5/6] 在远程服务器上解压和构建...
ssh %SSH_KEY% -p %REMOTE_PORT% %REMOTE_USER%@%REMOTE_HOST% "cd %REMOTE_PATH% && tar -xzf deploy-package.tar.gz && rm deploy-package.tar.gz"
if %errorlevel% neq 0 (
    echo ❌ 错误: 解压失败
    pause
    exit /b 1
)
echo ✅ 文件解压成功

echo.
echo [6/6] 构建并启动Docker容器...
ssh %SSH_KEY% -p %REMOTE_PORT% %REMOTE_USER%@%REMOTE_HOST% "cd %REMOTE_PATH% && docker-compose down && docker-compose build && docker-compose up -d"
if %errorlevel% neq 0 (
    echo ❌ 错误: Docker部署失败
    pause
    exit /b 1
)

echo.
echo ========================================
echo ✅ 部署成功！
echo ========================================
echo.
echo 应用信息:
echo   - 访问地址: http://%REMOTE_HOST%:8080
echo   - 容器名称: xianyu-assistant
echo.
echo 常用命令:
echo   查看日志: ssh %REMOTE_USER%@%REMOTE_HOST% "cd %REMOTE_PATH% && docker-compose logs -f"
echo   停止服务: ssh %REMOTE_USER%@%REMOTE_HOST% "cd %REMOTE_PATH% && docker-compose down"
echo   重启服务: ssh %REMOTE_USER%@%REMOTE_HOST% "cd %REMOTE_PATH% && docker-compose restart"
echo   查看状态: ssh %REMOTE_USER%@%REMOTE_HOST% "cd %REMOTE_PATH% && docker-compose ps"
echo.

REM 清理本地临时文件
if exist deploy-package.tar.gz del deploy-package.tar.gz

pause
