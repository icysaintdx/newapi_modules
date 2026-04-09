# New-API 增强功能详细部署指南

本文档提供完整的部署步骤，适合首次部署或在新服务器上部署。

## 环境要求

### 必需软件

- Docker 20.10+
- Docker Compose 2.0+
- Nginx 1.18+（需要 `http_sub_module` 模块）
- Git

### 系统要求

- Linux 服务器（推荐 Ubuntu 20.04+）
- 至少 4GB RAM
- 至少 20GB 磁盘空间

### 检查环境

```bash
# 检查 Docker
docker --version
docker-compose --version

# 检查 Nginx
nginx -v
nginx -V 2>&1 | grep -o http_sub_module

# 检查 Git
git --version
```

## 部署步骤

### 第一步：准备 New-API

确保 New-API 已经部署并正常运行。

```bash
# 如果还没有部署 New-API
cd /docker
git clone <new-api-repo>
cd new-api
docker-compose up -d

# 验证 New-API 运行
docker ps | grep new-api
curl http://localhost:3002/api/status
```

### 第二步：获取增强功能代码

```bash
# 克隆或复制本项目
cd /docker
git clone <this-repo-url> newapi-enhancements

# 或者如果已经有代码
cd /docker/newapi-enhancements
```

### 第三步：部署渠道状态监控

```bash
cd /docker/newapi-enhancements

# 运行部署脚本
bash scripts/deploy-status.sh

# 验证部署
docker ps | grep channel-status
curl http://localhost:2086
```

**配置说明：**

渠道状态监控需要连接 New-API 的 PostgreSQL 数据库。默认配置：

```bash
DATABASE_URL="postgresql://root:123456@postgres:5432/new-api?sslmode=disable"
```

如果你的数据库配置不同，修改 `scripts/deploy-status.sh` 中的环境变量。

### 第四步：部署支付系统

```bash
# 1. 配置支付系统
cd /docker/newapi-enhancements/paypro

# 2. 编辑配置文件
# 修改 src/main/resources/application.yml
# 配置支付宝、数据库、Redis 等信息
# 详细配置说明见 paypro/README.md

# 3. 运行部署脚本
bash ../scripts/deploy-paypro.sh

# 4. 验证部署
docker ps | grep paypro
curl http://localhost:8889
```

**重要配置项：**

- 支付宝收款二维码 URL
- 支付宝 userId
- 支付宝当面付 AppId 和密钥
- MySQL 数据库连接
- Redis 连接
- 邮件服务器配置

详细配置说明请查看 `paypro/README.md`。

### 第五步：配置 Nginx

#### 方式一：使用提供的配置文件（推荐）

```bash
# 1. 备份现有配置
cp /etc/nginx/nginx.conf /etc/nginx/nginx.conf.backup

# 2. 编辑主配置文件
nano /etc/nginx/nginx.conf

# 3. 在 http 块中添加
http {
    # ... 其他配置 ...
    
    # 引入 New-API 增强配置
    include /docker/newapi-enhancements/nginx/newapi.conf;
}

# 4. 测试配置
nginx -t

# 5. 重载 Nginx
nginx -s reload
```

#### 方式二：手动配置

创建 `/etc/nginx/sites-available/newapi.conf`：

```nginx
upstream newapi_backend {
    server localhost:3002;
}

upstream channel_status {
    server localhost:2086;
}

upstream paypro_backend {
    server localhost:8889;
}

server {
    listen 80;
    server_name your-domain.com;  # 修改为你的域名
    
    client_max_body_size 100M;
    
    # New-API 主站
    location / {
        proxy_pass http://newapi_backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # WebSocket 支持
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        
        # 主题注入（关键配置）
        sub_filter '</head>' '<link rel="stylesheet" href="/themes/theme-switcher.css"><script src="/themes/theme-switcher.js"></script></head>';
        sub_filter_once on;
        sub_filter_types text/html;
        
        # 必须关闭 gzip，否则 sub_filter 不生效
        proxy_set_header Accept-Encoding "";
    }
    
    # 主题文件
    location /themes/ {
        alias /docker/newapi-enhancements/themes/;
        expires 7d;
        add_header Cache-Control "public, immutable";
    }
    
    # 渠道状态监控
    location /status {
        proxy_pass http://channel_status;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        
        # 禁用缓存
        proxy_cache_bypass 1;
        proxy_no_cache 1;
        add_header Cache-Control "no-store, no-cache, must-revalidate";
    }
    
    # 支付系统
    location /pay {
        proxy_pass http://paypro_backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        
        # 支付回调需要较长超时
        proxy_connect_timeout 120s;
        proxy_send_timeout 120s;
        proxy_read_timeout 120s;
    }
}
```

启用配置：

```bash
# 创建软链接
ln -s /etc/nginx/sites-available/newapi.conf /etc/nginx/sites-enabled/

# 测试配置
nginx -t

# 重载 Nginx
systemctl reload nginx
```

### 第六步：配置 HTTPS（可选但推荐）

```bash
# 使用 Certbot 获取免费 SSL 证书
apt install certbot python3-certbot-nginx
certbot --nginx -d your-domain.com

# Certbot 会自动修改 nginx 配置并配置 HTTPS
```

### 第七步：验证部署

