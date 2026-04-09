# New-API 定制化部署文档

> 最后更新: 2026-04-05
> 适用版本: new-api v0.12.1+（calciumion/new-api:latest）

## 一、核心设计原则

**零侵入、全外挂**：所有定制化修改都在 Docker 容器外部通过 Nginx 实现，不修改 new-api 任何源码或容器内部文件。new-api 仓库更新时只需 `docker compose pull && docker compose up -d`，所有定制配置不受影响。

## 二、系统架构

```
                      ┌─ location = /              → newapi_themes/$theme/home.html（静态）
                      ├─ location = /login          → newapi_themes/$theme/login.html（静态）
                      ├─ location = /register       → newapi_themes/$theme/register.html（静态）
                      ├─ location = /reset          → newapi_themes/$theme/reset.html（静态）
                      ├─ location = /about          → newapi_themes/$theme/about.html（静态）
用户请求 → Nginx ─────├─ location = /api/channel-status → channel-status 容器（:2086）
  (Cookie: saint_theme)├─ location /status/          → channel-status 容器（:2086）
                      ├─ location /api              → new-api 容器（:3002）纯代理，不修改
                      └─ location /                 → new-api 容器（:3002）+ sub_filter 注入主题JS
```

### 三大定制层

| 层 | 作用 | 实现方式 | 影响范围 |
|----|------|---------|---------|
| **公开页面** | 首页/登录/注册等完全自定义 | Nginx rewrite → 静态HTML | 访客可见页面 |
| **SPA 主题注入** | 控制台等 SPA 页面换肤 | Nginx sub_filter 注入 theme-switcher.js | 已登录用户页面 |
| **渠道状态监控** | 独立的渠道状态页 | 独立 Go 服务 + Nginx 反代 | /status/ 路径 |

## 三、目录结构

```
/docker/
├── new-api/
│   ├── docker-compose.yml          # new-api + postgres + redis
│   ├── data/                       # new-api 数据目录（持久化）
│   ├── logs/                       # new-api 日志
│   └── newapi_themes/              # ★ 主题文件目录（核心）
│       ├── DEPLOYMENT.md           # 本文档
│       ├── theme-switcher.js       # 主题切换核心脚本
│       ├── status.html             # 渠道状态页面模板
│       ├── dark/                   # 深色主题
│       │   ├── home.html
│       │   ├── login.html
│       │   ├── register.html
│       │   ├── reset.html
│       │   ├── about.html
│       │   ├── announcement.html
│       │   ├── privacy.html
│       │   └── spa.css
│       ├── light/                  # 浅色主题（同上结构）
│       ├── tech/                   # 科技主题
│       ├── cartoon/                # 卡通主题
│       ├── pixel/                  # 像素主题
│       └── animated/              # 动画主题（含额外的 topup.html、characters.js）
│
└── channel-status/
    ├── main.go                     # 渠道状态监控 Go 源码
    ├── go.mod
    ├── go.sum
    ├── Dockerfile
    └── channel-status              # 编译后的二进制
```

## 四、Docker Compose 配置

### new-api（`/docker/new-api/docker-compose.yml`）

```yaml
version: '3.4'

services:
  new-api:
    image: calciumion/new-api:latest
    container_name: new-api
    restart: always
    command: --log-dir /app/logs
    ports:
      - "3002:3000"                # 宿主机端口 3002
    volumes:
      - ./data:/data
      - ./logs:/app/logs
    environment:
      - SQL_DSN=postgresql://root:改成你的密码@postgres:5432/new-api
      - REDIS_CONN_STRING=redis://redis
      - TZ=Asia/Shanghai
      - ERROR_LOG_ENABLED=true
      - BATCH_UPDATE_ENABLED=true
      - SESSION_SECRET=改成随机字符串    # 重要！多机部署必须一致
    depends_on:
      - redis
      - postgres
    networks:
      - new-api-network
    healthcheck:
      test: ["CMD-SHELL", "wget -q -O - http://localhost:3000/api/status | grep -o '\"success\":\\s*true' || exit 1"]
      interval: 30s
      timeout: 10s
      retries: 3

  redis:
    image: redis:latest
    container_name: redis
    restart: always
    networks:
      - new-api-network

  postgres:
    image: postgres:15
    container_name: postgres
    restart: always
    environment:
      POSTGRES_USER: root
      POSTGRES_PASSWORD: 改成你的密码    # 与 SQL_DSN 中一致
      POSTGRES_DB: new-api
    volumes:
      - pg_data:/var/lib/postgresql/data
    networks:
      - new-api-network

volumes:
  pg_data:

networks:
  new-api-network:
    driver: bridge
```

