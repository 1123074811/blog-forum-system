# 多环境配置使用指南

## 📁 配置文件结构

```
src/main/resources/
├── application.yml          # 主配置文件（公共配置）
├── application-dev.yml      # 开发环境配置
└── application-prod.yml     # 生产环境配置
```

## 🚀 使用方法

### 1. 本地开发环境

默认使用 `dev` 配置，直接运行即可：

```bash
# IDEA 中直接运行
# 或使用 Maven
mvn spring-boot:run
```

### 2. 切换到生产环境

#### 方法一：启动参数
```bash
java -jar blog-backend.jar --spring.profiles.active=prod
```

#### 方法二：环境变量
```bash
export SPRING_PROFILES_ACTIVE=prod
java -jar blog-backend.jar
```

#### 方法三：application.yml 修改
```yaml
spring:
  profiles:
    active: prod  # 改为 prod
```

### 3. 生产环境配置

生产环境使用环境变量来保护敏感信息：

```bash
# 1. 复制环境变量模板
cp .env.example .env

# 2. 编辑 .env 文件，填入实际值
vim .env

# 3. 加载环境变量（Linux/Mac）
source .env

# 4. 启动应用
java -jar blog-backend.jar --spring.profiles.active=prod
```

## 🔐 安全建议

1. **永远不要提交敏感信息到 Git**
   - `.env` 文件已添加到 `.gitignore`
   - 生产环境密钥通过环境变量注入

2. **生产环境 JWT 密钥生成**
   ```bash
   # 生成强密钥（Linux/Mac）
   openssl rand -base64 32

   # 或使用在线工具
   # https://www.random.org/strings/
   ```

3. **数据库连接**
   - 开发环境：localhost
   - 生产环境：使用云数据库（如阿里��RDS、腾讯云MySQL）

## 🐳 Docker 部署示例

```dockerfile
# Dockerfile
FROM openjdk:17-jdk-slim
WORKDIR /app
COPY target/blog-backend-1.0.0.jar app.jar

# 使用环境变量
ENV SPRING_PROFILES_ACTIVE=prod

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

```yaml
# docker-compose.yml
version: '3.8'
services:
  backend:
    build: .
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - DB_URL=${DB_URL}
      - DB_USERNAME=${DB_USERNAME}
      - DB_PASSWORD=${DB_PASSWORD}
      - REDIS_HOST=${REDIS_HOST}
      - REDIS_PASSWORD=${REDIS_PASSWORD}
      - JWT_SECRET=${JWT_SECRET}
      # ... 其他环境变量
    env_file:
      - .env
```

## 📊 配置差异对比

| 配置项 | 开发环境 (dev) | 生产环境 (prod) |
|--------|----------------|-----------------|
| 数据库 | localhost | 云数据库/环境变量 |
| Redis | localhost | 云Redis/环境变量 |
| MinIO | localhost:9000 | 云存储/环境变量 |
| SQL日志 | 开启 | 关闭 |
| JWT密钥 | 测试密钥 | 强密钥（环境变量） |
| OAuth回调 | localhost:8080 | 生产域名 |

## 🔧 常见问题

### Q: 如何验证当前使用的配置？
```bash
# 查看启动日志
# 会显示：The following profiles are active: dev
```

### Q: 环境变量未生效？
检查：
1. 环境变量是否正确设置：`echo $DB_PASSWORD`
2. 启动命令是否指定了 `--spring.profiles.active=prod`
3. 配置文件中占位符格式是否正确：`${VAR_NAME}`

### Q: 如何在 IDEA 中切换环境？
1. Run -> Edit Configurations
2. Active profiles: 填入 `prod` 或 `dev`
3. Environment variables: 添加所需环境变量

## 📝 部署检查清单

- [ ] 复制 `.env.example` 为 `.env` 并填入生产环境配置
- [ ] 生成强 JWT 密钥
- [ ] 配置生产数据库连接
- [ ] 配置 Redis 连接
- [ ] 配置 MinIO/OSS 对象存储
- [ ] 更新 OAuth 回调地址为生产域名
- [ ] 确认 `.env` 文件在 `.gitignore` 中
- [ ] 测试生产环境配置：`--spring.profiles.active=prod`
