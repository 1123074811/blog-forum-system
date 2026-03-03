# 本地运行指南

## 环境要求

### 后端
- Java 17+
- Maven 3.6+
- MySQL 8.0+
- Redis 6.0+
- MinIO（可选，用于文件存储）

### 前端
- Node.js 16+
- npm 或 yarn

## 快速启动

### 1. 数据库准备

#### MySQL
```bash
# 创建数据库
mysql -u root -p
CREATE DATABASE blog_forum CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

导入数据库脚本（如果有 SQL 文件）或启动后端让 MyBatis Plus 自动创建表。

#### Redis
```bash
# 启动 Redis（Windows）
redis-server

# 或使用 WSL/Docker
docker run -d -p 6379:6379 --name redis redis:latest
```

### 2. MinIO（可选）

如果需要文件上传功能：

```bash
# 使用 Docker 启动 MinIO
docker run -d \
  -p 9000:9000 \
  -p 9001:9001 \
  --name minio \
  -e "MINIO_ROOT_USER=minioadmin" \
  -e "MINIO_ROOT_PASSWORD=minioadmin" \
  minio/minio server /data --console-address ":9001"
```

访问 http://localhost:9001 创建名为 `blog` 的 bucket。

### 3. 启动后端

```bash
cd blog-backend

# 使用 Maven 启动
mvn spring-boot:run

# 或使用 IDE（IDEA/Eclipse）直接运行主类
```

后端将在 http://localhost:8080 启动。

### 4. 启动前端

```bash
cd blog-frontend

# 安装依赖（首次运行）
npm install

# 启动开发服务器
npm run dev
```

前端将在 http://localhost:5173 启动。

## 配置说明

### 后端配置

- `.env` - 环境变量配置（已创建）
- `application.yml` - 主配置文件
- `application-dev.yml` - 开发环境配置

### 前端配置

- `.env.development` - 开发环境配置（已创建）
- `.env.production` - 生产环境配置
- `vite.config.js` - Vite 配置（包含代理设置）

## 常见问题

### 1. 数据库连接失败
- 检查 MySQL 是否启动
- 确认数据库名称、用户名、密码是否正确
- 检查 `application-dev.yml` 中的数据库配置

### 2. Redis 连接失败
- 检查 Redis 是否启动
- 确认 Redis 密码配置（如果设置了密码）

### 3. 前端无法访问后端 API
- 确认后端已启动在 8080 端口
- 检查 `vite.config.js` 中的代理配置
- 查看浏览器控制台的网络请求

### 4. MinIO 文件上传失败
- 确认 MinIO 已启动
- 检查 bucket 是否已创建
- 确认访问密钥配置正确

## 开发模式

前端开发服务器已配置代理，所有 `/api` 请求会自动转发到后端 `http://localhost:8080`。

## 注意事项

1. `.env` 文件包含敏感信息，不要提交到版本控制
2. 开发环境使用的是测试密钥，生产环境需要更换
3. 确保所有依赖服务（MySQL、Redis）都已启动