### channel-status（独立容器）

```bash
# 构建
cd /docker/channel-status
docker build -t channel-status .

# 运行（必须加入 new-api 的网络才能访问 postgres）
docker run -d \
  --name channel-status \
  --restart always \
  --network new-api_new-api-network \
  -p 2086:8080 \
  -e DATABASE_URL="postgresql://root:改成你的密码@postgres:5432/new-api?sslmode=disable" \
  channel-status
```

> **注意**：channel-status 对数据库是**只读**的，只查询 `channels` 表，不写入任何数据。

## 五、Nginx 完整配置

以下是 `/etc/nginx/sites-available/your-domain` 的完整配置，可直接使用（替换域名和路径）：

```nginx
# ============================================================
# 主题路由：根据 Cookie 'saint_theme' 决定主题
# 支持的值: dark, light, tech, cartoon, pixel, animated
# 默认: dark
# ============================================================
map $cookie_saint_theme $theme {
    default "dark";
    "~^(dark|light|tech|cartoon|pixel|animated)$" $1;
}

# ============================================================
# 上游服务
# ============================================================
upstream channel_status {
    server 127.0.0.1:2086;       # channel-status 容器
}

upstream new_api_backend {
    server 127.0.0.1:3002;       # new-api 容器
}

# ============================================================
# HTTP → HTTPS 重定向
# ============================================================
server {
    listen 80;
    server_name your-domain.com www.your-domain.com;
    return 301 https://$host$request_uri;
}

# ============================================================
# 主站 HTTPS
# ============================================================
server {
    listen 443 ssl http2;
    server_name your-domain.com www.your-domain.com;
    ssl_certificate /etc/letsencrypt/live/your-domain.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/your-domain.com/privkey.pem;

    # ----------------------------------------------------------
    # 1. 主题静态资源（JS + CSS）
    #    访问路径: /theme/dark/spa.css, /theme/theme-switcher.js
    # ----------------------------------------------------------
    location ^~ /theme/ {
        alias /docker/new-api/newapi_themes/;
        try_files $uri =404;
    }

    # ----------------------------------------------------------
    # 2. 主题 HTML 内部路由（被 rewrite 指向）
    #    禁止缓存，防止主题混乱
    # ----------------------------------------------------------
    location ^~ /newapi_themes/ {
        alias /docker/new-api/newapi_themes/;
        try_files $uri =404;
        add_header X-Design-Theme $theme;
        add_header Cache-Control "no-cache, no-store, must-revalidate";
        add_header Pragma "no-cache";
        add_header Expires "0";
        add_header Vary "Cookie";
    }

    # ----------------------------------------------------------
    # 3. 公开展示页面 → 根据 Cookie 返回对应主题 HTML
    # ----------------------------------------------------------
    location = /            { rewrite ^ /newapi_themes/$theme/home.html last; }
    location = /about       { rewrite ^ /newapi_themes/$theme/about.html last; }
    location = /announcement { rewrite ^ /newapi_themes/$theme/announcement.html last; }
    location = /privacy     { rewrite ^ /newapi_themes/$theme/privacy.html last; }

    # ----------------------------------------------------------
    # 4. 认证页面 → 自定义主题页面（与 SPA localStorage 兼容）
    #
    #    ★★★ 关键注意事项 ★★★
    #    login.html 中的 JS 登录逻辑必须这样写：
    #
    #      fetch('/api/user/login', {...})
    #        .then(r => r.json())
    #        .then(d => {
    #          if(d.success) {
    #            localStorage.setItem('user', JSON.stringify(d.data));  // ← 必须直接存 d.data
    #            window.location.href = '/console';
    #          }
    #        });
    #
    #    不要额外调 /api/user/self，直接存 login 返回的 d.data。
    #    SPA 启动时从 localStorage.getItem('user') 恢复认证状态，
    #    只要 d.data 包含 token 字段就能正常工作。
    # ----------------------------------------------------------
    location = /login    { rewrite ^ /newapi_themes/$theme/login.html last; }
    location = /register { rewrite ^ /newapi_themes/$theme/register.html last; }
    location = /reset    { rewrite ^ /newapi_themes/$theme/reset.html last; }
    location = /topup    { rewrite ^ /newapi_themes/animated/topup.html last; }

    # ----------------------------------------------------------
    # 5. 渠道状态监控页面 → channel-status 容器
    #    sub_filter 将页面中的 /api/status 替换为 /api/channel-status
    #    以避免与 new-api 自身的 /api/status 冲突
    # ----------------------------------------------------------
    location /status/ {
        proxy_pass http://channel_status/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header Accept-Encoding "";

        sub_filter_types text/html;
        sub_filter_once off;
        sub_filter "/api/status" "/api/channel-status";
    }

    # ----------------------------------------------------------
    # 6. 渠道状态 API → channel-status 容器
    #
    #    ★★★ 关键注意事项 ★★★
    #    绝对不能用 /api/status 这个路径！
    #    new-api 自身有 /api/status 接口，SPA 依赖它获取系统配置
    #    （chats、quota_per_unit、display_in_currency 等）。
    #    如果被劫持，会导致：
    #      - 额度显示 $NaN
    #      - "聊天数据解析失败"
    #      - "聊天链接配置错误"
    # ----------------------------------------------------------
    location = /api/channel-status {
        proxy_pass http://channel_status/api/status;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }

    # ----------------------------------------------------------
    # 7. API 路由 → new-api 后端（纯代理，不做任何修改）
    #    包括 /api/status, /api/user/login, /api/user/self 等
    # ----------------------------------------------------------
    location /api {
        proxy_pass http://new_api_backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # ----------------------------------------------------------
    # 8. 其他所有路由 → new-api SPA + 注入主题脚本
    #    sub_filter 向 SPA 的 HTML 页面注入 theme-switcher.js
    #
    #    ★ sub_filter_types text/html → 仅对 HTML 生效，不碰 JSON
    #    ★ Accept-Encoding "" → 禁用 gzip 使 sub_filter 能匹配文本
    #    ★ 这两行缺一不可
    # ----------------------------------------------------------
    location / {
        proxy_pass http://new_api_backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header Accept-Encoding "";
        proxy_connect_timeout 300s;
        proxy_read_timeout 300s;
        proxy_send_timeout 300s;

        sub_filter_types text/html;
        sub_filter_once on;
        sub_filter "</head>" "<script src=\"/theme/theme-switcher.js\"></script></head>";
    }

    # ----------------------------------------------------------
    # 9. 静态资源 → new-api 后端（不经过 sub_filter）
    # ----------------------------------------------------------
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2|ttf|eot)$ {
        proxy_pass http://new_api_backend;
        proxy_set_header Host $host;
        proxy_buffering off;
    }
}
```

