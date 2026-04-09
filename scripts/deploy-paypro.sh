#!/bin/bash
# PayPro 支付系统部署脚本

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
PAYPRO_DIR="$PROJECT_ROOT/paypro"

echo "部署 PayPro 支付系统..."

cd "$PAYPRO_DIR"

# 检查 docker-compose.yml
if [ ! -f "docker-compose.yml" ]; then
    echo "错误: 找不到 docker-compose.yml"
    exit 1
fi

# 使用 docker-compose 部署
echo "启动 PayPro 容器..."
docker-compose up -d

echo ""
echo "PayPro 支付系统部署完成！"
echo "访问: http://localhost:8889"
echo ""
echo "注意: 请确保已配置以下内容:"
echo "1. 修改 src/main/resources/application.yml 中的支付宝配置"
echo "2. 配置数据库连接 (MySQL)"
echo "3. 配置 Redis 连接"
echo "详细配置说明请查看: $PAYPRO_DIR/README.md"
