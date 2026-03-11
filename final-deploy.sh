#!/bin/bash

# ============================================
# 博客论坛系统 - 一键部署脚本（最终版）
# 适用于 Ubuntu 24.04
# ============================================

set -e

echo "=========================================="
echo "  博客论坛系统 - 一键部署"
echo "=========================================="

# 询问服务器 IP 地址
echo ""
echo "请输入服务器的公网 IP 地址："
read -p "IP地址: " SERVER_IP

# 验证 IP 地址格式
if [[ ! "$SERVER_IP" =~ ^[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
    log_error "无效的 IP 地址格式"
    exit 1
fi

echo ""
echo "=========================================="
echo "  服务器 IP: $SERVER_IP"
echo "=========================================="
echo ""

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

log_step() {
    echo -e "${BLUE}[STEP]${NC} $1"
}

# 检查是否以 root 身份运行
if [ "$EUID" -ne 0 ]; then 
    log_error "请使用 root 用户运行此脚本"
    exit 1
fi

# 进入应用目录
cd /opt/blog

log_step "1. 安装系统依赖..."
apt-get update
apt-get install -y \
    maven \
    nodejs \
    npm \
    mysql-server \
    redis-server \
    nginx \
    openjdk-17-jdk \
    curl \
    wget \
    unzip
log_info "系统依赖安装完成"

log_step "2. 配置 MySQL 数据库..."
systemctl start mysql
systemctl enable mysql

# 配置 MySQL 密码和远程访问
mysql -u root << 'SQLEOF'
ALTER USER 'root'@'localhost' IDENTIFIED WITH mysql_native_password BY 'ojc132598.';
CREATE USER IF NOT EXISTS 'root'@'%' IDENTIFIED WITH mysql_native_password BY 'ojc132598.';
GRANT ALL PRIVILEGES ON *.* TO 'root'@'%' WITH GRANT OPTION;
FLUSH PRIVILEGES;
CREATE DATABASE IF NOT EXISTS blog_forum CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
SQLEOF

# 修改 MySQL 绑定地址允许远程连接
sed -i 's/bind-address.*/bind-address = 0.0.0.0/' /etc/mysql/mysql.conf.d/mysqld.cnf 2>/dev/null || true
systemctl restart mysql
log_info "MySQL 配置完成"

log_step "3. 导入数据库数据..."
if [ -f "/opt/blog/blog-backend/sql/blog_forum.sql" ]; then
    mysql -uroot -pojc132598. blog_forum < /opt/blog/blog-backend/sql/blog_forum.sql
    log_info "数据导入完成"
else
    log_warn "SQL 文件不存在，跳过数据导入"
fi

log_step "4. 配置 Redis..."
# 启动 Redis
systemctl start redis-server || systemctl start redis || true
systemctl enable redis-server || systemctl enable redis || true

# 配置 Redis 密码和绑定地址
REDIS_CONF="/etc/redis/redis.conf"
if [ -f "$REDIS_CONF" ]; then
    # 移除旧的 bind 配置
    sed -i '/^bind /d' "$REDIS_CONF" 2>/dev/null || true
    # 添加新的 bind 配置（允许所有地址）
    echo "bind 0.0.0.0" >> "$REDIS_CONF"
    # 添加密码
    if ! grep -q "^requirepass ojc132598." "$REDIS_CONF"; then
        echo "requirepass ojc132598." >> "$REDIS_CONF"
    fi
    # 重启 Redis
    systemctl restart redis-server || systemctl restart redis || true
fi
log_info "Redis 配置完成"

log_step "5. 安装和配置 MinIO..."
# 下载 MinIO
if [ ! -f "/usr/local/bin/minio" ]; then
    wget -q https://dl.min.io/server/minio/release/linux-amd64/minio -O /usr/local/bin/minio
    chmod +x /usr/local/bin/minio
fi

# 创建数据目录
mkdir -p /opt/minio/data

# 创建 MinIO 服务（绑定到所有接口）
cat > /etc/systemd/system/minio.service << 'MINIOEOF'
[Unit]
Description=MinIO Storage Service
After=network.target

[Service]
Type=simple
User=root
WorkingDirectory=/opt/minio
ExecStart=/usr/local/bin/minio server /opt/minio/data --address "0.0.0.0:9000" --console-address "0.0.0.0:9001"
Environment="MINIO_ROOT_USER=minioadmin"
Environment="MINIO_ROOT_PASSWORD=minioadmin"
Restart=always

[Install]
WantedBy=multi-user.target
MINIOEOF

systemctl daemon-reload
systemctl start minio
systemctl enable minio
log_info "MinIO 安装完成"

log_step "6. 打包后端项目..."
cd /opt/blog/blog-backend
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64

# Maven 打包
mvn clean package -DskipTests -Dmaven.test.skip=true

# 复制 JAR 文件
if [ -f "target/*.jar" ]; then
    JAR_FILE=$(ls target/*.jar | head -1)
    cp $JAR_FILE /opt/blog/blog-backend.jar
    log_info "后端打包完成：$JAR_FILE"
else
    log_error "后端打包失败，未找到 JAR 文件"
    exit 1
fi

log_step "7. 打包前端项目..."
cd /opt/blog/blog-frontend

# 安装依赖
npm install --registry=https://registry.npmmirror.com

# 配置生产环境
cat > .env.production << ENVEOF
VITE_API_BASE_URL=http://$SERVER_IP/api
VITE_UPLOAD_BASE_URL=http://$SERVER_IP
ENVEOF

# 构建前端
npm run build

# 复制构建产物
mkdir -p /var/www/blog
if [ -d "dist" ]; then
    cp -r dist/* /var/www/blog/
    log_info "前端打包完成"
else
    log_error "前端构建失败，未找到 dist 目录"
    exit 1
fi

log_step "8. 创建后端服务..."
cd /opt/blog

# 创建 systemd 服务文件
cat > /etc/systemd/system/blog-backend.service << 'SYSTEMDEOF'
[Unit]
Description=Blog Backend Service
After=network.target mysql.service redis.service minio.service

[Service]
Type=simple
User=root
WorkingDirectory=/opt/blog
ExecStart=/usr/bin/java -jar -Xms512m -Xmx1024m /opt/blog/blog-backend.jar --spring.profiles.active=prod
Restart=always
RestartSec=10
StandardOutput=journal
StandardError=journal
SyslogIdentifier=blog-backend

[Install]
WantedBy=multi-user.target
SYSTEMDEOF

# 创建生产环境配置文件
cat > /opt/blog/application-prod.yml << PRODYML
server:
  port: 8080

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/blog_forum?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
    username: root
    password: ojc132598.
    driver-class-name: com.mysql.cj.jdbc.Driver
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000

  data:
    redis:
      host: 127.0.0.1
      port: 6379
      password: ojc132598.
      database: 3
      timeout: 10000ms
      lettuce:
        pool:
          max-active: 16
          max-idle: 8
          min-idle: 2

  mail:
    host: smtp.qq.com
    port: 587
    username: yc2118w40csh8@qq.com
    password: omsqdmirlczmeaea
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true

mybatis-plus:
  configuration:
    log-impl: org.apache.ibatis.logging.nologging.NoLoggingImpl

jwt:
  secret: prod-secret-key-change-in-production-12345678

minio:
  endpoint: http://localhost:9000
  access-key: minioadmin
  secret-key: minioadmin
  bucket: blog

amap:
  key: 08a571b9e5fdf32e3ece1875c9c6e41c

zhipu:
  api-key: 649edd3035634c99854b2b557c347cdd.kU936A2nVHnf9FSF

oauth:
  github:
    client-id: Ov23liSHhYNxADOfz6pN
    client-secret: 1f52c0d0cec92039c2ad86dee281a49bc3e22243
    redirect-uri: http://$SERVER_IP/api/auth/github/callback
  gitee:
    client-id: 30b750e1afef9df1e371d33d92d40b5dd7af791c74f9ca05dddc81cdf861c4c6
    client-secret: 5a4d8e35892b218736cb260f796cfb715bc0f640d2147ee6e0f286b73723759e
    redirect-uri: http://$SERVER_IP/api/auth/gitee/callback
PRODYML

systemctl daemon-reload
systemctl start blog-backend
systemctl enable blog-backend
log_info "后端服务创建完成"

log_step "9. 配置 Nginx..."
# 创建 Nginx 配置文件
cat > /etc/nginx/sites-available/blog << 'NGINXEOF'
server {
    listen 80;
    server_name _;
    
    # 前端静态文件
    location / {
        root /var/www/blog;
        index index.html;
        try_files $uri $uri/ /index.html;
    }
    
    # 后端 API 代理
    location /api/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_connect_timeout 60s;
        proxy_read_timeout 60s;
    }
    
    # 文件上传代理
    location /uploads/ {
        alias /opt/blog/uploads/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
NGINXEOF

# 移除默认配置，启用博客配置
rm -f /etc/nginx/sites-enabled/default
ln -sf /etc/nginx/sites-available/blog /etc/nginx/sites-enabled/blog

# 测试并重启 Nginx
nginx -t
systemctl restart nginx
systemctl enable nginx
log_info "Nginx 配置完成"

log_step "10. 检查服务状态..."
echo ""
echo "=========================================="
echo "  服务状态检查"
echo "=========================================="

services=("mysql" "redis-server" "minio" "blog-backend" "nginx")
for service in "${services[@]}"; do
    if systemctl is-active --quiet "$service" 2>/dev/null || systemctl is-active --quiet "${service}.service" 2>/dev/null; then
        log_info "✓ $service 运行正常"
    else
        log_warn "✗ $service 未运行"
    fi
done

echo ""
echo "=========================================="
echo "  部署完成！"
echo "=========================================="
echo ""
echo "访问地址：http://$SERVER_IP"
echo ""
echo "数据库连接信息："
echo "  主机：$SERVER_IP"
echo "  端口：3306"
echo "  用户：root"
echo "  密码：ojc132598."
echo ""
echo "MinIO 控制台：http://$SERVER_IP:9001"
echo "  用户：minioadmin"
echo "  密码：minioadmin"
echo ""
echo "查看后端日志：journalctl -u blog-backend -f"
echo "查看 Nginx 日志：tail -f /var/log/nginx/error.log"
echo ""
