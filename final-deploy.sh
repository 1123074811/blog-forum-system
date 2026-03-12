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

ENV_FILE="/opt/blog/blog-backend/.env"
if [ ! -f "$ENV_FILE" ]; then
    ENV_FILE="$(cd "$(dirname "$0")" && pwd)/blog-backend/.env"
fi

if [ ! -f "$ENV_FILE" ]; then
    log_error "未找到配置文件：/opt/blog/blog-backend/.env"
    exit 1
fi

# 规范化 .env 行尾（处理 Windows CRLF 导致的 $'\r' 报错）
if grep -q $'\r' "$ENV_FILE"; then
    sed -i 's/\r$//' "$ENV_FILE"
fi

set -a
source "$ENV_FILE"
set +a

# 交互填充缺失的关键变量（避免因 .env 未配置而中断）
if [ -z "$SERVER_PORT" ]; then
    read -p "后端服务端口 [8080]: " SERVER_PORT
    SERVER_PORT=${SERVER_PORT:-8080}
fi

DB_NAME_DEFAULT="blog_forum"
DB_NAME_FROM_URL=$(echo "${DB_URL:-}" | sed -E 's#^jdbc:mysql://[^/]+/([^?]+).*#\1#')
DB_NAME=${DB_NAME_FROM_URL:-$DB_NAME_DEFAULT}
JDBC_DEFAULT="jdbc:mysql://localhost:3306/$DB_NAME?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true"

if [ -z "$DB_URL" ]; then
    read -p "MySQL JDBC URL [$JDBC_DEFAULT]: " DB_URL
    DB_URL=${DB_URL:-$JDBC_DEFAULT}
fi

if [ -z "$DB_USERNAME" ]; then
    read -p "数据库用户名: " DB_USERNAME
fi

while [ -z "$DB_USERNAME" ]; do
    read -p "数据库用户名: " DB_USERNAME
done

if [ -z "$DB_PASSWORD" ]; then
    read -s -p "数据库密码: " DB_PASSWORD
    echo ""
fi

while [ -z "$DB_PASSWORD" ]; do
    read -s -p "数据库密码不能为空，请重新输入: " DB_PASSWORD
    echo ""
done

MYSQL_ROOT_PASSWORD="${MYSQL_ROOT_PASSWORD:-}"
if [ -z "$MYSQL_ROOT_PASSWORD" ] && [ "$DB_USERNAME" = "root" ]; then
    MYSQL_ROOT_PASSWORD="$DB_PASSWORD"
fi

if [ -n "$MYSQL_ROOT_PASSWORD" ]; then
    MYSQL_ADMIN_ARGS=(mysql -uroot -p"$MYSQL_ROOT_PASSWORD")
else
    MYSQL_ADMIN_ARGS=(mysql)
fi

REQUIRED_VARS=(
    SERVER_PORT
    DB_URL
    DB_USERNAME
    DB_PASSWORD
    REDIS_HOST
    REDIS_PORT
    REDIS_PASSWORD
    REDIS_DATABASE
    MAIL_HOST
    MAIL_PORT
    MAIL_USERNAME
    MAIL_PASSWORD
    JWT_SECRET
    ALIYUN_OSS_ENDPOINT
    ALIYUN_OSS_ACCESS_KEY_ID
    ALIYUN_OSS_ACCESS_KEY_SECRET
    ALIYUN_OSS_BUCKET
    AMAP_KEY
    ZHIPU_API_KEY
    OAUTH_GITHUB_CLIENT_ID
    OAUTH_GITHUB_CLIENT_SECRET
    OAUTH_GITHUB_REDIRECT_URI
    OAUTH_GITEE_CLIENT_ID
    OAUTH_GITEE_CLIENT_SECRET
    OAUTH_GITEE_REDIRECT_URI
)

for var_name in "${REQUIRED_VARS[@]}"; do
    if [ -z "${!var_name}" ]; then
        log_error ".env 中缺少必要配置：$var_name"
        exit 1
    fi
done

