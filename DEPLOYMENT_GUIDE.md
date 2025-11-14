# 闲鱼助手 - 部署指南

## 快速开始

### 方式一：一体化部署（生产环境推荐）

1. **构建前端项目**
   ```bash
   cd vue-code
   npm install
   npm run build:spring
   ```

2. **启动 Spring Boot**
   ```bash
   cd ..
   mvn spring-boot:run
   ```

3. **访问应用**
   
   打开浏览器访问：`http://localhost:8080`

### 方式二：分离开发（开发环境推荐）

1. **启动后端服务**
   ```bash
   mvn spring-boot:run
   ```
   后端运行在：`http://localhost:8080`

2. **启动前端开发服务器**
   ```bash
   cd vue-code
   npm install
   npm run dev
   ```
   前端运行在：`http://localhost:5173`

3. **访问应用**
   
   打开浏览器访问：`http://localhost:5173`

## 项目结构

```
XianYuAssistant/
├── src/main/                    # Spring Boot 后端
│   ├── java/                    # Java 源代码
│   └── resources/
│       ├── static/              # 前端构建输出目录（自动生成）
│       └── application.yml      # 后端配置
├── vue-code/                    # Vue 3 前端
│   ├── src/                     # 前端源代码
│   ├── vite.config.ts          # Vite 配置
│   └── package.json            # 前端依赖
└── pom.xml                     # Maven 配置
```

## 构建说明

### 前端构建

Vue 项目配置为构建到 `src/main/resources/static/` 目录：

```typescript
// vite.config.ts
build: {
  outDir: '../src/main/resources/static',
  emptyOutDir: true
}
```

### 自动化构建脚本

**Windows:**
```bash
cd vue-code
build-to-spring.bat
```

**Linux/Mac:**
```bash
cd vue-code
chmod +x build-to-spring.sh
./build-to-spring.sh
```

## API 配置

### 开发环境

前端开发服务器配置了代理，自动转发 API 请求到后端：

```typescript
// vite.config.ts
server: {
  proxy: {
    '/api': {
      target: 'http://localhost:8080',
      changeOrigin: true
    }
  }
}
```

### 生产环境

前端构建后与后端同源，直接访问 `/api/*` 路径。

## 路由配置

### Vue Router

使用 History 模式：

```typescript
const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [...]
})
```

### Spring Boot

配置了 `WebMvcConfig` 支持 SPA 路由，所有非 API 请求都返回 `index.html`。

## 端口配置

- **后端端口**: 8080（可在 `application.yml` 中修改）
- **前端开发端口**: 5173（可在 `vite.config.ts` 中修改）

## 常见问题

### Q: 构建后访问页面显示 404

**A:** 确保：
1. `src/main/resources/static/index.html` 文件存在
2. Spring Boot 应用已重启
3. 访问的是 `http://localhost:8080` 而不是其他端口

### Q: API 请求失败

**A:** 检查：
1. 后端服务是否正常运行
2. API 路径是否正确（应该以 `/api` 开头）
3. 浏览器控制台的网络请求详情

### Q: 页面刷新后 404

**A:** 确保 `WebMvcConfig` 配置类已生效，它会将所有路由请求转发到 `index.html`。

### Q: 开发模式下 API 请求跨域

**A:** 
- 开发模式使用 Vite 代理，不会有跨域问题
- 如果仍有问题，检查后端 CORS 配置

## 生产部署

### 打包 JAR

```bash
# 1. 构建前端
cd vue-code
npm run build:spring

# 2. 打包后端（包含前端静态文件）
cd ..
mvn clean package

# 3. 运行 JAR
java -jar target/xianyu-assistant-*.jar
```

### Docker 部署

```dockerfile
FROM openjdk:17-jdk-slim
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

## 技术栈

### 前端
- Vue 3
- TypeScript
- Element Plus
- Vite
- Vue Router
- Pinia

### 后端
- Spring Boot
- MyBatis Plus
- WebSocket
- SQLite

## 更多文档

- [Vue 项目 README](vue-code/README.md)
- [快速开始指南](vue-code/QUICK_START.md)
- [部署详细说明](vue-code/DEPLOYMENT.md)