## 六、Nginx 路由优先级说明

Nginx location 匹配的优先级决定了整个系统能否正常工作：

```
优先级从高到低：
1. location = /api/channel-status    精确匹配 → channel-status 服务
2. location = /login                 精确匹配 → 主题 login.html
3. location = /register              精确匹配 → 主题 register.html
4. location = /                      精确匹配 → 主题 home.html
   ... (其他 = 精确匹配)
5. location ^~ /theme/               前缀匹配 → 主题静态资源
6. location ^~ /newapi_themes/       前缀匹配 → 主题 HTML 内部路由
7. location ~* \.(js|css|...)$       正则匹配 → new-api 静态资源
8. location /status/                 前缀匹配 → channel-status 页面
9. location /api                     前缀匹配 → new-api API（纯代理）
10. location /                       通用匹配 → new-api SPA（含 sub_filter）
```

## 七、已踩过的坑（必读）

### 坑 1：/api/status 路由冲突

**症状**：控制台显示 `$NaN`、"聊天数据解析失败"、"聊天链接配置错误"

**原因**：如果 channel-status 的 API 用 `location = /api/status`，精确匹配优先级高于 `location /api` 的前缀匹配，会劫持 new-api 自身的 `/api/status` 接口。SPA 依赖这个接口获取 `quota_per_unit`、`chats` 等系统配置。

