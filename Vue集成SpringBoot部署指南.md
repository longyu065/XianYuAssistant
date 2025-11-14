# Vue 项目集成到 Spring Boot 部署指南

本文档详细说明如何将 Vue 前端项目打包并集成到 Spring Boot 后端项目中，实现前后端一体化部署。

## 一、项目结构

```
项目根目录/
├── vue-code/                    # Vue 前端项目
│   ├── src/
│   ├── vite.config.ts          # Vite 配置文件（关键）
│   ├── package.json
│   └── build-to-spring.bat     # 构建脚本
├── src/
│   └── main/
│       ├── java/
│       │   └── config/
│       │       └── WebMvcConfig.java  # Spring MVC 配置（关键）
│       └── resources/
│           ├── static/          # Vue 构建产物输出目录
│           └── application.properties  # Spring Boot 配置
└── pom.xml
```

## 二、关键配置

### 1. Vue 项目配置 (vite.config.ts)

```typescript
import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  base: '/',  // 重要：设置为根路径
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  },
  build: {
    outDir: '../src/main/resources/static',  // 关键：输出到 Spring Boot 的 static 目录
    assetsDir: 'assets',                     // 静态资源目录
    sourcemap: false,
    emptyOutDir: true,                       // 构建前清空输出目录
    rollupOptions: {
      output: {
        manualChunks: undefined
      }
    }
  }
})
```

**关键点说明：**
- `base: '/'`：确保资源路径从根路径开始
- `outDir: '../src/main/resources/static'`：将构建产物直接输出到 Spring Boot 的静态资源目录
- `emptyOutDir: true`：每次构建前清空目录，避免旧文件残留

### 2. Spring Boot 静态资源配置 (application.properties)

```properties
server.port=8080

# 静态资源配置
spring.web.resources.static-locations=classpath:/static/
spring.web.resources.add-mappings=true

# 确保正确的 MIME 类型
spring.mvc.contentnegotiation.favor-path-extension=false
```

### 3. Spring MVC 配置 (WebMvcConfig.java)

```java
package com.feijimiao.xianyuassistant.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

/**
 * Web MVC 配置
 * 支持 Vue Router 的 History 模式
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        // 尝试获取请求的资源
                        Resource requestedResource = location.createRelative(resourcePath);
                        
                        // 如果资源存在且可读，直接返回（静态文件、API等）
                        if (requestedResource.exists() && requestedResource.isReadable()) {
                            return requestedResource;
                        }
                        
                        // 如果是 API 请求，返回 null 让 Controller 处理
                        if (resourcePath.startsWith("api/")) {
                            return null;
                        }
                        
                        // 其他情况返回 index.html，让 Vue Router 处理
                        return new ClassPathResource("/static/index.html");
                    }
                });
    }
}
```

**关键点说明：**
- 拦截所有请求 `/**`
- 优先返回存在的静态资源（JS、CSS、图片等）
- API 请求交给 Spring Controller 处理
- 其他请求返回 `index.html`，支持 Vue Router 的 History 模式

### 4. Vue 前端 API 请求配置 (request.ts)

```typescript
import axios from 'axios'

const service = axios.create({
  baseURL: '/api',  // 关键：API 请求前缀
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 响应拦截器
service.interceptors.response.use(
  (response) => {
    const res = response.data
    
    // 统一处理响应码
    if (res.code !== 0 && res.code !== 200) {
      return Promise.reject(new Error(res.msg || '请求失败'))
    }
    
    return response
  },
  (error) => {
    return Promise.reject(error)
  }
)

export function request(config) {
  return service.request(config).then(response => response.data)
}
```

**关键点说明：**
- `baseURL: '/api'`：所有 API 请求都会加上 `/api` 前缀
- 响应拦截器统一处理后端返回的数据格式

### 5. Spring Boot Controller 配置

```java
@RestController
@RequestMapping("/api/account")  // 关键：API 路径以 /api 开头
@CrossOrigin(origins = "*")
public class AccountController {
    
    @PostMapping("/list")
    public ResultObject<GetAccountListRespDTO> getAccountList() {
        // 返回统一格式
        return ResultObject.success(data);
    }
}
```

**关键点说明：**
- Controller 的 `@RequestMapping` 必须以 `/api` 开头
- 返回统一的 JSON 格式：`{ code: 200, msg: "成功", data: {...} }`

