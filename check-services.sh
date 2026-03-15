#!/bin/bash

set -e

BACKEND_CONFIG_DIR="$(cd "$(dirname "$0")" && pwd)/blog-backend/src/main/resources"
RUNTIME_ENV_FILE="/opt/blog/blog-backend/.env"
RUNTIME_CONFIG_FILE="/opt/blog/application-prod.yml"
SOURCE_CONFIG_FILE="$BACKEND_CONFIG_DIR/application-prod.yml"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
MAGENTA='\033[1;35m'
NC='\033[0m'

STORAGE_TYPE=""
DOMAIN_NAME=""
SERVER_IP=""
MYSQL_PASSWORD="${DB_PASSWORD:-ojc132598.}"
REDIS_PASSWORD="${REDIS_PASSWORD:-ojc132598.}"

log_info() {
    echo -e "${GREEN}[√]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[!]${NC} $1"
}

log_error() {
    echo -e "${RED}[x]${NC} $1"
}

trim() {
    echo "$1" | sed 's/^[[:space:]]*//; s/[[:space:]]*$//'
}

load_env_file() {
    local env_file="$1"

    [ -f "$env_file" ] || return 1

    set -a
    # shellcheck disable=SC1090
    source "$env_file"
    set +a

    MYSQL_PASSWORD="${DB_PASSWORD:-$MYSQL_PASSWORD}"
    REDIS_PASSWORD="${REDIS_PASSWORD:-$REDIS_PASSWORD}"
    return 0
}

extract_storage_type_from_yaml() {
    local yaml_file="$1"
    local raw=""

    [ -f "$yaml_file" ] || return 1

    raw=$(awk '
        /^storage:[[:space:]]*$/ { in_storage=1; next }
        in_storage && /^[^[:space:]]/ { in_storage=0 }
        in_storage && /^[[:space:]]+type:[[:space:]]*/ {
            sub(/^[[:space:]]+type:[[:space:]]*/, "", $0)
            print $0
            exit
        }
    ' "$yaml_file")

    raw=$(trim "$raw")
    raw="${raw%\"}"
    raw="${raw#\"}"
    raw="${raw%\'}"
    raw="${raw#\'}"

    if [[ "$raw" =~ ^\$\{STORAGE_TYPE:([^}]+)\}$ ]]; then
        echo "${BASH_REMATCH[1]}"
        return 0
    fi

    [ -n "$raw" ] || return 1
    echo "$raw"
}

detect_storage_type() {
    if load_env_file "$RUNTIME_ENV_FILE" && [ -n "${STORAGE_TYPE:-}" ]; then
        return 0
    fi

    if [ -n "${STORAGE_TYPE:-}" ]; then
        return 0
    fi

    STORAGE_TYPE=$(extract_storage_type_from_yaml "$RUNTIME_CONFIG_FILE" || true)
    if [ -n "$STORAGE_TYPE" ]; then
        return 0
    fi

    STORAGE_TYPE=$(extract_storage_type_from_yaml "$SOURCE_CONFIG_FILE" || true)
    if [ -n "$STORAGE_TYPE" ]; then
        return 0
    fi

    STORAGE_TYPE="oss"
}

detect_server_ip() {
    SERVER_IP=$(timeout 3 curl -s ifconfig.me || timeout 3 curl -s icanhazip.com || timeout 3 curl -s ipinfo.io/ip || true)
    if [ -z "$SERVER_IP" ]; then
        SERVER_IP=$(ip -o -4 addr show | grep -v lo | awk '{print $4}' | cut -d'/' -f1 | head -n 1 || true)
    fi
    if [ -z "$SERVER_IP" ]; then
        SERVER_IP=$(hostname -I | awk '{print $1}' || true)
    fi
    if [ -z "$SERVER_IP" ]; then
        SERVER_IP="127.0.0.1"
    fi
}

detect_domain_name() {
    local config_path=""
    local domains=""
    local nginx_config_paths=(
        "/etc/nginx/sites-available/blog"
        "/etc/nginx/conf.d/blog.conf"
        "/etc/nginx/nginx.conf"
    )

    for config_path in "${nginx_config_paths[@]}"; do
        if [ -f "$config_path" ]; then
            domains=$(grep -E "^[[:space:]]*server_name" "$config_path" | sed -E 's/^[[:space:]]*server_name[[:space:]]+//' | sed 's/;$//' | tr ' ' '\n' | grep -v '^$' | grep -v '^_$' | grep -v '^localhost$' || true)
            if [ -n "$domains" ]; then
                DOMAIN_NAME=$(echo "$domains" | head -n 1)
                return 0
            fi
        fi
    done

    return 1
}