OSS_ENDPOINT="$ALIYUN_OSS_ENDPOINT"
OSS_ACCESS_KEY_ID="$ALIYUN_OSS_ACCESS_KEY_ID"
OSS_ACCESS_KEY_SECRET="$ALIYUN_OSS_ACCESS_KEY_SECRET"
OSS_BUCKET="$ALIYUN_OSS_BUCKET"
OSS_CUSTOM_DOMAIN="${ALIYUN_OSS_CUSTOM_DOMAIN:-}"
DB_NAME=$(echo "$DB_URL" | sed -E 's#^jdbc:mysql://[^/]+/([^?]+).*#\1#')
DB_NAME=${DB_NAME:-blog_forum}

if [ "${STORAGE_TYPE:-oss}" != "oss" ]; then
    log_warn ".env 中 STORAGE_TYPE=${STORAGE_TYPE}，部署脚本将使用 OSS 配置"
fi

# 进入应用目录
cd /opt/blog

log_step "1. 安装系统依赖..."
apt-get update
apt-get install -y \
    maven \
    nodejs \
    mysql-server \
    redis-server \
    nginx \
    openjdk-17-jdk \
    curl \
    wget \
    unzip
if ! command -v npm >/dev/null 2>&1; then
    apt-get install -y npm
fi
log_info "系统依赖安装完成"

log_step "2. 配置 MySQL 数据库..."
systemctl start mysql
systemctl enable mysql

${MYSQL_ADMIN_ARGS[@]} << SQLEOF
CREATE USER IF NOT EXISTS '$DB_USERNAME'@'localhost' IDENTIFIED WITH mysql_native_password BY '$DB_PASSWORD';
ALTER USER '$DB_USERNAME'@'localhost' IDENTIFIED WITH mysql_native_password BY '$DB_PASSWORD';
CREATE USER IF NOT EXISTS '$DB_USERNAME'@'%' IDENTIFIED WITH mysql_native_password BY '$DB_PASSWORD';
ALTER USER '$DB_USERNAME'@'%' IDENTIFIED WITH mysql_native_password BY '$DB_PASSWORD';
GRANT ALL PRIVILEGES ON *.* TO '$DB_USERNAME'@'%' WITH GRANT OPTION;
FLUSH PRIVILEGES;
CREATE DATABASE IF NOT EXISTS $DB_NAME CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
SQLEOF

# 修改 MySQL 绑定地址允许远程连接
sed -i 's/bind-address.*/bind-address = 0.0.0.0/' /etc/mysql/mysql.conf.d/mysqld.cnf 2>/dev/null || true
systemctl restart mysql
log_info "MySQL 配置完成"

