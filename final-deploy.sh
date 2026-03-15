#!/bin/bash

# ============================================
# 博客论坛系统 - 一键部署脚本（最终版）
# 适用于 Ubuntu 24.04
# 支持全新部署和升级更新
# ============================================

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
OPS_DIR="$SCRIPT_DIR/ops"
SECURE_NGINX_TEMPLATE="$OPS_DIR/nginx/secure-blog.conf"
SECURITY_EVENT_SQL="$SCRIPT_DIR/blog-backend/sql/security_event.sql"
KEY_ROTATION_RUNBOOK="$OPS_DIR/key-rotation-runbook.md"
CLOUDFLARE_RUNBOOK="$OPS_DIR/cloudflare-runbook.md"

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

import_optional_sql() {
    local sql_file="$1"
    local db_name="$2"

    if [ ! -f "$sql_file" ]; then
        log_warn "SQL 文件不存在，跳过导入：$sql_file"
        return 0
    fi

    mysql -u"$DB_USERNAME" -p"$DB_PASSWORD" "$db_name" < "$sql_file"
    log_info "SQL 导入完成：$(basename "$sql_file")"
}

write_secure_nginx_conf() {
    local domain_name="$1"
    local server_port="$2"

    cat > /etc/nginx/sites-available/blog <<EOF
# Generated from ops/nginx/secure-blog.conf
limit_req_zone  \$binary_remote_addr zone=api_limit:10m    rate=20r/s;
limit_req_zone  \$binary_remote_addr zone=login_limit:10m  rate=5r/m;
limit_req_zone  \$binary_remote_addr zone=admin_limit:10m  rate=30r/m;
limit_conn_zone \$binary_remote_addr zone=conn_limit:10m;

server {
    listen 80 default_server;
    return 444;
}

server {
    listen 80;
    server_name $domain_name www.$domain_name;

    location /.well-known/acme-challenge/ {
        root /var/www/certbot;
    }

    location / {
        return 301 https://\$host\$request_uri;
    }
}

server {
    listen 443 ssl http2;
    server_name $domain_name www.$domain_name;

    ssl_certificate     /etc/letsencrypt/live/$domain_name/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/$domain_name/privkey.pem;
    ssl_protocols TLSv1.2 TLSv1.3;

    add_header X-Frame-Options DENY always;
    add_header X-Content-Type-Options nosniff always;
    add_header X-XSS-Protection "1; mode=block" always;
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
    add_header Referrer-Policy "strict-origin-when-cross-origin" always;

    client_max_body_size 100m;

    location ~ /\.git { return 404; }
    location ~ /\.env { return 404; }
    location ~ /actuator/(env|beans) { return 404; }

    location /api/auth/login {
        limit_req zone=login_limit burst=3 nodelay;
        limit_conn zone=conn_limit 5;
        proxy_pass http://127.0.0.1:$server_port;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
    }

    location /api/admin/ {
        limit_req zone=admin_limit burst=10 nodelay;
        limit_conn zone=conn_limit 10;
        proxy_pass http://127.0.0.1:$server_port;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
    }

    location /api/ {
        limit_req zone=api_limit burst=40 nodelay;
        limit_conn zone=conn_limit 20;
        proxy_pass http://127.0.0.1:$server_port;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
        proxy_connect_timeout 60s;
        proxy_read_timeout 60s;
    }

    location /ws/ {
        proxy_pass http://127.0.0.1:$server_port;
        proxy_http_version 1.1;
        proxy_set_header Upgrade \$http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
        proxy_read_timeout 3600s;
    }

    location /uploads/ {
        alias /opt/blog/uploads/;
    }

    location /music-api/ {
        proxy_pass http://127.0.0.1:3001;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
    }

    location / {
        root /var/www/blog;
        index index.html;
        try_files \$uri \$uri/ /index.html;
    }
}
EOF

    log_info "已应用安全版 Nginx 配置"
}

echo "=========================================="
echo "  博客论坛系统 - 一键部署/升级"
echo "=========================================="