## 三、构建脚本 (build-to-spring.bat)

```batch
@echo off
echo ========================================
echo 正在构建 Vue 项目并部署到 Spring Boot...
echo ========================================

echo.
echo [1/3] 清理旧的构建文件...
if exist "..\src\main\resources\static" (
    rmdir /s /q "..\src\main\resources\static"
    echo 已清理旧文件
)

echo.
echo [2/3] 构建 Vue 项目...
call npm run build

if %errorlevel% neq 0 (
    echo.
    echo ❌ 构建失败！
    pause
    exit /b %errorlevel%
)

echo.
echo [3/3] 验证构建结果...
if exist "..\src\main\resources\static\index.html" (
    echo ✅ 构建成功！
    echo.
    echo 文件已部署到: src/main/resources/static/
    echo.
    echo 现在可以启动 Spring Boot 应用，访问 http://localhost:8080
) else (
    echo ❌ 构建文件未找到！
)

echo.
echo ========================================
echo 构建完成
echo ========================================
pause
```

## 四、部署流程

### 开发环境

1. **前端开发**（独立运行）
   ```bash
   cd vue-code
   npm run dev
   # 访问 http://localhost:5173
   ```

2. **后端开发**（独立运行）
   ```bash
   # 启动 Spring Boot
   # 访问 http://localhost:8080/api/...
   ```

### 生产环境部署

1. **构建 Vue 项目**
   ```bash
   cd vue-code
   npm run build
   # 或运行 build-to-spring.bat
   ```

2. **打包 Spring Boot**
   ```bash
   mvn clean package -DskipTests
   ```

3. **运行**
   ```bash
   java -jar target/XianYuAssistant-0.0.1-SNAPSHOT.jar
   # 访问 http://localhost:8080
   ```

## 五、常见问题

### 1. 404 错误：找不到静态资源

**原因：** Vue 项目未构建或构建输出路径不正确

**解决：**
- 检查 `vite.config.ts` 中的 `outDir` 配置
- 确认 `src/main/resources/static/index.html` 文件存在
- 重新运行 `npm run build`

### 2. API 请求 404

**原因：** API 路径配置不匹配

**解决：**
- 确认 Controller 的 `@RequestMapping` 以 `/api` 开头
- 检查前端 `baseURL: '/api'` 配置
- 查看浏览器 Network 面板确认实际请求路径

### 3. Vue Router 刷新页面 404

**原因：** Spring Boot 未正确配置 SPA 路由支持

**解决：**
- 确认 `WebMvcConfig.java` 配置正确
- 确保非静态资源和非 API 请求都返回 `index.html`

### 4. 静态资源缓存问题

**原因：** 浏览器缓存了旧的静态文件

**解决：**
- 清空浏览器缓存
- 使用 Ctrl+F5 强制刷新
- Vite 构建时会自动添加 hash 值到文件名

### 5. CORS 跨域问题

**原因：** 开发环境前后端分离时的跨域问题

**解决：**
- 开发环境：使用 Vite 的 proxy 配置
- 生产环境：前后端同域，无跨域问题
- 或在 Controller 添加 `@CrossOrigin` 注解

## 六、最佳实践

1. **开发时前后端分离**
   - 前端使用 `npm run dev` 独立运行
   - 通过 Vite proxy 代理 API 请求到后端

2. **生产环境前后端一体**
   - 构建 Vue 项目到 Spring Boot static 目录
   - 打包成单个 JAR 文件部署

3. **API 路径规范**
   - 所有后端 API 统一使用 `/api` 前缀
   - 避免与前端路由冲突

4. **版本控制**
   - `.gitignore` 中排除 `src/main/resources/static/`
   - 每次部署前重新构建 Vue 项目

5. **自动化构建**
   - 使用构建脚本自动化流程
   - CI/CD 中集成前端构建步骤

## 七、总结

通过以上配置，实现了：
- ✅ Vue 前端和 Spring Boot 后端一体化部署
- ✅ 支持 Vue Router History 模式
- ✅ API 请求正确路由到后端 Controller
- ✅ 静态资源正确加载
- ✅ 开发和生产环境分离

核心要点：
1. Vite 构建输出到 Spring Boot 的 static 目录
2. WebMvcConfig 正确处理 SPA 路由
3. API 请求使用统一前缀 `/api`
4. 响应数据格式统一
