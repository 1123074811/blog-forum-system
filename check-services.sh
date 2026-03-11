#!/bin/bash

# ============================================
# 博客论坛系统 - 服务检查和修复脚本
# ============================================

set -e

echo "=========================================="
echo "  博客论坛系统 - 服务检查"
echo "=========================================="

# 获取服务器 IP 地址
SERVER_IP=$(hostname -I | awk '{print $1}')
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

# 检查 MinIO
if systemctl is-active --quiet minio; then
    log_info "MinIO 运行正常"
else
    log_error "MinIO 未运行"
    systemctl start minio
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

ports=("3306:MySQL" "6379:Redis" "9000:MinIO" "9001:MinIO Console" "8080:Backend" "80:Nginx")
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

# 测试 MinIO
if curl -s http://localhost:9000/minio/health/live &>/dev/null; then
    log_info "MinIO 连接成功"
else
    log_error "MinIO 连接失败"
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
echo "网站地址：http://$SERVER_IP"
echo "MinIO 控制台：http://$SERVER_IP:9001"
echo "  用户：minioadmin"
echo "  密码：minioadmin"
echo ""
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