**解决**：channel-status API 路径必须用 `/api/channel-status`，通过 `proxy_pass` 内部转发到容器的 `/api/status`。

### 坑 2：自定义 login.html 登录后跳转循环

**症状**：登录成功 → 跳到 /console → 秒跳回 /login → 循环

**原因**：自定义 login.html 登录成功后调了 `/api/user/self` 再存 localStorage，但此时 session cookie 可能还未生效，导致 self 请求失败或返回不完整数据。SPA 启动后检测到 `localStorage.user` 无效（缺少 token 字段），跳回 login。

**解决**：登录成功后直接存 login API 返回的 `d.data`，不要额外调 `/api/user/self`。这和 SPA 原生的登录逻辑完全一致。

```javascript
// ✅ 正确写法
if(d.success) {
    if(d.data) { localStorage.setItem('user', JSON.stringify(d.data)); }
    window.location.href = '/console';
}

// ❌ 错误写法（之前的bug）
if(d.success) {
    fetch('/api/user/self').then(r2 => r2.json()).then(d2 => {
        localStorage.setItem('user', JSON.stringify(d2.data));  // self 可能失败
        window.location.href = '/console';
    });
}
```

### 坑 3：sub_filter 影响 API JSON 响应

**症状**：API 返回的 JSON 数据被损坏或前端解析异常

**原因**：`sub_filter` 如果没有 `sub_filter_types text/html` 限制，默认也是只对 text/html 生效，但如果忘了加这行，或者 `/api` 路径没有独立的 location 块（走了 `location /` 的带 sub_filter 配置），就会出问题。

**解决**：
1. `location /api` 必须是独立的 location 块，不包含任何 sub_filter
2. `location /` 中的 sub_filter 必须加 `sub_filter_types text/html`
3. 这两条配一起确保双重保险

### 坑 4：Accept-Encoding 与 sub_filter

**症状**：theme-switcher.js 没有被注入到 SPA 页面

**原因**：后端返回 gzip 压缩的响应时，sub_filter 无法匹配文本内容。

**解决**：在使用 sub_filter 的 location 块中必须加 `proxy_set_header Accept-Encoding ""`，强制后端返回未压缩的响应。但这行**只能出现在需要 sub_filter 的 location 中**，不要加到 `/api` 的 location 中。

## 八、SPA 认证机制说明

new-api 的 React SPA 使用以下认证流程：

```
登录: POST /api/user/login → 返回 {success: true, data: {id, username, token, role, quota, ...}}
                              ↓
存储: localStorage.setItem('user', JSON.stringify(data))
                              ↓
路由守卫检查: localStorage.getItem('user') 存在？
  ├─ 存在 → 允许访问控制台
  └─ 不存在 → 跳转 /login

API 认证: 从 localStorage.user 取 token 字段，
          设置 Authorization: Bearer <token> 头

登出: localStorage.removeItem('user') → 跳转 /login
```

自定义 login.html 只需要正确执行"存储"步骤，SPA 加载后会自动从 localStorage 恢复认证状态。

## 九、新服务器部署步骤

### 1. 部署 new-api

```bash
mkdir -p /docker/new-api
cd /docker/new-api

# 创建 docker-compose.yml（参考第四节）
# 修改密码和 SESSION_SECRET

docker compose up -d
```

### 2. 复制主题文件

```bash
# 从旧服务器复制整个主题目录
scp -r old-server:/docker/new-api/newapi_themes /docker/new-api/
```

### 3. 部署 channel-status（可选）

```bash
mkdir -p /docker/channel-status
cd /docker/channel-status

# 复制 main.go、go.mod、go.sum、Dockerfile
# 编译（需要 Go 环境，或在有 Go 的机器上交叉编译）
CGO_ENABLED=0 GOOS=linux GOARCH=amd64 go build -o channel-status .

# 构建镜像并运行
docker build -t channel-status .
docker run -d \
  --name channel-status \
  --restart always \
  --network new-api_new-api-network \
  -p 2086:8080 \
  -e DATABASE_URL="postgresql://root:你的密码@postgres:5432/new-api?sslmode=disable" \
  channel-status
```

### 4. 配置 Nginx

