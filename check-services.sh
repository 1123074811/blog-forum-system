#!/bin/bash

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
