# New-API 渠道状态监控

实时监控 New-API 所有渠道的状态，支持多种模型类型统计。

## 功能特性

- ✅ 实时显示所有渠道状态（启用/禁用）
- ✅ 按模型类型分组统计
- ✅ 显示渠道优先级和权重
- ✅ 响应式设计，支持移动端
- ✅ 自动刷新（可配置）
- ✅ 独立容器部署，不影响 New-API

## 技术栈

- Go 1.21
- PostgreSQL（连接 New-API 数据库）
- 纯 HTML/CSS/JS 前端

## 部署

### 快速部署

```bash
cd /docker/newapi-enhancements
bash scripts/deploy-status.sh
```

### 手动部署

```bash
cd /docker/newapi-enhancements/channel-status

# 1. 编译 Go 程序
docker run --rm -v $(pwd):/app -w /app golang:1.21-alpine sh -c "go mod download && go build -o channel-status main.go"

# 2. 构建镜像
docker build -t channel-status .

# 3. 启动容器
docker run -d \
    --name channel-status \
    --network new-api_new-api-network \
    -p 2086:8080 \
    -e DATABASE_URL="postgresql://root:123456@postgres:5432/new-api?sslmode=disable" \
    --restart unless-stopped \
    channel-status
```

## 配置

### 环境变量

- `DATABASE_URL` - PostgreSQL 连接字符串（必需）
  - 格式：`postgresql://用户名:密码@主机:端口/数据库?sslmode=disable`
  - 默认：`postgresql://root:123456@postgres:5432/new-api?sslmode=disable`

### 修改数据库连接

如果你的 New-API 使用不同的数据库配置，修改 `scripts/deploy-status.sh` 中的 `DATABASE_URL`。

## Nginx 配置

### 作为子路径

```nginx
location /status {
    proxy_pass http://channel-status:8080;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    
    # 禁用缓存，确保实时数据
    proxy_cache_bypass 1;
    proxy_no_cache 1;
    add_header Cache-Control "no-store, no-cache, must-revalidate";
}
```

### 作为独立域名

```nginx
server {
    listen 80;
    server_name status.your-domain.com;
    
    location / {
        proxy_pass http://channel-status:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
}
```

## 访问

- 直接访问：`http://localhost:2086`
- 通过 nginx：`http://your-domain.com/status`

## 数据库表结构

程序读取 New-API 的 `channels` 表：

```sql
SELECT 
    id, 
    name, 
    type, 
    status, 
    models, 
    priority, 
    weight 
FROM channels 
ORDER BY priority DESC, id ASC;
```

## 故障排查

### 容器无法启动

```bash
# 查看日志
docker logs channel-status

# 常见问题：
# 1. 数据库连接失败 - 检查 DATABASE_URL
# 2. 端口被占用 - 修改端口映射
# 3. 网络问题 - 确认容器在正确的 Docker 网络中
```

### 显示 500 错误

1. 检查数据库连接：
   ```bash
   docker exec -it postgres psql -U root -d new-api -c "SELECT COUNT(*) FROM channels;"
   ```

2. 检查容器网络：
   ```bash
   docker network inspect new-api_new-api-network
   ```

3. 查看详细日志：
   ```bash
   docker logs -f channel-status
   ```

### 数据不更新

1. 检查是否禁用了缓存（nginx 配置）
2. 清除浏览器缓存
3. 检查数据库中的数据是否正确

## 开发

### 本地开发

```bash
# 安装依赖
go mod download

# 设置环境变量
export DATABASE_URL="postgresql://root:123456@localhost:5432/new-api?sslmode=disable"

# 运行
go run main.go

# 访问
curl http://localhost:8080
```

### 修改代码

1. 编辑 `main.go`
2. 重新编译和部署：
   ```bash
   bash ../scripts/deploy-status.sh
   ```

### 添加新功能

主要代码结构：

```go
// 数据库连接
db, err := sql.Open("postgres", databaseURL)

// 查询渠道数据
rows, err := db.Query("SELECT ...")

// HTTP 服务器
http.HandleFunc("/", handleStatus)
http.ListenAndServe(":8080", nil)
```

## 性能

- 每次请求实时查询数据库
- 响应时间 < 100ms（取决于数据库性能）
- 支持并发访问
- 内存占用 < 20MB

## 安全

- 只读数据库访问（SELECT 权限）
- 不暴露敏感信息（API 密钥等）
- 可配置访问控制（通过 nginx）

## 更新

```bash
# 拉取最新代码
cd /docker/newapi-enhancements
git pull

# 重新部署
bash scripts/deploy-status.sh
```

## 卸载

```bash
# 停止并删除容器
docker stop channel-status
docker rm channel-status

# 删除镜像
docker rmi channel-status
```
