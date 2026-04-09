#!/bin/bash
# 渠道状态监控部署脚本

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
STATUS_DIR="$PROJECT_ROOT/channel-status"

echo "部署渠道状态监控..."

cd "$STATUS_DIR"

# 检查必要文件
if [ ! -f "main.go" ]; then
    echo "错误: 找不到 main.go"
    exit 1
fi

# 编译 Go 程序
echo "编译 Go 程序..."
docker run --rm -v "$STATUS_DIR:/app" -w /app golang:1.21-alpine sh -c "go mod download && go build -o channel-status main.go"

# 构建镜像
echo "构建 Docker 镜像..."
docker build -t channel-status .

# 停止旧容器
if docker ps -a | grep -q channel-status; then
    echo "停止旧容器..."
    docker stop channel-status || true
    docker rm channel-status || true
fi

# 启动新容器
echo "启动容器..."
docker run -d \
    --name channel-status \
    --network new-api_new-api-network \
    -p 2086:8080 \
    -e DATABASE_URL="postgresql://root:123456@postgres:5432/new-api?sslmode=disable" \
    --restart unless-stopped \
    channel-status

echo "渠道状态监控部署完成！"
echo "访问: http://localhost:2086"