```bash
apt install -y nginx

# 创建配置文件（参考第五节完整配置，替换域名和路径）
nano /etc/nginx/sites-available/your-domain

# 启用站点
ln -s /etc/nginx/sites-available/your-domain /etc/nginx/sites-enabled/
rm -f /etc/nginx/sites-enabled/default

# 测试并启动
nginx -t && systemctl restart nginx
```

### 5. 配置 SSL

```bash
apt install -y certbot python3-certbot-nginx
certbot --nginx -d your-domain.com
```

### 6. 验证

```bash
# 1. 首页走自定义主题
curl -sk https://your-domain.com/ | head -3
# 应该看到自定义 HTML（含 design-page meta 标签）

# 2. /api/status 返回 new-api 系统配置（不是渠道列表）
curl -sk https://your-domain.com/api/status | python3 -c "
import json,sys; d=json.load(sys.stdin)
print('OK' if isinstance(d.get('data'), dict) and 'chats' in d['data'] else 'FAIL')
"

# 3. /api/channel-status 返回渠道状态
curl -sk https://your-domain.com/api/channel-status | python3 -c "
import json,sys; d=json.load(sys.stdin)
print('OK' if isinstance(d.get('data'), list) else 'FAIL')
"

# 4. SPA 页面注入了 theme-switcher.js
curl -sk https://your-domain.com/console | grep "theme-switcher"
# 应该看到 <script src="/theme/theme-switcher.js"></script>

# 5. API 响应未被 sub_filter 篡改（md5 一致）
DIRECT=$(curl -s http://127.0.0.1:3002/api/status | md5sum)
NGINX=$(curl -sk https://your-domain.com/api/status | md5sum)
[ "$DIRECT" = "$NGINX" ] && echo "API OK" || echo "API TAMPERED!"

# 6. 登录页走自定义主题
curl -sk https://your-domain.com/login | grep "design-page"
# 应该看到 <meta name="design-page" content="true">
```

## 十、new-api 更新流程

```bash
cd /docker/new-api

# 拉取最新镜像
docker compose pull

# 重启（主题文件和 nginx 配置不受影响）
docker compose up -d

# 验证
curl -sk https://your-domain.com/api/status | python3 -c "
import json,sys; d=json.load(sys.stdin); print('Version:', d['data'].get('version'))
"
```

### 更新后需要检查的唯一风险点

如果 new-api 未来某个版本改了 SPA 的认证方式（比如不再使用 `localStorage.user` 存储用户数据，或 login API 返回的数据结构变了），自定义 login.html 的登录脚本需要同步调整。检查方法：

```bash
# 下载 SPA 主 JS bundle，搜索认证逻辑
curl -sk https://your-domain.com/console | grep -o 'src="/assets/index-[^"]*"'
# 用返回的路径下载 JS，然后搜索：
# - localStorage.getItem("user") → 认证检查方式
# - localStorage.setItem("user" → 登录后存储方式
# - /api/user/login → 登录 API 路径
```

## 十一、添加新主题

```bash
# 1. 创建目录并复制模板
cp -r /docker/new-api/newapi_themes/dark /docker/new-api/newapi_themes/mytheme

# 2. 修改 HTML 文件中的样式和内容

# 3. Nginx map 中添加新主题名
#    "~^(dark|light|tech|cartoon|pixel|animated|mytheme)$" $1;

# 4. theme-switcher.js 中的 THEMES 数组添加新主题

# 5. 重载 Nginx
nginx -t && nginx -s reload
```

## 十二、文件清单

| 文件 | 位置 | 用途 | 更新时需改？ |
|------|------|------|-------------|
| docker-compose.yml | /docker/new-api/ | new-api 容器编排 | 否 |
| newapi_themes/ | /docker/new-api/ | 所有主题文件 | 否 |
| theme-switcher.js | newapi_themes/ | 主题切换核心逻辑 | 添加新主题时 |
| {theme}/login.html | newapi_themes/ | 自定义登录页 | 认证方式变更时 |
| {theme}/spa.css | newapi_themes/ | SPA 页面样式覆盖 | 否 |
| main.go | /docker/channel-status/ | 渠道状态监控源码 | 否 |
| nginx 配置 | /etc/nginx/sites-available/ | 反代+路由+注入 | 部署时改域名/路径 |
