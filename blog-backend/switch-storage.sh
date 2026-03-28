#!/bin/bash

# ============================================
# 存储方式切换脚本
# ============================================
# 用法：
#   ./switch-storage.sh minio   # 切换到 MinIO
#   ./switch-storage.sh oss     # 切换到阿里云 OSS
# ============================================

STORAGE_TYPE=$1
ENV_FILE=".env"

if [ -z "$STORAGE_TYPE" ]; then
    echo "错误：请指定存储类型"
    echo "用法: $0 [minio|oss]"
    exit 1
fi

if [ "$STORAGE_TYPE" != "minio" ] && [ "$STORAGE_TYPE" != "oss" ]; then
    echo "错误：存储类型必须是 'minio' 或 'oss'"
    exit 1
fi

if [ ! -f "$ENV_FILE" ]; then
    echo "错误：找不到 .env 文件"
    echo "请先从 .env.example 复制并配置 .env 文件"
    exit 1
fi

# 备份原配置
cp "$ENV_FILE" "${ENV_FILE}.backup.$(date +%Y%m%d_%H%M%S)"
echo "已备份原配置到 ${ENV_FILE}.backup.$(date +%Y%m%d_%H%M%S)"

# 更新存储类型
if grep -q "^STORAGE_TYPE=" "$ENV_FILE"; then
    # 如果存在，则替换
    sed -i "s/^STORAGE_TYPE=.*/STORAGE_TYPE=$STORAGE_TYPE/" "$ENV_FILE"
else
    # 如果不存在，则添加
    echo "STORAGE_TYPE=$STORAGE_TYPE" >> "$ENV_FILE"
fi

echo "✓ 已切换存储类型为: $STORAGE_TYPE"

if [ "$STORAGE_TYPE" = "minio" ]; then
    echo ""
    echo "当前使用 MinIO 存储"
    echo "请确保以下配置正确："
    echo "  - MINIO_ENDPOINT"
    echo "  - MINIO_ACCESS_KEY"
    echo "  - MINIO_SECRET_KEY"
    echo "  - MINIO_BUCKET"
elif [ "$STORAGE_TYPE" = "oss" ]; then
    echo ""
    echo "当前使用阿里云 OSS 存储"
    echo "请确保以下配置正确："
    echo "  - ALIYUN_OSS_ENDPOINT"
    echo "  - ALIYUN_OSS_ACCESS_KEY_ID"
    echo "  - ALIYUN_OSS_ACCESS_KEY_SECRET"
    echo "  - ALIYUN_OSS_BUCKET"
    echo "  - ALIYUN_OSS_CUSTOM_DOMAIN (可选)"
fi

echo ""
echo "请重启应用以使配置生效"
