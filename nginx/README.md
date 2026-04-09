# Nginx 配置说明

本目录包含所有与 New-API 集成相关的 nginx 配置文件。

## 配置文件说明

- `newapi.conf` - New-API 主站点配置
- `themes-inject.conf` - 主题文件注入配置（通过 nginx 注入静态资源）
- `status-proxy.conf` - 渠道状态监控代理配置
- `paypro-proxy.conf` - 支付系统代理配置

## 部署方式

### 方式一：Include 方式（推荐）

在你的主 nginx 配置中引入这些配置：

```nginx
http {
    # 其他配置...
    
    # 引入 New-API 增强配置
    include /docker/newapi-enhancements/nginx/*.conf;
}
```

### 方式二：复制到 nginx 配置目录

```bash
cp /docker/newapi-enhancements/nginx/*.conf /etc/nginx/conf.d/
nginx -t && nginx -s reload
```

## 配置说明

### 主题注入原理

通过 nginx 的 `sub_filter` 模块，在 HTML 响应中注入自定义的 CSS/JS 文件，实现主题切换功能，无需修改 New-API 容器内部文件。

### 代理配置

- 渠道状态页：`/status` -> `http://channel-status:8080`
- 支付系统：`/pay` -> `http://paypro:8889`

## 注意事项

1. 确保 nginx 编译时包含 `http_sub_module` 模块
2. 所有代理的后端服务必须在同一 Docker 网络中
3. 修改配置后记得执行 `nginx -t` 测试配置正确性
