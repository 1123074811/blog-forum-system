# Blog Forum System

一个前后端分离的博客论坛系统，包含文章发布、分类标签、评论互动、用户关系、相册媒体、题库测验、树洞、私信通知、后台管理等功能。项目由 `blog-backend` 后端服务、`blog-frontend` 前端应用和若干部署/检查脚本组成。

## 项目结构

```text
blog_forum/
├── blog-backend/                 # Spring Boot 后端服务
│   ├── src/main/java/com/blog/    # 后端业务代码
│   ├── src/main/resources/        # 配置文件
│   └── sql/                       # 数据库初始化与迁移脚本
├── blog-frontend/                # Vue 3 + Vite 前端应用
│   ├── src/                       # 前端源码
│   ├── meting-server.js           # B 站音乐代理服务
│   └── package.json               # 前端依赖与脚本
├── check-services.sh              # Linux 服务器服务检查脚本
└── final-deploy.sh                # Linux 一键部署/升级脚本
```

## 技术栈

### 后端

- **基础框架**：Spring Boot 3.2.0、Java 17
- **认证授权**：Spring Security、JWT、方法级权限控制
- **数据访问**：MyBatis-Plus、MySQL
- **缓存与并发**：Redis、Redisson、Caffeine
- **实时通信**：WebSocket
- **文件存储**：MinIO、阿里云 OSS
- **可观测性**：Spring Boot Actuator、Micrometer、Prometheus
- **其他能力**：邮件、验证码、OAuth、Resilience4j、AOP、限流、审计日志

### 前端

- **基础框架**：Vue 3、Vite 5
- **状态与路由**：Pinia、Vue Router
- **UI 与样式**：Element Plus、Tailwind CSS
- **请求层**：Axios
- **内容能力**：Markdown 编辑、图片/文档预览、ECharts、Konva、XLSX、PDF、DOCX
- **辅助服务**：`meting-server.js` 提供 B 站音乐搜索、音频和图片代理能力

## 主要功能

- **用户体系**：注册、登录、找回密码、JWT 鉴权、GitHub/Gitee OAuth 登录
- **内容管理**：文章发布、编辑、搜索、分类、标签、浏览统计
- **互动功能**：评论、点赞、收藏、关注、消息通知、私信聊天
- **媒体空间**：相册、素材库、文件上传、公开/匿名媒体展示
- **学习工具**：题库测验、背书神器、ER 图工具、番茄时钟
- **社区功能**：发现页、树洞、人生模拟器、公告展示
- **后台管理**：用户、文章、评论、分类、标签、收藏、相册、素材、题库、树洞、站点信息、公告管理
- **运维能力**：健康检查、指标监控、Prometheus 指标、生产部署脚本

## 环境要求

- JDK 17+
- Maven 3.8+
- Node.js 18+
- npm 9+
- MySQL 8+
- Redis 6+
- 可选：MinIO 或阿里云 OSS、SMTP 邮箱、高德地图 API、智谱 AI、GitHub/Gitee OAuth 应用

## 数据库初始化

后端默认连接数据库名为 `blog_forum`。

可以根据需要选择 SQL 脚本：

- **完整数据表脚本**：`blog-backend/sql/blog_forum.sql`
- **基础结构脚本**：`blog-backend/sql/schema.sql`
- **迁移/优化脚本**：`blog-backend/sql/*_migration.sql`、`blog-backend/sql/performance_indexes.sql`

示例：

```bash
mysql -u root -p < blog-backend/sql/blog_forum.sql
```

如果只需要创建基础结构，也可以导入：

```bash
mysql -u root -p < blog-backend/sql/schema.sql
```

## 后端本地启动

进入后端目录：

```bash
cd blog-backend
```

按需配置环境变量。Windows PowerShell 示例：

```powershell
$env:DB_URL="jdbc:mysql://localhost:3306/blog_forum?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true"
$env:DB_USERNAME="root"
$env:DB_PASSWORD="your_mysql_password"
$env:REDIS_HOST="127.0.0.1"
$env:REDIS_PORT="6379"
$env:REDIS_PASSWORD=""
$env:REDIS_DATABASE="3"
$env:JWT_SECRET="change-me-to-a-long-random-secret"
```

启动开发环境：

```bash
mvn spring-boot:run
```

默认配置：

- **服务端口**：`8080`
- **激活环境**：`dev`
- **接口前缀**：`/api`
- **Actuator**：`/actuator/health`、`/actuator/info`、`/actuator/metrics`、`/actuator/prometheus`

后端配置文件：

- `blog-backend/src/main/resources/application.yml`
- `blog-backend/src/main/resources/application-dev.yml`
- `blog-backend/src/main/resources/application-prod.yml`
- `blog-backend/src/main/resources/application-resilience4j.yml`

## 前端本地启动

进入前端目录：

```bash
cd blog-frontend
```

安装依赖：

```bash
npm install
```

启动前端和音乐代理服务：

```bash
npm run dev
```

也可以分别启动：

```bash
npm run dev:vite
npm run music-api
```

默认访问地址：

- **前端页面**：`http://localhost:5173`
- **后端代理**：`/api` -> `http://localhost:8080`
- **WebSocket 代理**：`/ws` -> `ws://localhost:8080`
- **音乐代理**：`/music-api` -> `http://localhost:3000`

前端环境变量示例：

```env
VITE_API_BASE_URL=/api
VITE_WS_BASE_URL=ws://localhost:8080
```

## 前端构建

```bash
cd blog-frontend
npm run build
```

构建产物输出到：

```text
blog-frontend/dist/
```

本地预览：

```bash
npm run preview
```

## 常用环境变量

### 后端