```bash
# 1. 检查所有容器运行状态
docker ps

# 应该看到：
# - new-api
# - postgres
# - redis
# - channel-status
# - paypro
# - paypro-mysql

# 2. 测试各个服务
curl http://your-domain.com/api/status          # New-API
curl http://your-domain.com/status              # 渠道状态
curl http://your-domain.com/pay                 # 支付系统

# 3. 在浏览器中访问
# http://your-domain.com - 主站（应该能看到主题切换器）
# http://your-domain.com/status - 渠道状态页
```

## Docker 网络配置

确保所有容器在同一网络中，以便互相通信。

```bash
# 查看 New-API 的网络
docker network ls | grep new-api

# 如果需要，将其他容器加入 New-API 网络
docker network connect new-api_new-api-network channel-status
docker network connect new-api_new-api-network paypro
```

## 完整的 Docker Compose（可选）

如果你想用一个 docker-compose 管理所有服务，可以创建：

`/docker/newapi-enhancements/docker-compose.full.yml`

```yaml
version: '3.8'

services:
  # 渠道状态监控
  channel-status:
    image: channel-status
    container_name: channel-status
    restart: unless-stopped
    ports:
      - "2086:8080"
    environment:
      - DATABASE_URL=postgresql://root:123456@postgres:5432/new-api?sslmode=disable
    networks:
      - new-api-network
    depends_on:
      - postgres

  # 支付系统
  paypro:
    build: ./paypro
    container_name: paypro
    restart: unless-stopped
    ports:
      - "8889:8889"
    depends_on:
      - paypro-mysql
      - redis
    networks:
      - new-api-network

  paypro-mysql:
    image: mysql:5.7
    container_name: paypro-mysql
    restart: unless-stopped
    environment:
      MYSQL_ROOT_PASSWORD: 123456
      MYSQL_DATABASE: paypro
    ports:
      - "3307:3306"
    volumes:
      - paypro-mysql-data:/var/lib/mysql
    networks:
      - new-api-network

networks:
  new-api-network:
    external: true
    name: new-api_new-api-network

volumes:
  paypro-mysql-data:
```

使用：

```bash
cd /docker/newapi-enhancements
docker-compose -f docker-compose.full.yml up -d
```

## 故障排查

### 主题不显示

1. 检查 nginx 配置中的 `sub_filter` 是否正确
2. 确认关闭了 gzip：`proxy_set_header Accept-Encoding "";`
3. 清除浏览器缓存
4. 检查主题文件路径是否正确：`ls /docker/newapi-enhancements/themes/`

### 渠道状态页 500 错误

1. 检查数据库连接：
   ```bash
   docker logs channel-status
   ```
2. 确认 PostgreSQL 可访问
3. 检查容器网络：
   ```bash
   docker network inspect new-api_new-api-network
   ```

### 支付系统无法启动

1. 检查配置文件：`paypro/src/main/resources/application.yml`
2. 确认 MySQL 和 Redis 正常运行
3. 查看日志：
   ```bash
   docker logs paypro
   ```

### Nginx 配置测试失败

```bash
# 查看详细错误
nginx -t

# 常见问题：
# - 缺少 http_sub_module：重新编译 nginx 或使用包含该模块的版本
# - 语法错误：检查配置文件语法
# - 文件路径错误：确认所有路径存在
```

## 维护和更新

### 更新 New-API

```bash
cd /docker/new-api
docker-compose pull
docker-compose up -d
```

增强功能不受影响。

### 更新渠道状态监控

```bash
cd /docker/newapi-enhancements/channel-status
# 修改 main.go
bash ../scripts/deploy-status.sh
```

### 更新支付系统

```bash
cd /docker/newapi-enhancements/paypro
# 修改代码
bash ../scripts/deploy-paypro.sh
```

### 更新主题

```bash
# 直接修改主题文件
cd /docker/newapi-enhancements/themes/
# 修改后无需重启，刷新浏览器即可
```

## 备份

```bash
# 备份整个项目
tar -czf newapi-enhancements-backup-$(date +%Y%m%d).tar.gz /docker/newapi-enhancements

# 备份数据库
docker exec postgres pg_dump -U root new-api > new-api-backup-$(date +%Y%m%d).sql
docker exec paypro-mysql mysqldump -uroot -p123456 paypro > paypro-backup-$(date +%Y%m%d).sql
```

## 性能优化

### Nginx 缓存

```nginx
# 在 http 块中添加
proxy_cache_path /var/cache/nginx levels=1:2 keys_zone=newapi_cache:10m max_size=1g inactive=60m;

# 在 location / 中添加
proxy_cache newapi_cache;
proxy_cache_valid 200 10m;
proxy_cache_bypass $http_cache_control;
```

### 数据库优化

- 为 PostgreSQL 和 MySQL 配置合适的内存和连接池
- 定期清理日志和临时文件
- 监控数据库性能

## 安全建议

1. 使用 HTTPS（Let's Encrypt 免费证书）
2. 配置防火墙，只开放必要端口
3. 定期更新 Docker 镜像
4. 使用强密码
5. 限制数据库只能从本地访问
6. 配置 fail2ban 防止暴力破解

## 下一步

- 配置监控和告警（Prometheus + Grafana）
- 配置日志收集（ELK Stack）
- 配置自动备份
- 配置 CDN 加速静态资源

## 获取帮助

如有问题，请查看：
- `nginx/README.md` - Nginx 配置说明
- `channel-status/README.md` - 渠道状态监控说明
- `paypro/README.md` - 支付系统说明
- 或提交 Issue
