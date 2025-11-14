# Docker 部署指南

## 快速开始

### 方式一：自动化远程部署（推荐）

1. **配置部署脚本**

编辑 `deploy-to-remote.bat` (Windows) 或 `deploy-to-remote.sh` (Linux/Mac)，修改服务器配置：

```bash
REMOTE_HOST="your-server-ip"      # 服务器IP地址
REMOTE_USER="root"                 # SSH用户名
REMOTE_PORT="22"                   # SSH端口
REMOTE_PATH="/opt/xianyu-assistant" # 部署目录
SSH_KEY=""                         # SSH密钥路径（可选）
```

2. **执行部署**

**Windows:**
```bash
deploy-to-remote.bat
```

**Linux/Mac:**
```bash
chmod +x deploy-to-remote.sh
./deploy-to-remote.sh
```

脚本会自动完成：
- ✅ 检查本地环境
- ✅ 测试SSH连接
- ✅ 打包项目文件
- ✅ 上传到远程服务器
- ✅ 构建Docker镜像
- ✅ 启动容器

3. **访问应用**

打开浏览器访问：`http://your-server-ip:8080`

### 方式二：本地Docker构建

1. **构建镜像**
```bash
docker-compose build
```

2. **启动容器**
```bash
docker-compose up -d
```

3. **访问应用**
```bash
http://localhost:8080
```

## 部署架构

### 多阶段构建

Dockerfile 使用三阶段构建，优化镜像大小：

```
阶段1: 前端构建 (Node.js)
  ├─ 安装依赖
  ├─ 构建Vue项目
  └─ 输出静态文件

阶段2: 后端构建 (Maven)
  ├─ 下载依赖
  ├─ 复制前端静态文件
  └─ 构建Spring Boot JAR

阶段3: 运行时 (JRE)
  ├─ 复制JAR文件
  └─ 启动应用
```

### 目录结构

```
/opt/xianyu-assistant/          # 远程服务器部署目录
├── Dockerfile                  # Docker构建文件
├── docker-compose.yml          # Docker Compose配置
├── .dockerignore              # Docker忽略文件
├── pom.xml                    # Maven配置
├── src/                       # 后端源码
├── vue-code/                  # 前端源码
├── data/                      # 数据持久化目录（自动创建）
└── logs/                      # 日志目录（自动创建）
```

## 配置说明

### Docker Compose 配置

```yaml
services:
  xianyu-assistant:
    ports:
      - "8080:8080"           # 端口映射
    volumes:
      - ./data:/app/data      # 数据持久化
      - ./logs:/app/logs      # 日志持久化
    environment:
      - JAVA_OPTS=-Xms256m -Xmx512m  # JVM参数
      - TZ=Asia/Shanghai               # 时区
    restart: unless-stopped            # 自动重启
```

### 环境变量

| 变量 | 说明 | 默认值 |
|------|------|--------|
| JAVA_OPTS | JVM参数 | -Xms256m -Xmx512m |
| TZ | 时区 | Asia/Shanghai |
| SPRING_PROFILES_ACTIVE | Spring配置文件 | prod |

## 常用命令

### 容器管理

```bash
# 启动容器
docker-compose up -d

# 停止容器
docker-compose down

# 重启容器
docker-compose restart

# 查看状态
docker-compose ps

# 查看日志
docker-compose logs -f

# 查看实时日志（最近100行）
docker-compose logs -f --tail=100
```

### 远程服务器操作

```bash
# SSH连接到服务器
ssh user@your-server-ip

# 进入部署目录
cd /opt/xianyu-assistant

# 查看容器状态
docker-compose ps

# 查看日志
docker-compose logs -f

# 重启服务
docker-compose restart

# 更新部署（重新运行部署脚本）
# 在本地执行: deploy-to-remote.bat
```

### 数据备份

```bash
# 备份数据库
docker-compose exec xianyu-assistant tar -czf /app/backup.tar.gz /app/data

# 复制备份到本地
docker cp xianyu-assistant:/app/backup.tar.gz ./backup.tar.gz

# 或使用SCP从远程服务器下载
scp user@your-server-ip:/opt/xianyu-assistant/data/*.db ./backup/
```

## 远程服务器要求

### 系统要求

- **操作系统**: Linux (推荐 Ubuntu 20.04+, CentOS 7+)
- **内存**: 最低 1GB，推荐 2GB+
- **磁盘**: 最低 5GB 可用空间
- **网络**: 开放 8080 端口

### 软件要求