log_step "3. 导入数据库数据..."
COUNT=$(mysql -u"$DB_USERNAME" -p"$DB_PASSWORD" -N -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='$DB_NAME'" 2>/dev/null || echo 0)
if [ "$COUNT" -gt 0 ]; then
    log_info "检测到数据库已有表，跳过数据导入"
else
    if [ -f "/opt/blog/blog-backend/sql/blog_forum.sql" ]; then
        mysql -u"$DB_USERNAME" -p"$DB_PASSWORD" "$DB_NAME" < /opt/blog/blog-backend/sql/blog_forum.sql
        log_info "数据导入完成"
    else
        log_warn "SQL 文件不存在，跳过数据导入"
    fi
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
    if ! grep -Fq "requirepass $REDIS_PASSWORD" "$REDIS_CONF"; then
        echo "requirepass $REDIS_PASSWORD" >> "$REDIS_CONF"
    fi
    # 重启 Redis
    systemctl restart redis-server || systemctl restart redis || true
fi
log_info "Redis 配置完成"

log_step "5. 配置 OSS 存储参数..."
log_info "OSS 参数已设置：Endpoint=$OSS_ENDPOINT, Bucket=$OSS_BUCKET"

log_step "6. 打包后端项目..."
if [ -f "/opt/blog/blog-backend.jar" ]; then
    log_info "检测到后端 JAR 已存在，跳过打包"
else
    cd /opt/blog/blog-backend
    export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
    mvn clean package -DskipTests -Dmaven.test.skip=true
    JAR_FILE=$(find target -maxdepth 1 -type f -name "*.jar" | head -n 1)
    if [ -n "$JAR_FILE" ] && [ -f "$JAR_FILE" ]; then
        cp "$JAR_FILE" /opt/blog/blog-backend.jar
        log_info "后端打包完成：$JAR_FILE"
    else
        log_error "后端打包失败，未找到 JAR 文件（请检查 target 目录及 Maven 打包）"
        exit 1
    fi
fi

log_step "7. 打包前端项目..."
if [ -f "/var/www/blog/index.html" ]; then
    log_info "检测到前端已部署，跳过构建"
else
    cd /opt/blog/blog-frontend
    npm install --registry=https://registry.npmmirror.com
    cat > .env.production << ENVEOF
VITE_API_BASE_URL=http://$SERVER_IP/api
VITE_UPLOAD_BASE_URL=http://$SERVER_IP
ENVEOF
    npm run build
    mkdir -p /var/www/blog
    if [ -d "dist" ]; then
        cp -r dist/* /var/www/blog/
        log_info "前端打包完成"
    else
        log_error "前端构建失败，未找到 dist 目录"
        exit 1
    fi
fi

log_step "8. 创建后端服务..."
cd /opt/blog

if [ ! -f "/etc/systemd/system/blog-backend.service" ]; then
cat > /etc/systemd/system/blog-backend.service << 'SYSTEMDEOF'
[Unit]
Description=Blog Backend Service
After=network.target mysql.service redis.service

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
fi

if [ ! -f "/opt/blog/application-prod.yml" ]; then
cat > /opt/blog/application-prod.yml << PRODYML
server:
  port: $SERVER_PORT

spring:
  datasource:
    url: $DB_URL
    username: $DB_USERNAME
    password: $DB_PASSWORD
    driver-class-name: com.mysql.cj.jdbc.Driver
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000

  data:
    redis:
      host: $REDIS_HOST
      port: $REDIS_PORT
      password: $REDIS_PASSWORD
      database: $REDIS_DATABASE
      timeout: 10000ms
      lettuce:
        pool:
          max-active: 16
          max-idle: 8
          min-idle: 2

  mail:
    host: $MAIL_HOST
    port: $MAIL_PORT
    username: $MAIL_USERNAME
    password: $MAIL_PASSWORD
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
  secret: $JWT_SECRET

storage:
  type: oss

aliyun:
  oss:
    endpoint: $OSS_ENDPOINT
    access-key-id: ${ALIYUN_OSS_ACCESS_KEY_ID:}
    access-key-secret: ${ALIYUN_OSS_ACCESS_KEY_SECRET:}
    bucket: $OSS_BUCKET
    custom-domain: "$OSS_CUSTOM_DOMAIN"

amap:
  key: $AMAP_KEY

zhipu:
  api-key: $ZHIPU_API_KEY

oauth:
  github:
    client-id: $OAUTH_GITHUB_CLIENT_ID
    client-secret: $OAUTH_GITHUB_CLIENT_SECRET
    redirect-uri: http://$SERVER_IP/api/auth/github/callback
  gitee:
    client-id: $OAUTH_GITEE_CLIENT_ID
    client-secret: $OAUTH_GITEE_CLIENT_SECRET
    redirect-uri: http://$SERVER_IP/api/auth/gitee/callback
PRODYML
fi

systemctl daemon-reload
systemctl start blog-backend
systemctl enable blog-backend
log_info "后端服务创建完成"

log_step "9. 配置 Nginx..."
if [ ! -f "/etc/nginx/sites-available/blog" ]; then
cat > /etc/nginx/sites-available/blog << NGINXEOF
server {
    listen 80;
    server_name _;
    
    # 前端静态文件
    location / {
        root /var/www/blog;
        index index.html;
        try_files \$uri \$uri/ /index.html;
    }
    
    # 后端 API 代理
    location /api/ {
        proxy_pass http://127.0.0.1:$SERVER_PORT;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
        proxy_connect_timeout 60s;
        proxy_read_timeout 60s;
    }
    
    # 文件上传代理
    location /uploads/ {
        alias /opt/blog/uploads/;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
    }
}
NGINXEOF
fi

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

services=("mysql" "redis-server" "blog-backend" "nginx")
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
echo "  用户：$DB_USERNAME"
echo "  密码：$DB_PASSWORD"
echo ""
echo "OSS 存储配置："
echo "  Endpoint：$OSS_ENDPOINT"
echo "  Bucket：$OSS_BUCKET"
if [ -n "$OSS_CUSTOM_DOMAIN" ]; then
    echo "  自定义域名：$OSS_CUSTOM_DOMAIN"
fi
echo ""
echo "查看后端日志：journalctl -u blog-backend -f"
echo "查看 Nginx 日志：tail -f /var/log/nginx/error.log"
echo ""
