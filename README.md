# New-API 增强功能套件

这是一个完全外部化的 New-API 增强功能集合，包括主题系统、渠道状态监控和支付系统。所有功能都通过 nginx 注入实现，**完全不修改 New-API 容器内部文件**，确保 New-API 更新时不会产生冲突。

## 核心原则

- ✅ **完全外部化** - 所有优化都在外部实现，不修改 New-API 容器
- ✅ **模块化设计** - 每个功能独立部署，可按需启用
- ✅ **易于迁移** - 整个项目可直接推送到 Git，在其他服务器克隆使用
- ✅ **更新友好** - New-API 容器更新不影响增强功能

## 功能模块

### 1. 主题系统 (`themes/`)
- 6 套精美主题：动画、卡通、暗色、亮色、像素、科技
- 主题切换器，用户可自由切换
- 通过 nginx 注入，无需修改 New-API 代码

### 2. 渠道状态监控 (`channel-status/`)
- 实时显示所有 API 渠道状态
- 支持多种模型类型统计
- Go 语言开发，独立容器部署

### 3. 支付系统 (`paypro/`)
- 支持支付宝当面付
- 自动充值到 New-API 账户
- Java Spring Boot 开发

## 快速开始

### 前置要求

- Docker & Docker Compose
- Nginx（用于代理和注入）
- 已部署的 New-API 实例

### 一键部署

```bash
cd /docker/newapi-enhancements
bash scripts/deploy-all.sh
```

### 分步部署

```bash
# 1. 部署渠道状态监控
bash scripts/deploy-status.sh

# 2. 部署支付系统
bash scripts/deploy-paypro.sh

# 3. 部署主题（配置 nginx）
bash scripts/deploy-themes.sh
```

### 配置 Nginx

参考 `nginx/README.md` 配置 nginx 代理和注入。

基本配置：

```nginx
# 引入所有配置
include /docker/newapi-enhancements/nginx/*.conf;

# 或者手动配置
server {
    listen 80;
    server_name your-domain.com;
    
    # New-API 主站
    location / {
        proxy_pass http://new-api:3000;
        # ... 其他配置
    }
    
    # 主题文件
    location /themes/ {
        alias /docker/newapi-enhancements/themes/;
    }
    
    # 渠道状态
    location /status {
        proxy_pass http://channel-status:8080;
    }
    
    # 支付系统
    location /pay {
        proxy_pass http://paypro:8889;
    }
}
```

## 目录结构

```
newapi-enhancements/
├── README.md                 # 本文件
├── DEPLOYMENT.md             # 详细部署指南
├── docker-compose.full.yml   # 完整 docker-compose（可选）
│
├── themes/                   # 主题模块
│   ├── README.md
│   ├── animated/
│   ├── cartoon/
│   ├── dark/
│   ├── light/
│   ├── pixel/
│   └── tech/
│
├── channel-status/           # 状态监控模块
│   ├── README.md
│   ├── Dockerfile
│   ├── main.go
│   └── ...
│
├── paypro/                   # 支付模块
│   ├── README.md
│   ├── Dockerfile
│   ├── docker-compose.yml
│   ├── src/
│   └── newapi-integration/   # New-API 集成文件
│
├── nginx/                    # Nginx 配置
│   ├── README.md
│   ├── newapi.conf
│   ├── themes-inject.conf
│   ├── status-proxy.conf
│   └── paypro-proxy.conf
│
└── scripts/                  # 部署脚本
    ├── deploy-all.sh
    ├── deploy-themes.sh
    ├── deploy-status.sh
    └── deploy-paypro.sh
```

## 访问地址

部署完成后：

- 主站：`http://your-domain.com`
- 渠道状态：`http://your-domain.com/status`
- 支付系统：`http://your-domain.com/pay`

## 更新 New-API

当 New-API 有新版本时：

```bash
cd /docker/new-api
docker-compose pull
docker-compose up -d
```

增强功能不受影响，无需重新配置。

## 迁移到其他服务器

```bash
# 在新服务器上
git clone <your-repo-url>
cd newapi-enhancements
bash scripts/deploy-all.sh

# 配置 nginx（参考 nginx/README.md）
```

## 技术栈

- **主题系统**: HTML/CSS/JavaScript
- **渠道状态**: Go 1.21 + PostgreSQL
- **支付系统**: Java 8 + Spring Boot + MySQL + Redis
- **代理注入**: Nginx

## 常见问题

### 主题不生效？

1. 检查 nginx 是否正确配置 `sub_filter`
2. 清除浏览器缓存
3. 确认 nginx 关闭了 gzip（`proxy_set_header Accept-Encoding "";`）

### 渠道状态页显示错误？

1. 检查数据库连接配置
2. 确认容器在同一 Docker 网络
3. 查看容器日志：`docker logs channel-status`

### 支付系统无法访问？

1. 检查 MySQL 和 Redis 是否正常运行
2. 确认 `application.yml` 配置正确
3. 查看容器日志：`docker logs paypro`

### 浏览器标签页图标不对？

1. 确认图标文件已复制到 `/docker/newapi-enhancements/themes/`
2. 检查 nginx 配置了 `/themes/` 路径
3. **清除浏览器缓存**（Ctrl+Shift+Delete）
4. 查看详细解决方案：[MIGRATION-ISSUES.md](MIGRATION-ISSUES.md)

## 迁移问题排查

如果从旧目录迁移遇到问题，请查看 [MIGRATION-ISSUES.md](MIGRATION-ISSUES.md)，其中记录了所有已知问题和解决方案。

## 贡献

欢迎提交 Issue 和 Pull Request。

## 许可

MIT License