# 检测是否为升级模式
IS_UPGRADE=false
if [ -f "/opt/blog/blog-backend.jar" ] || [ -d "/var/www/blog" ]; then
    echo ""
    echo "检测到现有部署，是否进行升级？"
    echo "  - 选择 'y'：升级现有部署（保留数据库）"
    echo "  - 选择 'n'：全新部署（会重新初始化数据库）"
    read -p "是否升级 (y/n) [y]: " UPGRADE_OPT
    UPGRADE_OPT=${UPGRADE_OPT:-y}
    if [[ "$UPGRADE_OPT" =~ ^[Yy]$ ]]; then
        IS_UPGRADE=true
        echo ""
        echo "=========================================="
        echo "  模式：升级现有部署"
        echo "  说明：将保留数据库，仅更新前后端代码"
        echo "=========================================="
    else
        echo ""
        echo "=========================================="
        echo "  模式：全新部署"
        echo "  说明：将重新初始化数据库"
        echo "=========================================="
    fi
fi

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

if [ "$IS_UPGRADE" = "false" ]; then
    log_step "2. 配置 MySQL 数据库..."
    systemctl start mysql
    systemctl enable mysql

    # 检查并加载 mysql_native_password 插件
    ${MYSQL_ADMIN_ARGS[@]} -e "INSTALL PLUGIN mysql_native_password SONAME 'mysql_native_password.so';" 2>/dev/null || true

log_step "2. 配置 MySQL 数据库（强制兼容 JDBC 连接）..."

systemctl start mysql
systemctl enable mysql

# 确保 mysql_native_password 插件可用（MySQL 8 有时需要手动 install）
${MYSQL_ADMIN_ARGS[@]} -e "INSTALL PLUGIN mysql_native_password SONAME 'mysql_native_password.so';" 2>/dev/null || true

# 使用 caching_sha2_password（MySQL 8 默认推荐）或 mysql_native_password（更兼容旧 JDBC）
# 这里优先用 caching_sha2_password，如果你 driver 版本较旧可改成 mysql_native_password
AUTH_PLUGIN="caching_sha2_password"
# AUTH_PLUGIN="mysql_native_password"   # 如果上面不行，取消注释这一行

${MYSQL_ADMIN_ARGS[@]} << SQLEOF
-- 先处理 localhost 用户（最关键）
CREATE USER IF NOT EXISTS '$DB_USERNAME'@'localhost' IDENTIFIED WITH $AUTH_PLUGIN BY '$DB_PASSWORD';
ALTER USER '$DB_USERNAME'@'localhost' IDENTIFIED WITH $AUTH_PLUGIN BY '$DB_PASSWORD';

-- 同时创建/更新 % 用户（允许远程，如果以后需要）
CREATE USER IF NOT EXISTS '$DB_USERNAME'@'%' IDENTIFIED WITH $AUTH_PLUGIN BY '$DB_PASSWORD';
ALTER USER '$DB_USERNAME'@'%' IDENTIFIED WITH $AUTH_PLUGIN BY '$DB_PASSWORD';

-- 授予权限（生产建议限制到具体数据库，但这里保持原样方便开发）
GRANT ALL PRIVILEGES ON *.* TO '$DB_USERNAME'@'localhost';
GRANT ALL PRIVILEGES ON *.* TO '$DB_USERNAME'@'%' WITH GRANT OPTION;

FLUSH PRIVILEGES;

-- 创建数据库
CREATE DATABASE IF NOT EXISTS $DB_NAME CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 调试输出：确认插件是否正确设置
SELECT user, host, plugin, authentication_string FROM mysql.user WHERE user = '$DB_USERNAME';
SQLEOF

# 允许远程连接（0.0.0.0）
sed -i 's/^bind-address.*/bind-address = 0.0.0.0/' /etc/mysql/mysql.conf.d/mysqld.cnf 2>/dev/null || true
sed -i 's/^#bind-address.*/bind-address = 0.0.0.0/' /etc/mysql/mysql.conf.d/mysqld.cnf 2>/dev/null || true

systemctl restart mysql

# 等待 MySQL 重启完成
sleep 5

