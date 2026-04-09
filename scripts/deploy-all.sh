#!/bin/bash
# New-API 增强功能完整部署脚本

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

echo "=== New-API 增强功能部署 ==="
echo "项目目录: $PROJECT_ROOT"
echo ""

# 检查 Docker
if ! command -v docker &> /dev/null; then
    echo "错误: 未安装 Docker"
    exit 1
fi

# 检查 Docker Compose
if ! command -v docker-compose &> /dev/null && ! docker compose version &> /dev/null; then
    echo "错误: 未安装 Docker Compose"
    exit 1
fi

# 部署渠道状态监控
echo ">>> 部署渠道状态监控..."
bash "$SCRIPT_DIR/deploy-status.sh"

# 部署支付系统
echo ""
echo ">>> 部署支付系统..."
bash "$SCRIPT_DIR/deploy-paypro.sh"

# 部署主题
echo ""
echo ">>> 部署主题文件..."
bash "$SCRIPT_DIR/deploy-themes.sh"

echo ""
echo "=== 部署完成 ==="
echo ""
echo "下一步:"
echo "1. 配置 nginx - 参考 $PROJECT_ROOT/nginx/README.md"
echo "2. 访问 http://your-domain.com 查看主站"
echo "3. 访问 http://your-domain.com/status 查看渠道状态"
echo "4. 访问 http://your-domain.com/pay 访问支付系统"