ensure_service_running() {
    local primary_service="$1"
    local display_name="$2"
    local secondary_service="${3:-}"

    if systemctl is-active --quiet "$primary_service"; then
        log_info "$display_name 运行正常"
        return 0
    fi

    if [ -n "$secondary_service" ] && systemctl is-active --quiet "$secondary_service"; then
        log_info "$display_name 运行正常"
        return 0
    fi

    log_error "$display_name 未运行"
    if systemctl start "$primary_service" >/dev/null 2>&1; then
        log_info "$display_name 已启动"
        return 0
    fi

    if [ -n "$secondary_service" ] && systemctl start "$secondary_service" >/dev/null 2>&1; then
        log_info "$display_name 已启动"
        return 0
    fi

    if [ -n "$secondary_service" ]; then
        echo "Failed to start ${primary_service}.service or ${secondary_service}.service"
    else
        echo "Failed to start ${primary_service}.service"
    fi
    return 1
}

check_port() {
    local port="$1"
    local name="$2"

    if command -v ss >/dev/null 2>&1; then
        if ss -tlnp 2>/dev/null | grep -q ":$port\\b"; then
            log_info "$name 端口 $port 正在监听"
        else
            log_warn "$name 端口 $port 未监听"
        fi
        return 0
    fi

    if netstat -tlnp 2>/dev/null | grep -q ":$port "; then
        log_info "$name 端口 $port 正在监听"
    else
        log_warn "$name 端口 $port 未监听"
    fi
}

echo "=========================================="
echo "  博客论坛系统 - 服务检查"
echo "=========================================="
echo

detect_server_ip
detect_storage_type
detect_domain_name || true

echo "=========================================="
echo -e "  服务器 IP: ${MAGENTA}${SERVER_IP}${NC}"
echo "=========================================="
echo

echo "===== 存储类型 ====="
log_info "当前存储类型：$STORAGE_TYPE"
echo

echo "===== 域名检测 ====="
if [ -n "$DOMAIN_NAME" ]; then
    log_info "检测到域名配置：$DOMAIN_NAME"
    if nslookup "$DOMAIN_NAME" >/dev/null 2>&1; then
        log_info "域名 $DOMAIN_NAME 解析正常"
    else
        log_warn "域名 $DOMAIN_NAME 解析失败"
    fi
else
    log_warn "未检测到域名配置，使用 IP 访问"
fi
echo

echo "===== 服务状态 ====="
ensure_service_running "mysql" "MySQL"
ensure_service_running "redis-server" "Redis" "redis"

if [ "$STORAGE_TYPE" = "minio" ]; then
    ensure_service_running "minio" "MinIO"
else
    log_warn "当前使用阿里云 OSS，跳过 MinIO 检查"
fi

ensure_service_running "blog-backend" "后端服务"
ensure_service_running "nginx" "Nginx"
echo

echo "===== 端口监听 ====="
check_port "3306" "MySQL"
check_port "6379" "Redis"
if [ "$STORAGE_TYPE" = "minio" ]; then
    check_port "9000" "MinIO"
    check_port "9001" "MinIO Console"
fi
check_port "8080" "后端服务"
check_port "80" "Nginx"
echo

echo "===== 连接测试 ====="
if mysql -uroot -p"$MYSQL_PASSWORD" -e "SELECT 1" >/dev/null 2>&1; then
    log_info "MySQL 连接成功"
else
    log_error "MySQL 连接失败"
fi

if redis-cli -a "$REDIS_PASSWORD" ping >/dev/null 2>&1; then
    log_info "Redis 连接成功"
else
    log_error "Redis 连接失败"
fi

if [ "$STORAGE_TYPE" = "minio" ]; then
    if curl -fsS http://localhost:9000/minio/health/live >/dev/null 2>&1; then
        log_info "MinIO 连接成功"
    else
        log_error "MinIO 连接失败"
    fi