log_info "MySQL 配置完成，已强制使用 $AUTH_PLUGIN 认证插件（兼容 Spring Boot JDBC）"
log_info "建议在 application-prod.yml 中使用 url: jdbc:mysql://127.0.0.1:3306/... 而非 localhost（更稳定）"

    # 修改 MySQL 绑定地址允许远程连接
    sed -i 's/bind-address.*/bind-address = 0.0.0.0/' /etc/mysql/mysql.conf.d/mysqld.cnf 2>/dev/null || true
    systemctl restart mysql
    log_info "MySQL 配置完成"
else
    log_step "2. 跳过数据库配置（升级模式）..."
    log_info "数据库将保持不变"
fi

if [ "$IS_UPGRADE" = "false" ]; then
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
else
    log_step "3. 跳过数据库数据导入（升级模式）..."
    log_info "数据库数据将保持不变"
fi

if [ "$IS_UPGRADE" = "false" ]; then
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
else
    log_step "4. 跳过 Redis 配置（升级模式）..."
    log_info "Redis 配置将保持不变"
fi

log_step "5. 配置 OSS 存储参数..."
log_info "OSS 参数已设置：Endpoint=$OSS_ENDPOINT, Bucket=$OSS_BUCKET"

log_step "6. 打包后端项目..."
if [ "$IS_UPGRADE" = "true" ]; then
    log_info "升级模式：重新打包后端项目"
fi
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

log_step "7. 打包前端项目..."
if [ "$IS_UPGRADE" = "true" ]; then
    log_info "升级模式：重新打包前端项目"
fi
cd /opt/blog/blog-frontend
npm install --registry=https://registry.npmmirror.com
cat > .env.production << ENVEOF
VITE_API_BASE_URL=/api
VITE_UPLOAD_BASE_URL=/
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

log_step "8. 配置音乐API服务..."
cd /opt/blog

# 无论是否升级模式，都确保服务文件存在
if [ ! -f "/etc/systemd/system/blog-music-api.service" ]; then
    cat > /etc/systemd/system/blog-music-api.service << 'SYSTEMDEOF'
[Unit]
Description=Blog Music API Service
After=network.target

[Service]
Type=simple
User=root
WorkingDirectory=/opt/blog/blog-frontend
ExecStart=/usr/bin/node meting-server.js
Restart=always
RestartSec=10
StandardOutput=journal
StandardError=journal
SyslogIdentifier=blog-music-api

[Install]
WantedBy=multi-user.target
SYSTEMDEOF
    log_info "创建音乐API服务文件"
fi

systemctl daemon-reload
systemctl start blog-music-api
systemctl enable blog-music-api

if [ "$IS_UPGRADE" = "true" ]; then
    log_info "升级模式：音乐API服务重启完成"
else
    log_info "音乐API服务创建完成"
fi

log_step "9. 配置后端服务..."
cd /opt/blog

