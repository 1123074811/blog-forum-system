#!/bin/bash

# ============================================
# 博客论坛系统 - 服务检查脚本（修复版）
# ============================================

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
MYSQL_PASSWORD=""
MYSQL_USERNAME="root"
REDIS_PASSWORD=""

log_info() {
    echo -e "${GREEN}[OK]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[!!]${NC} $1"
}

log_error() {
    echo -e "${RED}[XX]${NC} $1"
}

trim() {
    echo "$1" | sed 's/^[[:space:]]*//; s/[[:space:]]*$//'
}

load_env_file() {
    local env_file="$1"
    [ -f "$env_file" ] || return 1

    while IFS= read -r line || [ -n "$line" ]; do
        line="${line%$'\r'}"
        [[ "$line" =~ ^[[:space:]]*# ]] && continue
        [[ -z "${line// }" ]] && continue
        if [[ "$line" =~ ^[A-Za-z_][A-Za-z0-9_]*= ]]; then
            export "$line" 2>/dev/null || true
        fi
    done < "$env_file"

    MYSQL_PASSWORD="${DB_PASSWORD:-$MYSQL_PASSWORD}"
    MYSQL_USERNAME="${DB_USERNAME:-root}"
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
    STORAGE_TYPE=$(extract_storage_type_from_yaml "$RUNTIME_CONFIG_FILE" 2>/dev/null || true)
    if [ -n "$STORAGE_TYPE" ]; then
        return 0
    fi
    STORAGE_TYPE=$(extract_storage_type_from_yaml "$SOURCE_CONFIG_FILE" 2>/dev/null || true)
    if [ -n "$STORAGE_TYPE" ]; then
        return 0
    fi
    STORAGE_TYPE="oss"
}

detect_server_ip() {
    SERVER_IP=$(timeout 3 curl -s ifconfig.me 2>/dev/null || true)
    if [ -z "$SERVER_IP" ]; then
        SERVER_IP=$(timeout 3 curl -s icanhazip.com 2>/dev/null || true)
    fi
    if [ -z "$SERVER_IP" ]; then
        SERVER_IP=$(timeout 3 curl -s ipinfo.io/ip 2>/dev/null || true)
    fi
    if [ -z "$SERVER_IP" ]; then
        SERVER_IP=$(ip -o -4 addr show 2>/dev/null | grep -v lo | awk '{print $4}' | cut -d'/' -f1 | head -n 1 || true)
    fi
    if [ -z "$SERVER_IP" ]; then
        SERVER_IP=$(hostname -I 2>/dev/null | awk '{print $1}' || true)
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
            domains=$(grep -E "^[[:space:]]*server_name" "$config_path" \
                | sed -E 's/^[[:space:]]*server_name[[:space:]]+//' \
                | sed 's/;$//' \
                | tr ' ' '\n' \
                | grep -v '^$' \
                | grep -v '^_$' \
                | grep -v '^localhost$' \
                | grep -v '^127\.' \
                || true)
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

    if systemctl is-active --quiet "$primary_service" 2>/dev/null; then
        log_info "$display_name 运行正常"
        return 0
    fi
    if [ -n "$secondary_service" ] && systemctl is-active --quiet "$secondary_service" 2>/dev/null; then
        log_info "$display_name 运行正常 ($secondary_service)"
        return 0
    fi

    log_error "$display_name 未运行，尝试启动..."
    if systemctl start "$primary_service" >/dev/null 2>&1; then
        sleep 2
        if systemctl is-active --quiet "$primary_service" 2>/dev/null; then
            log_info "$display_name 启动成功"
            return 0
        fi
    fi
    if [ -n "$secondary_service" ] && systemctl start "$secondary_service" >/dev/null 2>&1; then
        sleep 2
        if systemctl is-active --quiet "$secondary_service" 2>/dev/null; then
            log_info "$display_name 启动成功 ($secondary_service)"
            return 0
        fi
    fi

    log_error "$display_name 启动失败，请手动检查：journalctl -u $primary_service -n 30 --no-pager"
    return 1
}

check_port() {
    local port="$1"
    local name="$2"

    if command -v ss >/dev/null 2>&1; then
        if ss -tlnp 2>/dev/null | grep -qE ":${port}[[:space:]]|:${port}$"; then
            log_info "$name 端口 $port 正在监听"
        else
            log_warn "$name 端口 $port 未监听"
        fi
        return 0
    fi
    if command -v netstat >/dev/null 2>&1; then
        if netstat -tlnp 2>/dev/null | grep -q ":$port "; then
            log_info "$name 端口 $port 正在监听"
        else
            log_warn "$name 端口 $port 未监听"
        fi
        return 0
    fi
    log_warn "无法检查端口（ss 和 netstat 均不可用）"
}

# ==================== 主流程 ====================

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
        log_warn "域名 $DOMAIN_NAME 解析失败，请检查 DNS 配置"
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
ensure_service_running "blog-music-api" "音乐服务(Meting API)"
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
check_port "3000" "音乐服务(Meting API)"
check_port "80"   "Nginx HTTP"
check_port "443"  "Nginx HTTPS"
echo

echo "===== 连接测试 ====="
if [ -n "$MYSQL_PASSWORD" ]; then
    if mysql -u"$MYSQL_USERNAME" -p"$MYSQL_PASSWORD" -e "SELECT 1" >/dev/null 2>&1; then
        log_info "MySQL 连接成功（用户：$MYSQL_USERNAME）"
    else
        log_error "MySQL 连接失败，请检查用户名/密码"
    fi
else
    if mysql -u"$MYSQL_USERNAME" -e "SELECT 1" >/dev/null 2>&1; then
        log_info "MySQL 连接成功（socket 认证）"
    else
        log_error "MySQL 连接失败（未读取到密码，请检查 $RUNTIME_ENV_FILE 中的 DB_PASSWORD）"
    fi
fi

if [ -n "$REDIS_PASSWORD" ]; then
    if redis-cli -a "$REDIS_PASSWORD" ping >/dev/null 2>&1; then
        log_info "Redis 连接成功"
    else
        log_error "Redis 连接失败，请检查密码或服务状态"
    fi
else
    if redis-cli ping >/dev/null 2>&1; then
        log_info "Redis 连接成功（无密码）"
    else
        log_error "Redis 连接失败（未读取到密码，请检查 $RUNTIME_ENV_FILE 中的 REDIS_PASSWORD）"
    fi
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
    log_warn "后端服务连接失败（可能仍在启动中，或 /actuator/health 未开放）"
    log_warn "查看详细日志：journalctl -u blog-backend -n 50 --no-pager"
fi

if curl -fsS "http://localhost:3000/search?keywords=test&limit=1&platform=netease" >/dev/null 2>&1; then
    log_info "音乐服务(Meting API)连接成功"
else
    log_warn "音乐服务(Meting API)连接失败（可能仍在启动中）"
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
echo "  journalctl -u blog-music-api -f"
echo "  tail -f /var/log/nginx/error.log"
echo

exit 0