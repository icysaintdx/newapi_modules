# New-API 主题系统

6 套精美主题，通过 nginx 注入实现，无需修改 New-API 容器。

## 主题列表

- **animated** - 动画主题（带可爱角色动画）
- **cartoon** - 卡通主题
- **dark** - 暗色主题
- **light** - 亮色主题（默认）
- **pixel** - 像素风格主题
- **tech** - 科技风格主题

## 工作原理

通过 nginx 的 `sub_filter` 模块，在 HTML 响应中注入主题切换器的 CSS 和 JS 文件：

```nginx
sub_filter '</head>' '<link rel="stylesheet" href="/themes/theme-switcher.css"><script src="/themes/theme-switcher.js"></script></head>';
sub_filter_once on;
sub_filter_types text/html;

# 必须关闭 gzip
proxy_set_header Accept-Encoding "";
```

## 部署步骤

### 1. 配置 nginx 代理主题文件

```nginx
location /themes/ {
    alias /docker/newapi-enhancements/themes/;
    expires 7d;
    add_header Cache-Control "public, immutable";
}
```

### 2. 在 New-API 的 location 中添加注入

```nginx
location / {
    proxy_pass http://new-api:3000;
    # ... 其他配置 ...
    
    # 主题注入
    sub_filter '</head>' '<link rel="stylesheet" href="/themes/theme-switcher.css"><script src="/themes/theme-switcher.js"></script></head>';
    sub_filter_once on;
    sub_filter_types text/html;
    
    # 关闭 gzip（重要！）
    proxy_set_header Accept-Encoding "";
}
```

### 3. 重载 nginx

```bash
nginx -t && nginx -s reload
```

### 4. 验证

访问 New-API 主站，应该能在页面右下角看到主题切换器。

## 目录结构

```
themes/
├── animated/
│   ├── login.html
│   ├── register.html
│   ├── reset.html
│   ├── home.html
│   ├── about.html
│   ├── announcement.html
│   ├── privacy.html
│   └── characters.js      # 角色动画脚本
├── cartoon/
│   └── ...
├── dark/
│   └── ...
├── light/
│   └── ...
├── pixel/
│   └── ...
├── tech/
│   └── ...
├── theme-switcher.js       # 主题切换器脚本
└── theme-switcher.css      # 主题切换器样式
```

## 自定义主题

### 创建新主题

1. 复制现有主题目录：
   ```bash
   cp -r themes/light themes/my-theme
   ```

2. 修改主题文件中的样式

3. 在 `theme-switcher.js` 中添加新主题：
   ```javascript
   const themes = [
       // ... 现有主题
       { name: 'my-theme', label: '我的主题', icon: '🎨' }
   ];
   ```

### 修改现有主题

直接编辑对应主题目录下的 HTML 文件即可，无需重启任何服务。

## 注意事项

1. **必须关闭 gzip** - `sub_filter` 只对未压缩的响应生效
2. **清除浏览器缓存** - 修改主题后需要清除缓存才能看到效果
3. **主题文件路径** - 确保 nginx 能访问 `/docker/newapi-enhancements/themes/` 目录
4. **nginx 模块** - 确认 nginx 编译时包含 `http_sub_module`

## 故障排查

### 主题切换器不显示

1. 检查浏览器控制台是否有 JS 错误
2. 确认 `/themes/theme-switcher.js` 和 `/themes/theme-switcher.css` 可访问
3. 查看 HTML 源码，确认注入成功（搜索 `theme-switcher`）

### 主题不生效

1. 清除浏览器缓存（Ctrl+Shift+R）
2. 检查浏览器 LocalStorage 中的主题设置
3. 确认主题文件存在：`ls /docker/newapi-enhancements/themes/`

### sub_filter 不工作

1. 确认关闭了 gzip：`proxy_set_header Accept-Encoding "";`
2. 检查 nginx 是否有 `http_sub_module`：`nginx -V 2>&1 | grep http_sub_module`
3. 查看 nginx 错误日志：`tail -f /var/log/nginx/error.log`

## 技术细节

### 主题切换原理

1. 用户选择主题后，JS 保存到 LocalStorage
2. 页面加载时，JS 读取 LocalStorage 中的主题设置
3. 根据当前页面路径（如 `/login`），动态加载对应主题的 HTML
4. 使用 `fetch` 获取主题 HTML，替换页面内容

### 性能优化

- 主题文件设置了 7 天缓存
- 使用 `Cache-Control: public, immutable`
- 主题切换器脚本异步加载

## 更新日志

### 2026-04-05
- 所有主题的登录/注册/重置页面添加首页链接
- 所有主题的首页添加状态页链接
- 修复动画角色表情错误
- 优化主题切换器样式
