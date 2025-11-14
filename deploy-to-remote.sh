#!/bin/bash

# 闲鱼助手 - 远程Docker部署脚本 (Linux/Mac)

set -e

echo "========================================"
echo "闲鱼助手 - 远程Docker部署脚本"
echo "========================================"
echo ""

# ===== 配置区域 - 请修改为你的服务器信息 =====
REMOTE_HOST="your-server-ip"
REMOTE_USER="root"
REMOTE_PORT="22"
REMOTE_PATH="/opt/xianyu-assistant"
SSH_KEY=""
# 如果使用SSH密钥，设置为: -i /path/to/your/key.pem
# ============================================

echo "[配置信息]"
echo "远程主机: $REMOTE_HOST"
echo "远程用户: $REMOTE_USER"
echo "远程路径: $REMOTE_PATH"
echo ""

# 检查是否配置了服务器信息
if [ "$REMOTE_HOST" = "your-server-ip" ]; then
    echo "❌ 错误: 请先在脚本中配置远程服务器信息！"
    echo ""
    echo "请编辑 deploy-to-remote.sh 文件，修改以下变量:"
    echo "  - REMOTE_HOST: 你的服务器IP地址"
    echo "  - REMOTE_USER: SSH用户名"
    echo "  - REMOTE_PORT: SSH端口 (默认22)"
    echo "  - REMOTE_PATH: 部署目录路径"
    echo "  - SSH_KEY: SSH密钥路径 (可选)"
    echo ""
    exit 1
fi

echo "[1/6] 检查本地环境..."
if ! command -v docker &> /dev/null; then
    echo "❌ 错误: 未找到 Docker，请先安装 Docker"
    exit 1
fi

if ! command -v ssh &> /dev/null; then
    echo "❌ 错误: 未找到 SSH 客户端"
    exit 1
fi
echo "✅ 环境检查通过"

echo ""
echo "[2/6] 测试SSH连接..."
if ! ssh $SSH_KEY -p $REMOTE_PORT -o ConnectTimeout=5 $REMOTE_USER@$REMOTE_HOST "echo '连接成功'" &> /dev/null; then
    echo "❌ 错误: 无法连接到远程服务器"
    echo "请检查:"
    echo "  1. 服务器IP地址是否正确"
    echo "  2. SSH端口是否正确"
    echo "  3. 用户名和密码/密钥是否正确"
    echo "  4. 服务器防火墙是否开放SSH端口"
    exit 1
fi
echo "✅ SSH连接成功"

echo ""
echo "[3/6] 创建部署包..."
rm -f deploy-package.tar.gz

tar -czf deploy-package.tar.gz \
    --exclude=node_modules \
    --exclude=target \
    --exclude=dist \
    --exclude=.git \
    --exclude=data \
    --exclude=logs \
    --exclude='*.log' \
    Dockerfile \
    docker-compose.yml \
    .dockerignore \
    pom.xml \
    src \
    vue-code

echo "✅ 部署包创建成功"

echo ""
echo "[4/6] 上传文件到远程服务器..."
# 创建远程目录
ssh $SSH_KEY -p $REMOTE_PORT $REMOTE_USER@$REMOTE_HOST "mkdir -p $REMOTE_PATH"

# 上传部署包
scp $SSH_KEY -P $REMOTE_PORT deploy-package.tar.gz $REMOTE_USER@$REMOTE_HOST:$REMOTE_PATH/
echo "✅ 文件上传成功"

echo ""
echo "[5/6] 在远程服务器上解压和构建..."
ssh $SSH_KEY -p $REMOTE_PORT $REMOTE_USER@$REMOTE_HOST "cd $REMOTE_PATH && tar -xzf deploy-package.tar.gz && rm deploy-package.tar.gz"
echo "✅ 文件解压成功"

echo ""
echo "[6/6] 构建并启动Docker容器..."
ssh $SSH_KEY -p $REMOTE_PORT $REMOTE_USER@$REMOTE_HOST "cd $REMOTE_PATH && docker-compose down && docker-compose build && docker-compose up -d"

echo ""
echo "========================================"
echo "✅ 部署成功！"
echo "========================================"
echo ""
echo "应用信息:"
echo "  - 访问地址: http://$REMOTE_HOST:8080"
echo "  - 容器名称: xianyu-assistant"
echo ""
echo "常用命令:"
echo "  查看日志: ssh $REMOTE_USER@$REMOTE_HOST \"cd $REMOTE_PATH && docker-compose logs -f\""
echo "  停止服务: ssh $REMOTE_USER@$REMOTE_HOST \"cd $REMOTE_PATH && docker-compose down\""
echo "  重启服务: ssh $REMOTE_USER@$REMOTE_HOST \"cd $REMOTE_PATH && docker-compose restart\""
echo "  查看状态: ssh $REMOTE_USER@$REMOTE_HOST \"cd $REMOTE_PATH && docker-compose ps\""
echo ""

# 清理本地临时文件
rm -f deploy-package.tar.gz