| 变量名 | 说明 | 默认值 |
| --- | --- | --- |
| `SERVER_PORT` | 后端服务端口 | `8080` |
| `DB_URL` | MySQL 连接地址 | `jdbc:mysql://localhost:3306/blog_forum...` |
| `DB_USERNAME` | MySQL 用户名 | `root` |
| `DB_PASSWORD` | MySQL 密码 | 开发环境为空 |
| `REDIS_HOST` | Redis 主机 | `127.0.0.1` |
| `REDIS_PORT` | Redis 端口 | `6379` |
| `REDIS_PASSWORD` | Redis 密码 | 开发环境为空 |
| `REDIS_DATABASE` | Redis 数据库编号 | `3` |
| `JWT_SECRET` | JWT 签名密钥 | 开发环境测试密钥 |
| `STORAGE_TYPE` | 文件存储类型，支持 `minio`/`oss` | `minio` |
| `MINIO_ENDPOINT` | MinIO 服务地址 | `http://localhost:9000` |
| `MINIO_ACCESS_KEY` | MinIO Access Key | `minioadmin` |
| `MINIO_SECRET_KEY` | MinIO Secret Key | `minioadmin` |
| `MINIO_BUCKET` | MinIO Bucket | `blog` |
| `ALIYUN_OSS_ENDPOINT` | 阿里云 OSS Endpoint | `oss-cn-beijing.aliyuncs.com` |
| `ALIYUN_OSS_ACCESS_KEY_ID` | 阿里云 OSS Access Key ID | 空 |
| `ALIYUN_OSS_ACCESS_KEY_SECRET` | 阿里云 OSS Access Key Secret | 空 |
| `ALIYUN_OSS_BUCKET` | 阿里云 OSS Bucket | `iblog-forum` |
| `MAIL_HOST` | SMTP 主机 | `smtp.qq.com` |
| `MAIL_PORT` | SMTP 端口 | `587` |
| `MAIL_USERNAME` | SMTP 用户名 | 空 |
| `MAIL_PASSWORD` | SMTP 密码 | 空 |
| `AMAP_KEY` | 高德地图 API Key | 空 |
| `ZHIPU_API_KEY` | 智谱 AI API Key | 空 |
| `OAUTH_GITHUB_CLIENT_ID` | GitHub OAuth Client ID | 空 |
| `OAUTH_GITHUB_CLIENT_SECRET` | GitHub OAuth Client Secret | 空 |
| `OAUTH_GITEE_CLIENT_ID` | Gitee OAuth Client ID | 空 |
| `OAUTH_GITEE_CLIENT_SECRET` | Gitee OAuth Client Secret | 空 |
| `CORS_ALLOWED_ORIGINS` | 跨域允许来源 | `http://localhost:5173` |
| `WS_ALLOWED_ORIGINS` | WebSocket 允许来源 | `http://localhost:5173` |

### 前端

| 变量名 | 说明 | 默认值 |
| --- | --- | --- |
| `VITE_API_BASE_URL` | API 请求基础路径 | `/api` |
| `VITE_WS_BASE_URL` | WebSocket 基础地址 | 根据当前站点协议和域名推导 |
| `VITE_LAYOUT_STYLE` | 前端用户端风格，支持 `classic`/`modern`；`classic` 为顶部横向导航风格，`modern` 为 `003c02bee539708fbb0ce6a87ce59c0a2bc46dea` 引入的全屏 Hero 与左侧竖排导航风格 | `classic` |

前端风格可在 `blog-frontend/.env.development` 和 `blog-frontend/.env.production` 中分别配置：

```env
VITE_LAYOUT_STYLE=classic
```

或：

```env
VITE_LAYOUT_STYLE=modern
```

## 生产部署

项目根目录提供 Linux 部署脚本：

- `final-deploy.sh`：部署/升级前后端、音乐代理、后端 systemd 服务和 Nginx 静态资源目录
- `check-services.sh`：检查运行环境、服务状态和关键配置

生产后端启动方式示例：

```bash
java -jar blog-backend.jar --spring.profiles.active=prod
```

生产环境建议通过环境变量或外部配置文件提供敏感配置，不要将数据库密码、JWT 密钥、OSS 密钥、OAuth Secret 等提交到代码仓库。

## 开发说明

- 后端主类：`com.blog.BlogApplication`
- 后端接口主要位于：`blog-backend/src/main/java/com/blog/controller`
- 前端路由位于：`blog-frontend/src/router/index.js`
- 前端 API 封装位于：`blog-frontend/src/api`
- 前端全局配置位于：`blog-frontend/src/config.js`
- Vite 代理与构建配置位于：`blog-frontend/vite.config.js`

## 端口说明

| 服务 | 默认端口 | 说明 |
| --- | --- | --- |
| 后端 API | `8080` | Spring Boot 服务 |
| 前端开发服务 | `5173` | Vite Dev Server |
| 音乐代理服务 | `3000` | `meting-server.js` |
| MinIO | `9000` | 本地对象存储，按需启用 |
| MySQL | `3306` | 数据库 |
| Redis | `6379` | 缓存与分布式能力 |

## 注意事项

- 开发环境默认启用 `dev` Profile。
- 前端开发环境通过 Vite Proxy 转发 `/api`、`/ws`、`/uploads`、`/media`、`/music-api`。
- 修改前端 `.env.development` 或 `.env.production` 后，需要重启 Vite 开发服务或重新构建前端；仅刷新浏览器不会重新读取环境变量。
- 文件上传大小默认上限为 `100MB`。
- 生产环境需要设置强随机 `JWT_SECRET`，并妥善保管所有密钥。
- 使用 OSS、OAuth、邮件、地图、AI 翻译等功能前，需要先配置对应第三方服务凭据。