```bash
# 安装 Docker
curl -fsSL https://get.docker.com | sh

# 安装 Docker Compose
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose

# 启动 Docker 服务
sudo systemctl start docker
sudo systemctl enable docker

# 验证安装
docker --version
docker-compose --version
```

### 防火墙配置

```bash
# Ubuntu/Debian (UFW)
sudo ufw allow 22/tcp    # SSH
sudo ufw allow 8080/tcp  # 应用端口
sudo ufw enable

# CentOS/RHEL (firewalld)
sudo firewall-cmd --permanent --add-port=22/tcp
sudo firewall-cmd --permanent --add-port=8080/tcp
sudo firewall-cmd --reload
```

## SSH 配置

### 使用密码认证

脚本会提示输入密码，无需额外配置。

### 使用密钥认证（推荐）

1. **生成SSH密钥**（如果还没有）

```bash
# Windows (Git Bash)
ssh-keygen -t rsa -b 4096 -C "your_email@example.com"

# Linux/Mac
ssh-keygen -t rsa -b 4096 -C "your_email@example.com"
```

2. **复制公钥到服务器**

```bash
ssh-copy-id -i ~/.ssh/id_rsa.pub user@your-server-ip
```

3. **配置部署脚本**

```bash
# Windows
SSH_KEY=-i "C:\Users\YourName\.ssh\id_rsa"

# Linux/Mac
SSH_KEY="-i ~/.ssh/id_rsa"
```

## 故障排查

### 问题：无法连接到远程服务器

**检查项**：
1. 服务器IP地址是否正确
2. SSH端口是否正确（默认22）
3. 防火墙是否开放SSH端口
4. SSH服务是否运行：`sudo systemctl status sshd`

### 问题：Docker构建失败

**解决方案**：
```bash
# 查看详细日志
docker-compose build --no-cache

# 检查磁盘空间
df -h

# 清理Docker缓存
docker system prune -a
```

### 问题：容器启动失败

**解决方案**：
```bash
# 查看容器日志
docker-compose logs

# 检查端口占用
netstat -tlnp | grep 8080

# 重新构建并启动
docker-compose down
docker-compose up -d --build
```

### 问题：无法访问应用

**检查项**：
1. 容器是否运行：`docker-compose ps`
2. 端口是否开放：`telnet your-server-ip 8080`
3. 防火墙配置是否正确
4. 查看应用日志：`docker-compose logs -f`

## 性能优化

### JVM 参数调优

根据服务器内存调整 `docker-compose.yml` 中的 JAVA_OPTS：

```yaml
# 1GB 内存服务器
JAVA_OPTS: "-Xms256m -Xmx512m"

# 2GB 内存服务器
JAVA_OPTS: "-Xms512m -Xmx1024m"

# 4GB+ 内存服务器
JAVA_OPTS: "-Xms1024m -Xmx2048m"
```

### 镜像优化

```bash
# 查看镜像大小
docker images | grep xianyu

# 清理未使用的镜像
docker image prune -a
```

## 更新部署

### 更新应用

1. 修改代码后，重新运行部署脚本：
```bash
deploy-to-remote.bat
```

2. 或手动更新：
```bash
# 上传新代码
scp -r ./src user@your-server-ip:/opt/xianyu-assistant/

# SSH到服务器
ssh user@your-server-ip

# 重新构建
cd /opt/xianyu-assistant
docker-compose down
docker-compose build
docker-compose up -d
```

## 监控和日志

### 查看实时日志

```bash
# 所有日志
docker-compose logs -f

# 最近100行
docker-compose logs -f --tail=100

# 特定时间段
docker-compose logs --since 30m
```

### 容器资源监控

```bash
# 查看资源使用
docker stats xianyu-assistant

# 查看容器详情
docker inspect xianyu-assistant
```

## 安全建议

1. **使用非root用户运行**
2. **配置SSH密钥认证，禁用密码登录**
3. **定期更新系统和Docker**
4. **配置防火墙，只开放必要端口**
5. **定期备份数据**
6. **使用HTTPS（配置Nginx反向代理）**

## 生产环境建议

### 使用 Nginx 反向代理

```nginx
server {
    listen 80;
    server_name your-domain.com;

    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

### 配置 HTTPS

```bash
# 使用 Let's Encrypt
sudo apt install certbot python3-certbot-nginx
sudo certbot --nginx -d your-domain.com
```

## 支持

如有问题，请查看：
- [项目 README](README.md)
- [部署指南](DEPLOYMENT_GUIDE.md)
- [GitHub Issues](https://github.com/your-repo/issues)