else
    log_warn "当前使用阿里云 OSS，跳过 MinIO 连接测试"
fi

if curl -fsS http://localhost:8080/actuator/health >/dev/null 2>&1; then
    log_info "后端服务连接成功"
else
    log_warn "后端服务连接失败，可能仍在启动中"
fi
echo

echo "=========================================="
echo "  访问信息"
echo "=========================================="
echo

if [ -n "$DOMAIN_NAME" ]; then
    echo "网站地址："
    echo "  HTTP:  http://$DOMAIN_NAME"
    echo "  HTTPS: https://$DOMAIN_NAME"
else
    echo "网站地址：http://$SERVER_IP"
fi

if [ "$STORAGE_TYPE" = "minio" ]; then
    echo
    echo "MinIO 控制台：http://$SERVER_IP:9001"
fi

echo
echo "查看日志："
echo "  journalctl -u blog-backend -f"
echo "  tail -f /var/log/nginx/error.log"
echo
exit 0

: <<'LEGACY_CHECK_SERVICES_OLD'

# ============================================
# 博客论坛系统 - 服务检查和修复脚本
# ============================================

set -e

echo "=========================================="
echo "  博客论坛系统 - 服务检查"
echo "=========================================="

# 获取服务器 IP 地址
# 尝试获取公网 IP（添加超时设置）
SERVER_IP=$(timeout 3 curl -s ifconfig.me || timeout 3 curl -s icanhazip.com || timeout 3 curl -s ipinfo.io/ip)
if [ -z "$SERVER_IP" ]; then
    # 尝试获取内网 IP
    SERVER_IP=$(ip -o -4 addr show | grep -v lo | awk '{print $4}' | cut -d'/' -f1 | head -n 1)
fi
if [ -z "$SERVER_IP" ]; then
    SERVER_IP=$(hostname -I | awk '{print $1}')
fi
if [ -z "$SERVER_IP" ]; then
    SERVER_IP="127.0.0.1"
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
NC='\033[0m'

log_info() {
    echo -e "${GREEN}[✓]${NC} $1"
}

log_error() {
    echo -e "${RED}[✗]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[!]${NC} $1"
}

# 检测存储类型
BACKEND_CONFIG_DIR="blog-backend/src/main/resources"
STORAGE_TYPE="minio"

if [ -f "$BACKEND_CONFIG_DIR/application-prod.yml" ]; then
    DETECTED_STORAGE=$(grep -A 2 "^storage:" "$BACKEND_CONFIG_DIR/application-prod.yml" | grep "type:" | awk -F': ' '{print $2}' | tr -d ' ')
    if [ -n "$DETECTED_STORAGE" ]; then
        STORAGE_TYPE=$DETECTED_STORAGE
    fi
fi

echo ""
echo "===== 存储类型 ====="
log_info "当前存储类型：$STORAGE_TYPE"
echo ""

echo ""
echo "===== 域名检测 ====="
# 检查 Nginx 配置中的域名
DOMAIN_NAME=""
# 尝试多种 Nginx 配置文件路径
NGINX_CONFIG_PATHS=(
    "/etc/nginx/sites-available/blog"
    "/etc/nginx/conf.d/blog.conf"
    "/etc/nginx/nginx.conf"
)

for config_path in "${NGINX_CONFIG_PATHS[@]}"; do
    if [ -f "$config_path" ]; then
        # 提取所有 server_name 配置
        DOMAINS=$(grep -E "^\s*server_name" "$config_path" | awk '{print $2}' | cut -d';' -f1 | tr ' ' '\n')
        # 过滤掉默认服务器配置
        DOMAINS=$(echo "$DOMAINS" | grep -v "_" | grep -v "localhost")
        if [ -n "$DOMAINS" ]; then
            DOMAIN_NAME=$(echo "$DOMAINS" | head -n 1)
            break
        fi
    fi
done

if [ -n "$DOMAIN_NAME" ]; then
    log_info "检测到域名配置：$DOMAIN_NAME"
    # 验证域名解析（使用 DNS 解析而非 ping）
    if nslookup "$DOMAIN_NAME" >/dev/null 2>&1; then
        log_info "域名 $DOMAIN_NAME 解析正常"
    else
        log_warn "域名 $DOMAIN_NAME 解析失败"
    fi