if [ "$IS_UPGRADE" = "false" ]; then
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
  frontend-redirect-base: ${OAUTH_FRONTEND_REDIRECT_BASE:https://oujincong.xyz}
  github:
    client-id: $OAUTH_GITHUB_CLIENT_ID
    client-secret: $OAUTH_GITHUB_CLIENT_SECRET
    redirect-uri: $OAUTH_GITHUB_REDIRECT_URI
  gitee:
    client-id: $OAUTH_GITEE_CLIENT_ID
    client-secret: $OAUTH_GITEE_CLIENT_SECRET
    redirect-uri: $OAUTH_GITEE_REDIRECT_URI
PRODYML
    fi
    
    systemctl daemon-reload
    systemctl start blog-backend
    systemctl enable blog-backend
    log_info "后端服务创建完成"
else
    log_info "升级模式：重启后端服务"
    systemctl daemon-reload
    systemctl restart blog-backend
    log_info "后端服务重启完成"
fi

if [ "$IS_UPGRADE" = "false" ]; then
    log_step "10. 配置 Nginx 和 SSL 证书..."

    # 询问是否配置域名和 HTTPS
    echo ""
    log_info "是否配置域名并启用 HTTPS？"
    echo "  - 选择 'y'：需要输入域名，自动申请 SSL 证书，启用 HTTPS 访问"
    echo "  - 选择 'n'：使用服务器 IP 直接访问，不启用 HTTPS"
    read -p "是否配置域名 (y/n) [y]: " ENABLE_DOMAIN
    ENABLE_DOMAIN=${ENABLE_DOMAIN:-y}

    DOMAIN_NAME=""
    if [[ "$ENABLE_DOMAIN" =~ ^[Yy]$ ]]; then
        echo ""
        echo "=========================================="
        echo "  域名配置说明"
        echo "=========================================="
        echo ""
        echo "请确保已完成以下域名配置："
        echo "1. 域名已备案（如使用中国大陆服务器）"
        echo "2. DNS 解析已设置："
        echo "   - A 记录：@ -> $SERVER_IP"
        echo "   - A 记录：www -> $SERVER_IP"
        echo "3. 服务器安全组已开放 80 和 443 端口"
        echo ""
        echo "常见 DNS 服务商解析设置："
        echo "  - 阿里云：域名控制台 -> 解析设置 -> 添加记录"
        echo "  - 腾讯云：DNS 控制台 -> 域名解析 -> 添加记录"
        echo "  - Cloudflare：DNS -> Add record"
        echo ""
        echo "验证 DNS 解析命令："
        echo "  ping yourdomain.com"
        echo "  ping www.yourdomain.com"
        echo ""
        
        read -p "请输入主域名（例如：example.com）: " DOMAIN_NAME
        
        if [[ -z "$DOMAIN_NAME" ]]; then
            log_error "域名不能为空"
            exit 1
        fi
        
        echo ""
        log_info "正在验证域名解析..."
        if ping -c 1 -W 1 "$DOMAIN_NAME" >/dev/null 2>&1; then
            log_info "域名 $DOMAIN_NAME 可以正常解析"
        else
            log_warn "域名 $DOMAIN_NAME 解析失败或超时"
            log_warn "请检查 DNS 解析配置是否正确"
            read -p "是否继续申请证书？（可能失败）(y/n): " CONTINUE_SSL
            if [[ ! "$CONTINUE_SSL" =~ ^[Yy]$ ]]; then
                log_info "已取消域名配置，将使用 IP 访问"
                DOMAIN_NAME=""
            fi
        fi
        
        if [[ -n "$DOMAIN_NAME" ]]; then
            log_info "正在安装 Certbot..."
            apt-get install -y certbot python3-certbot-nginx
            
            log_info "正在申请 SSL 证书..."
            certbot --nginx -d $DOMAIN_NAME -d www.$DOMAIN_NAME --non-interactive --agree-tos --email admin@$DOMAIN_NAME
            
            log_info "SSL 证书申请完成！"
            if [ -f "$SECURE_NGINX_TEMPLATE" ]; then
                log_info "应用安全版 Nginx 配置模板..."
                write_secure_nginx_conf "$DOMAIN_NAME" "$SERVER_PORT"
            else
                log_warn "未找到安全版 Nginx 模板，保留脚本生成的默认配置"
            fi
            log_info "证书有效期 90 天，Certbot 会自动续期"
        fi
    else
        log_warn "跳过域名配置，使用 HTTP + IP 访问"
    fi

    log_info "配置 Nginx..."
    if [ ! -f "/etc/nginx/sites-available/blog" ]; then
    if [[ "$ENABLE_DOMAIN" =~ ^[Yy]$ ]] && [ -n "$DOMAIN_NAME" ]; then
    cat > /etc/nginx/sites-available/blog << NGINXEOF
server {
    listen 80;
    server_name $DOMAIN_NAME www.$DOMAIN_NAME;
    
    location /.well-known/acme-challenge/ {
        root /var/www/certbot;
    }
    
    location / {
        return 301 https://\$server_name\$request_uri;
    }
}

server {
    listen 443 ssl http2;
    server_name $DOMAIN_NAME www.$DOMAIN_NAME;
    
    ssl_certificate /etc/letsencrypt/live/$DOMAIN_NAME/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/$DOMAIN_NAME/privkey.pem;
    
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;
    ssl_prefer_server_ciphers on;
    ssl_session_cache shared:SSL:10m;
    ssl_session_timeout 10m;

    client_max_body_size 100m;
    
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

    # WebSocket 代理
    location /ws/ {
        proxy_pass http://127.0.0.1:$SERVER_PORT;
        proxy_http_version 1.1;
        proxy_set_header Upgrade \$http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
        proxy_read_timeout 3600s;
    }
    
    # 文件上传代理
    location /uploads/ {
        alias /opt/blog/uploads/;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
    }
    
    # 音乐API代理
    location /music-api/ {
        proxy_pass http://localhost:3001;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
    }
    
    # 酷狗搜索API代理
    location /kugou-api/ {
        proxy_pass http://mobilecdn.kugou.com;
        proxy_set_header Host mobilecdn.kugou.com;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header Referer http://m.kugou.com/;
        proxy_set_header User-Agent "Mozilla/5.0 (iPhone; CPU iPhone OS 14_0 like Mac OS X) AppleWebKit/605.1.15";
    }
    
    # 酷狗播放URL获取接口
    location /kugou-play/ {
        proxy_pass http://m.kugou.com;
        proxy_set_header Host m.kugou.com;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header Referer http://m.kugou.com/;
        proxy_set_header User-Agent "Mozilla/5.0 (iPhone; CPU iPhone OS 14_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15E148";
    }
    
    # QQ音乐API代理
    location /qq-api/ {
        proxy_pass https://c.y.qq.com;
        proxy_set_header Host c.y.qq.com;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header Referer https://y.qq.com/;
        proxy_set_header Origin https://y.qq.com;
    }
    
    # QQ音乐播放URL代理
    location /qq-play/ {
        proxy_pass https://u.y.qq.com;
        proxy_set_header Host u.y.qq.com;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header Referer https://y.qq.com/;
        proxy_set_header Origin https://y.qq.com;
    }
}
NGINXEOF
    else
    cat > /etc/nginx/sites-available/blog << NGINXEOF
# HTTP server - redirect to HTTPS
server {
    listen 80;
    server_name _;
    
    location / {
        return 301 https://\$server_ip\$request_uri;
    }
}

# HTTPS server
server {
    listen 443 ssl http2;
    server_name _;
    
    # 自签名 SSL 证书（用于 IP 访问）
    ssl_certificate /etc/nginx/ssl/server.crt;
    ssl_certificate_key /etc/nginx/ssl/server.key;
    
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;
    ssl_prefer_server_ciphers on;
    ssl_session_cache shared:SSL:10m;
    ssl_session_timeout 10m;

    client_max_body_size 100m;
    
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

    # WebSocket 代理
    location /ws/ {
        proxy_pass http://127.0.0.1:$SERVER_PORT;
        proxy_http_version 1.1;
        proxy_set_header Upgrade \$http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
        proxy_read_timeout 3600s;
    }
    
    # 文件上传代理
    location /uploads/ {
        alias /opt/blog/uploads/;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
    }
    
    # 音乐API代理
    location /music-api/ {
        proxy_pass http://localhost:3001;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
    }
    
    # 酷狗搜索API代理
    location /kugou-api/ {
        proxy_pass http://mobilecdn.kugou.com;
        proxy_set_header Host mobilecdn.kugou.com;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header Referer http://m.kugou.com/;
        proxy_set_header User-Agent "Mozilla/5.0 (iPhone; CPU iPhone OS 14_0 like Mac OS X) AppleWebKit/605.1.15";
    }
    
    # 酷狗播放URL获取接口
    location /kugou-play/ {
        proxy_pass http://m.kugou.com;
        proxy_set_header Host m.kugou.com;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header Referer http://m.kugou.com/;
        proxy_set_header User-Agent "Mozilla/5.0 (iPhone; CPU iPhone OS 14_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15E148";
    }
    
    # QQ音乐API代理
    location /qq-api/ {
        proxy_pass https://c.y.qq.com;
        proxy_set_header Host c.y.qq.com;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header Referer https://y.qq.com/;
        proxy_set_header Origin https://y.qq.com;
    }
    
    # QQ音乐播放URL代理
    location /qq-play/ {
        proxy_pass https://u.y.qq.com;
        proxy_set_header Host u.y.qq.com;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header Referer https://y.qq.com/;
        proxy_set_header Origin https://y.qq.com;
    }
}
NGINXEOF

    # 创建自签名 SSL 证书目录和证书
    mkdir -p /etc/nginx/ssl
    if [ ! -f "/etc/nginx/ssl/server.crt" ]; then
        openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
            -keyout /etc/nginx/ssl/server.key \
            -out /etc/nginx/ssl/server.crt \
            -subj "/C=CN/ST=State/L=City/O=Organization/CN=$SERVER_IP"
        log_info "自签名 SSL 证书已生成"
    fi
    fi
    fi

    # 移除默认配置，启用博客配置
    rm -f /etc/nginx/sites-enabled/default
    ln -sf /etc/nginx/sites-available/blog /etc/nginx/sites-enabled/blog

    # 测试并重启 Nginx
    nginx -t
    systemctl restart nginx
    systemctl enable nginx
    log_info "Nginx 配置完成"
else
    log_step "10. 重启 Nginx（升级模式）..."
    nginx -t
    systemctl restart nginx
    log_info "Nginx 重启完成"
fi

log_step "11. 检查服务状态..."
echo ""
echo "=========================================="
echo "  服务状态检查"
echo "=========================================="

services=("mysql" "redis-server" "blog-music-api" "blog-backend" "nginx")
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
if [[ -n "$DOMAIN_NAME" ]]; then
    echo "访问地址："
    echo "  HTTPS: https://$DOMAIN_NAME"
    echo "         https://www.$DOMAIN_NAME"
    echo ""
    echo "SSL 证书信息："
    echo "  证书类型：Let's Encrypt（免费）"
    echo "  证书有效期：90 天"
    echo "  自动续期：已配置"
    echo ""
    echo "管理证书命令："
    echo "  查看证书：certbot certificates"
    echo "  续期证书：certbot renew"
else
    echo "访问地址：http://$SERVER_IP"
    echo ""
    echo "提示：如需启用 HTTPS，请配置域名后重新运行部署脚本"
fi
echo ""
echo "=========================================="
echo "  域名配置指南（如需配置域名）"
echo "=========================================="
echo ""
echo "1. 购买域名（阿里云、腾讯云、Namecheap 等）"
echo ""
echo "2. 添加 DNS 解析记录："
echo "   记录类型：A"
echo "   主机记录：@"
echo "   记录值：$SERVER_IP"
echo ""
echo "   记录类型：A"
echo "   主机记录：www"
echo "   记录值：$SERVER_IP"
echo ""
echo "3. 等待 DNS 生效（通常 5-10 分钟）"
echo ""
echo "4. 验证解析是否生效："
echo "   ping $DOMAIN_NAME"
echo ""
echo "5. 确保服务器安全组开放端口："
echo "   - 80 端口（HTTP，证书验证用）"
echo "   - 443 端口（HTTPS）"
echo ""
echo "=========================================="
echo ""
echo "数据库连接信息："
echo "  主机：$SERVER_IP"
echo "  端口：3306"
echo "  用户：$DB_USERNAME"
echo "  密码：$DB_PASSWORD"
echo ""
echo "运维附加文件："
[ -f "$SECURE_NGINX_TEMPLATE" ] && echo "  Nginx 安全模板：$SECURE_NGINX_TEMPLATE"
[ -f "$SECURITY_EVENT_SQL" ] && echo "  安全事件表脚本：$SECURITY_EVENT_SQL"
[ -f "$KEY_ROTATION_RUNBOOK" ] && echo "  密钥轮换指引：$KEY_ROTATION_RUNBOOK"
[ -f "$CLOUDFLARE_RUNBOOK" ] && echo "  Cloudflare 指引：$CLOUDFLARE_RUNBOOK"
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