else
    log_warn "未检测到域名配置，使用 IP 访问"
fi
echo ""

echo ""
echo "===== 服务状态 ====="

# 检查 MySQL
if systemctl is-active --quiet mysql; then
    log_info "MySQL 运行正常"
else
    log_error "MySQL 未运行"
    systemctl start mysql
fi

# 检查 Redis
if systemctl is-active --quiet redis-server || systemctl is-active --quiet redis; then
    log_info "Redis 运行正常"
else
    log_error "Redis 未运行"
    systemctl start redis-server || systemctl start redis
fi

# 检查 MinIO（仅当使用 MinIO 存储时）
if [ "$STORAGE_TYPE" = "minio" ]; then
    if systemctl is-active --quiet minio; then
        log_info "MinIO 运行正常"
    else
        log_error "MinIO 未运行"
        systemctl start minio
    fi
else
    log_warn "使用阿里云 OSS 存储，跳过 MinIO 检查"
fi

# 检查后端
if systemctl is-active --quiet blog-backend; then
    log_info "后端服务运行正常"
else
    log_error "后端服务未运行"
    systemctl start blog-backend
fi

# 检查 Nginx
if systemctl is-active --quiet nginx; then
    log_info "Nginx 运行正常"
else
    log_error "Nginx 未运行"
    systemctl start nginx
fi

echo ""
echo "===== 端口监听 ====="

# 根据存储类型动态配置端口检查
if [ "$STORAGE_TYPE" = "minio" ]; then
    ports=("3306:MySQL" "6379:Redis" "9000:MinIO" "9001:MinIO Console" "8080:Backend" "80:Nginx")
else
    ports=("3306:MySQL" "6379:Redis" "8080:Backend" "80:Nginx")
fi

for port_info in "${ports[@]}"; do
    port="${port_info%%:*}"
    name="${port_info##*:}"
    if netstat -tlnp 2>/dev/null | grep -q ":$port "; then
        log_info "$name (端口 $port) 正在监听"
    else
        log_warn "$name (端口 $port) 未监听"
    fi
done

echo ""
echo "===== 连接测试 ====="

# 测试 MySQL
if mysql -uroot -pojc132598. -e "SELECT 1" &>/dev/null; then
    log_info "MySQL 连接成功"
else
    log_error "MySQL 连接失败"
fi

# 测试 Redis
if redis-cli -a ojc132598. ping &>/dev/null; then
    log_info "Redis 连接成功"
else
    log_error "Redis 连接失败"
fi

# 测试 MinIO（仅当使用 MinIO 存储时）
if [ "$STORAGE_TYPE" = "minio" ]; then
    if curl -s http://localhost:9000/minio/health/live &>/dev/null; then
        log_info "MinIO 连接成功"
    else
        log_error "MinIO 连接失败"
    fi
else
    log_warn "使用阿里云 OSS 存储，跳过 MinIO 连接测试"
fi

# 测试后端
if curl -s http://localhost:8080/actuator/health &>/dev/null; then
    log_info "后端服务连接成功"
else
    log_warn "后端服务连接失败（可能正在启动）"
fi

echo ""
echo "=========================================="
echo "  访问信息"
echo "=========================================="
echo ""
if [ -n "$DOMAIN_NAME" ]; then
    echo "网站地址："
    echo "  HTTP: http://$DOMAIN_NAME"
    echo "  HTTPS: https://$DOMAIN_NAME"
else
    echo "网站地址：http://$SERVER_IP"
fi

# 仅在使用 MinIO 时显示 MinIO 控制台信息
if [ "$STORAGE_TYPE" = "minio" ]; then
    echo "MinIO 控制台：http://$SERVER_IP:9001"
    echo "  用户：minioadmin"
    echo "  密码：minioadmin"
    echo ""
fi

echo "数据库连接："
echo "  主机：$SERVER_IP"
echo "  端口：3306"
echo "  用户：root"
echo "  密码：ojc132598."
echo ""
echo "Redis 连接："
echo "  主机：$SERVER_IP"
echo "  端口：6379"
echo "  密码：ojc132598."
echo ""
echo "查看日志："
echo "  journalctl -u blog-backend -f"
echo "  tail -f /var/log/nginx/error.log"
echo ""

LEGACY_CHECK_SERVICES_OLD

