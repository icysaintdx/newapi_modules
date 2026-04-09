# PayPro Settings.html 配置页面修复指南

## 问题描述

访问 `https://pay.isaint.cc/admin/settings.html` 时出现以下问题：
1. 页面显示中文乱码（如 `ç½'ç«™åç§°` 而不是 `网站名称`）
2. 配置项缺失（邮箱配置、页面文案配置等）
3. 之前存在的配置项消失了

## 根本原因分析

### 1. Docker容器化部署特点
- PayPro使用Docker容器运行
- 应用打包成JAR文件，模板文件（HTML）包含在JAR内部
- 只有 `application.yml` 通过volume挂载，其他文件需要重新构建镜像

### 2. 缓存问题
- **Nginx缓存**：静态资源设置了7天缓存（`expires 7d`）
- **浏览器缓存**：浏览器缓存了旧版本的HTML文件
- **Docker镜像缓存**：修改源文件后未重新构建镜像

### 3. 字符编码问题
- 数据库使用UTF-8存储（正确）
- JSON API返回Unicode转义序列（正确）
- 浏览器查看源代码时显示乱码（误导性问题）

## 完整解决方案

### 步骤1：确认数据库配置完整性

```bash
# 检查数据库中的配置项数量
docker exec paypro-mysql mysql -uroot -ppaypro123456 -e "USE pay; SELECT config_group, COUNT(*) as count FROM t_system_config GROUP BY config_group ORDER BY config_group;" 2>&1 | grep -v "Warning"
```

**期望结果：**
```
config_group    count
email           7
page_text       51
payment         8
payment_method  24
security        4
system          8
```

**总计：102个配置项**

如果配置项不完整，执行初始化SQL：

```bash
# 执行完整配置初始化
docker exec -i paypro-mysql mysql -uroot -ppaypro123456 pay < /docker/paypro/init_all_configs.sql 2>&1 | grep -v "Warning"
```

### 步骤2：验证源文件正确性

```bash
# 检查settings.html文件大小和关键内容
wc -l /docker/paypro/src/main/resources/templates/admin/settings.html
grep -n "v-for.*getConfigsByGroup" /docker/paypro/src/main/resources/templates/admin/settings.html | head -5
```

**期望结果：**
- 文件约500行
- 包含多个 `v-for="config in getConfigsByGroup('xxx')"` 循环

### 步骤3：重新构建Docker镜像

```bash
# 进入项目目录
cd /docker/paypro

# 重新构建paypro镜像（包含最新的settings.html）
docker compose build paypro

# 重启容器
docker compose restart paypro

# 等待5秒后检查启动日志
sleep 5 && docker logs paypro --tail 20
```

**期望看到：**
```
Started PayApplication in X.XXX seconds
Tomcat started on port(s): 8889
```

### 步骤4：验证JAR包内容

```bash
# 检查JAR包中是否包含最新的settings.html
docker exec paypro sh -c "cd /app && jar -tf app.jar | grep settings.html"

# 提取并验证内容
docker exec paypro sh -c "cd /app && jar -xf app.jar BOOT-INF/classes/templates/admin/settings.html && grep -n 'v-for.*getConfigsByGroup' BOOT-INF/classes/templates/admin/settings.html | head -5"
```

### 步骤5：修复Nginx缓存配置

编辑 `/etc/nginx/sites-available/pay-isaint`：

```nginx
server {
    listen 443 ssl http2;
    server_name pay.isaint.cc;
    
    # SSL配置...
    
    # HTML文件不缓存（新增）
    location ~* \.(html)$ {
        proxy_pass http://paypro_backend;
        proxy_set_header Host $host;
        proxy_buffering off;
        expires -1;
        add_header Cache-Control "no-store, no-cache, must-revalidate, proxy-revalidate, max-age=0";
    }

    # 静态资源（保持缓存）
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2|ttf|eot)$ {
        proxy_pass http://paypro_backend;
        proxy_set_header Host $host;
        proxy_buffering off;
        expires 7d;
        add_header Cache-Control "public, immutable";
    }
    
    # 其他配置...
}
```

重新加载Nginx：

```bash
nginx -t && nginx -s reload
```

### 步骤6：清除浏览器缓存

**方法1：强制刷新**
- Windows/Linux: `Ctrl + Shift + R`
- Mac: `Cmd + Shift + R`
- 或者: `Ctrl + F5`

**方法2：开发者工具**
1. 打开开发者工具 (F12)
2. 右键点击刷新按钮
3. 选择"清空缓存并硬性重新加载"

**方法3：隐私模式**
- 使用浏览器的隐私/无痕模式访问

**方法4：URL加时间戳**
```
https://pay.isaint.cc/admin/settings.html?v=20260407
```

## 验证步骤

### 1. 检查数据库字符集

```bash
docker exec paypro-mysql mysql -uroot -ppaypro123456 --default-character-set=utf8mb4 -e "SHOW VARIABLES LIKE 'character%';" 2>&1 | grep -v "Warning"
```

**期望结果：**
```
character_set_client        utf8mb4
character_set_connection    utf8mb4
character_set_database      utf8mb4
character_set_results       utf8mb4
character_set_server        utf8mb4
```

### 2. 检查JDBC连接配置

```bash
docker exec paypro env | grep SPRING_DATASOURCE_URL
```

**期望结果：**
```
SPRING_DATASOURCE_URL=jdbc:mysql://paypro-mysql:3306/pay?useSSL=false&characterEncoding=utf-8&serverTimezone=GMT%2B8
```

### 3. 测试API响应

```bash
# 测试配置API（需要登录，会返回500但能看到编码）
curl -s 'http://localhost:8889/admin/api/config/group/system' | python3 -m json.tool
```

**期望结果：**
- JSON格式正确
- 中文使用Unicode转义序列（如 `\u7cfb\u7edf`）

### 4. 检查页面显示

访问 `https://pay.isaint.cc/admin/settings.html`，应该看到：

**系统配置标签页：**
- 网站名称
- 网站地址
- 网站标题
- 作者名称
- 项目下载地址
- 支持邮箱
- 二维码数量
- 游戏服务器地址

**邮箱配置标签页：**
- SMTP服务器地址
- SMTP端口
- 邮件协议
- 邮箱账号
- 邮箱授权码
- 审核邮件接收者
- 邮件发送者

**支付配置标签页：**
- 支付宝个人收款码URL
- 支付宝UserId
- 支付宝当面付AppId
- 支付宝应用私钥
- 支付宝公钥
- 支付宝当面付主题
- 微信收款码URL
- 微信赞赏码URL

**支付方式标签页：**
- 支付宝支付（启用/禁用、名称、描述、顺序、夜间支付、本地二维码）
- 微信支付（同上）
- 微信赞赏码（同上）
- 支付宝当面付（同上）

**安全配置标签页：**
- IP限流时间
- 二次验证token值
- Token过期时间
- OpenAPI密钥

**页面文案标签页：**
- 搜索框可搜索51个页面文案配置项

## 常见问题排查

### Q1: 修改了settings.html但页面没变化？

**原因：** Docker容器内的JAR包没有更新

**解决：**
```bash
cd /docker/paypro
docker compose build paypro
docker compose restart paypro
```

### Q2: 重新构建后还是显示旧页面？

**原因：** Nginx或浏览器缓存

**解决：**
1. 检查Nginx配置是否禁用HTML缓存
2. 强制刷新浏览器 (Ctrl+Shift+R)
3. 使用隐私模式测试

### Q3: 页面显示"配置加载失败"？

**原因：** 
- 未登录
- 权限不足（需要SUPER_ADMIN角色）
- 后端服务未启动

**解决：**
```bash
# 检查容器状态
docker ps | grep paypro

# 检查日志
docker logs paypro --tail 50

# 确认登录状态
# 访问 /admin/login.html 重新登录
```

### Q4: 配置保存后没有生效？

**原因：** 
- 某些配置需要重启应用
- 配置值格式错误

**解决：**
```bash
# 检查数据库中的值是否已更新
docker exec paypro-mysql mysql -uroot -ppaypro123456 -e "USE pay; SELECT config_key, config_value FROM t_system_config WHERE config_key='xxx';" 2>&1 | grep -v "Warning"

# 重启应用使配置生效
docker compose restart paypro
```

### Q5: 数据库中文显示乱码？

**原因：** MySQL客户端字符集不匹配

**解决：**
```bash
# 使用正确的字符集连接
docker exec paypro-mysql mysql -uroot -ppaypro123456 --default-character-set=utf8mb4 -e "USE pay; SELECT * FROM t_system_config LIMIT 5;" 2>&1 | grep -v "Warning"
```

## 配置项完整清单

### 系统配置组 (system) - 8项
| 配置键 | 描述 | 默认值 |
|--------|------|--------|
| site.name | 网站名称 | PayPro |
| site.url | 网站地址 | https://pay.isaint.cc |
| site.title | 网站标题 | PayPro个人收款系统 |
| site.author | 作者名称 | codewendao |
| site.download_url | 项目下载地址 | https://github.com/codewendao/PayPro |
| site.support_email | 支持邮箱 | support@example.com |
| site.qrcode_num | 二维码数量 | 2 |
| site.game_url | 游戏服务器地址 | http://localhost:8889 |

### 邮件配置组 (email) - 7项
| 配置键 | 描述 | 默认值 | 敏感 |
|--------|------|--------|------|
| email.smtp_host | SMTP服务器地址 | smtp.qq.com | 否 |
| email.smtp_port | SMTP端口 | 465 | 否 |
| email.protocol | 邮件协议 | smtps | 否 |
| email.username | 邮箱账号 | | 是 |
| email.password | 邮箱授权码 | | 是 |
| email.receiver | 审核邮件接收者 | | 否 |
| email.sender | 邮件发送者 | | 否 |

### 支付配置组 (payment) - 8项
| 配置键 | 描述 | 默认值 | 敏感 |
|--------|------|--------|------|
| alipay.custom.qr.url | 支付宝个人收款码URL | | 否 |
| alipay.user.id | 支付宝UserId | | 否 |
| alipay.dmf.app.id | 支付宝当面付AppId | | 是 |
| alipay.dmf.app.private.key | 支付宝应用私钥 | | 是 |
| alipay.dmf.public.key | 支付宝公钥 | | 是 |
| alipay.dmf.subject | 支付宝当面付主题 | 向PayPro作者捐赠 | 否 |
| wechat.qr.url | 微信收款码URL | | 否 |
| wechat.zs.qr.url | 微信赞赏码URL | | 否 |

### 支付方式配置组 (payment_method) - 24项
每个支付方式6个配置项：
- `payment.method.{方式}.enabled` - 是否启用
- `payment.method.{方式}.name` - 显示名称
- `payment.method.{方式}.description` - 描述
- `payment.method.{方式}.allow_night` - 允许夜间支付
- `payment.method.{方式}.use_local_qr` - 使用本地二维码
- `payment.method.{方式}.sort` - 显示顺序

支持的支付方式：
1. `alipay` - 支付宝支付
2. `wechat` - 微信支付
3. `wechat_zs` - 微信赞赏码
4. `alipay_dmf` - 支付宝当面付

### 安全配置组 (security) - 4项
| 配置键 | 描述 | 默认值 | 敏感 |
|--------|------|--------|------|
| security.rate_limit.ip_expire | IP限流时间(秒) | 2 | 否 |
| security.token.value | 二次验证token值 | 123 | 是 |
| security.token.expire | Token过期时间(天) | 14 | 否 |
| security.openapi.secret | OpenAPI密钥 | newapi_paypro_secret_2026 | 是 |

### 页面文案配置组 (page_text) - 51项
包含所有页面的可配置文案，如：
- 通用文案（页脚、Logo等）
- 首页文案（标题、按钮等）
- 支付页面文案（订单号、支付方式等）
- 充值页面文案
- 帮助中心文案
- 后台管理文案
- 订单状态文案
- 成功/失败页面文案

## 文件结构

```
/docker/paypro/
├── src/main/resources/
│   ├── templates/admin/
│   │   └── settings.html          # 配置管理页面（500行）
│   ├── application.yml             # 应用配置（挂载到容器）
│   └── pay.sql                     # 数据库初始化脚本
├── init_all_configs.sql            # 完整配置初始化SQL
├── docker-compose.yml              # Docker编排配置
├── Dockerfile                      # Docker镜像构建文件
└── pom.xml                         # Maven项目配置
```

## 技术栈说明

- **后端框架**: Spring Boot 2.3.2
- **数据库**: MySQL 5.7 (UTF-8MB4)
- **缓存**: Redis
- **认证**: Sa-Token
- **前端**: Vue.js 3 + Petite-Vue
- **Web服务器**: Nginx (反向代理)
- **容器化**: Docker + Docker Compose

## 维护建议

### 1. 修改配置页面时
```bash
# 1. 修改源文件
vim /docker/paypro/src/main/resources/templates/admin/settings.html

# 2. 重新构建镜像
cd /docker/paypro && docker compose build paypro

# 3. 重启容器
docker compose restart paypro

# 4. 清除浏览器缓存测试
```

### 2. 添加新配置项时
```sql
-- 在数据库中添加配置项
INSERT INTO t_system_config (config_key, config_value, config_type, config_group, description, is_sensitive) 
VALUES ('new.config.key', 'default_value', 'STRING', 'system', '配置描述', 0);
```

### 3. 备份重要配置
```bash
# 导出配置到文件
docker exec paypro-mysql mysqldump -uroot -ppaypro123456 pay t_system_config > /docker/paypro/backup/system_config_$(date +%Y%m%d).sql
```

### 4. 监控日志
```bash
# 实时查看应用日志
docker logs -f paypro

# 查看Nginx访问日志
tail -f /var/log/nginx/access.log

# 查看Nginx错误日志
tail -f /var/log/nginx/error.log
```

## 相关文件位置

- **Nginx配置**: `/etc/nginx/sites-available/pay-isaint`
- **SSL证书**: `/etc/letsencrypt/live/pay.isaint.cc/`
- **应用日志**: `docker logs paypro`
- **数据库数据**: Docker volume `paypro_mysql_data`

## 更新历史

- **2026-04-07**: 修复配置页面乱码和配置项缺失问题
  - 添加完整的102个配置项
  - 修复Nginx HTML缓存问题
  - 更新settings.html支持所有配置组
  - 添加支付方式管理功能

---

**文档版本**: 1.0  
**最后更新**: 2026-04-07  
**维护者**: PayPro Team

---

## 附录：完整历史记录

### 原始需求（2026-04-06 15:13）

用户要求继续完成之前的配置管理页面开发，当时的任务清单：

```
[✓] 创建配置管理后端 Controller
[✓] 创建配置实体类和 DTO
[✓] 创建配置管理 Service
[✓] 创建配置管理前端页面
[✓] 添加文件上传功能
[•] 添加菜单项到后台
[ ] 重新构建并测试
```

### 问题演变过程

#### 第一阶段：菜单乱码问题（15:27）
- **现象**：菜单显示 `ç³»ç»Ÿé…ç½®` 而不是"系统配置"
- **原因**：数据库字符集问题
- **解决**：确认数据库使用 UTF8MB4

#### 第二阶段：页面访问失败（15:28）
- **现象**：访问 `/admin/settings.html` 提示"操作失败"
- **原因**：settings.html 文件没有被打包到 Docker 镜像中
- **解决**：重新构建 Docker 镜像

#### 第三阶段：配置项缺失（22:29）
- **现象**：
  - 基本配置只有2个输入框
  - 邮箱配置显示"暂未实现"
  - 支付宝配置标签全是乱码
  - 页面文案配置显示"暂未实现"
- **原因**：settings.html 文件本身就是乱码（UTF-8 被错误保存为其他编码）
- **解决**：重新创建 settings.html 文件

#### 第四阶段：配置项不完整（22:55）
- **现象**：用户指出之前有更多配置项，现在都没了
- **原因**：在多次修改过程中，配置项逐渐丢失
- **解决**：
  1. 从数据库、application.yml、代码中搜集所有配置项
  2. 创建完整的 102 个配置项
  3. 重新设计 settings.html，包含 6 个标签页

### 最终解决方案

#### 1. 数据库配置（102个配置项）

**系统配置组 (system) - 8项**
- site.name, site.url, site.title, site.author
- site.download_url, site.support_email, site.qrcode_num, site.game_url

**邮件配置组 (email) - 7项**
- email.smtp_host, email.smtp_port, email.protocol
- email.username, email.password, email.receiver, email.sender

**支付配置组 (payment) - 8项**
- alipay.custom.qr.url, alipay.user.id
- alipay.dmf.app.id, alipay.dmf.app.private.key, alipay.dmf.public.key
- alipay.dmf.subject, wechat.qr.url, wechat.zs.qr.url

**支付方式配置组 (payment_method) - 24项**
- 每个支付方式6个配置：enabled, name, description, allow_night, use_local_qr, sort
- 4种支付方式：alipay, wechat, wechat_zs, alipay_dmf

**安全配置组 (security) - 4项**
- security.rate_limit.ip_expire, security.token.value
- security.token.expire, security.openapi.secret

**页面文案配置组 (page_text) - 51项**
- 通用文案、首页文案、支付页面文案、充值页面文案
- 帮助中心文案、后台管理文案、订单状态文案、成功/失败页面文案

#### 2. settings.html 页面结构

```html
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>系统配置 - PayPro</title>
</head>
<body>
    <div id="app">
        <!-- 6个标签页 -->
        <div class="tabs">
            <button>系统配置</button>
            <button>邮箱配置</button>
            <button>支付配置</button>
            <button>支付方式</button>
            <button>安全配置</button>
            <button>页面文案</button>
        </div>
        
        <!-- 系统配置标签页 -->
        <div class="tab-content active">
            <div v-for="config in getConfigsByGroup('system')">
                <label>{{ config.description }}</label>
                <input v-model="configValues[config.configKey]">
            </div>
        </div>
        
        <!-- 其他标签页类似... -->
    </div>
    
    <script>
        // Vue.js 3 应用
        const app = Vue.createApp({
            data() {
                return {
                    allConfigs: {},
                    configValues: {}
                }
            },
            methods: {
                async loadConfigs() {
                    const response = await fetch('/admin/api/config/all');
                    const result = await response.json();
                    this.allConfigs = result.data;
                },
                async saveConfigs() {
                    await fetch('/admin/api/config/save', {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify(this.configValues)
                    });
                }
            }
        });
    </script>
</body>
</html>
```

#### 3. 后端 API

**AdminConfigController.java**
```java
@RestController
@RequestMapping("/admin/api/config")
@SaCheckRole("SUPER_ADMIN")
public class AdminConfigController {
    
    @GetMapping("/all")
    public ResponseVO getAllConfigs() {
        Map<String, List<SystemConfig>> configs = 
            systemConfigService.getAllConfigsByGroup();
        return ResponseVO.successResponse(configs);
    }
    
    @PostMapping("/save")
    public ResponseVO saveConfig(@RequestBody Map<String, String> configMap) {
        systemConfigService.batchSaveConfigs(configMap);
        return ResponseVO.successResponse("配置保存成功");
    }
}
```

### 关键经验教训

1. **字符编码问题**
   - 始终使用 UTF-8 编码保存文件
   - 数据库必须使用 UTF8MB4
   - JDBC连接URL必须包含 `characterEncoding=utf-8`

2. **Docker容器化部署**
   - 修改源文件后必须重新构建镜像
   - 只有挂载的文件（如application.yml）可以直接修改
   - 模板文件（HTML）在JAR包内，需要重新构建

3. **缓存问题**
   - Nginx缓存HTML文件会导致更新不生效
   - 浏览器缓存需要强制刷新（Ctrl+Shift+R）
   - 建议HTML文件不缓存，静态资源可以缓存

4. **配置管理设计**
   - 配置项应该集中管理，不要分散在多个地方
   - 敏感配置（密钥）应该标记并特殊处理
   - 配置修改应该有日志记录

### 完整构建流程

```bash
# 1. 修改源文件
vim /docker/paypro/src/main/resources/templates/admin/settings.html

# 2. 重新构建镜像
cd /docker/paypro
docker compose build paypro

# 3. 重启容器
docker compose restart paypro

# 4. 验证部署
docker logs paypro --tail 20
docker exec paypro sh -c "cd /app && jar -tf app.jar | grep settings.html"

# 5. 清除缓存
# - Nginx: nginx -s reload
# - 浏览器: Ctrl+Shift+R
```

### 数据库初始化

完整的配置初始化SQL已保存在：
```
/docker/paypro/init_all_configs.sql
```

执行方式：
```bash
docker exec -i paypro-mysql mysql -uroot -ppaypro123456 pay < /docker/paypro/init_all_configs.sql
```

---

**文档最后更新**: 2026-04-07 01:52  
**问题解决时长**: 约10小时（从15:13到01:52）  
**主要困难**: 字符编码问题、Docker缓存、配置项丢失


---

## 补充：完整实现版本记录

### 最终完成版本（2026-04-07 00:00）

根据数据库记录，最终成功实现的版本包含：

#### 1. 数据库配置（102个配置项）

**分组统计：**
```
email          7项
page_text     51项
payment        8项
payment_method 24项
security       4项
system         8项
```

#### 2. settings.html 页面结构（6个标签页）

1. **系统配置** - 8个配置项
2. **邮箱配置** - 7个配置项
3. **支付配置** - 8个配置项
4. **支付方式管理** - 24个配置项（4种支付方式 × 6个配置）
5. **安全配置** - 4个配置项
6. **页面文案** - 51个配置项（带搜索功能）

#### 3. 页面文案分组优化

页面文案标签页内部又分为9个子分组：
- 通用文案（页脚、Logo等）
- 首页文案
- 支付页面文案
- 充值页面文案
- 帮助中心文案
- 后台管理文案
- 订单状态文案
- 成功页面文案
- 错误页面文案

#### 4. 关键功能特性

**前端功能：**
- ✅ Vue.js 3 响应式数据绑定
- ✅ 标签页切换
- ✅ 配置项搜索（页面文案）
- ✅ 表单验证
- ✅ 保存/重置功能
- ✅ 敏感信息脱敏显示（密钥显示为 ******）

**后端功能：**
- ✅ 按分组获取配置 `/admin/api/config/all`
- ✅ 批量保存配置 `/admin/api/config/save`
- ✅ 超级管理员权限控制 `@SaCheckRole("SUPER_ADMIN")`
- ✅ 敏感信息过滤

#### 5. 支付方式管理详细配置

每个支付方式包含6个配置项：

**支付宝支付 (alipay)**
```
payment.method.alipay.enabled       - 是否启用
payment.method.alipay.name          - 显示名称
payment.method.alipay.description   - 描述
payment.method.alipay.allow_night   - 允许夜间支付
payment.method.alipay.use_local_qr  - 使用本地二维码
payment.method.alipay.sort          - 显示顺序
```

**微信支付 (wechat)** - 同上6项
**微信赞赏码 (wechat_zs)** - 同上6项
**支付宝当面付 (alipay_dmf)** - 同上6项

### 实现时间线

```
15:13 - 用户要求继续完成配置管理页面
15:27 - 发现菜单乱码问题
15:28 - 发现页面访问失败
22:29 - 用户报告配置项缺失和乱码
22:55 - 用户指出之前有更多配置项
22:58 - 开始添加完整的102个配置项
23:21 - 创建包含6个标签页的完整页面
00:00 - 重新构建并部署成功
00:43 - 优化页面文案分组显示
```

### 最终文件大小

- **settings.html**: 约800-1000行
- **init_all_configs.sql**: 约500行
- **AdminConfigController.java**: 约175行

### 页面截图说明（基于用户反馈）

用户在22:29复制的页面内容显示：
- 5个标签页（基本配置、邮箱配置、支付宝配置、页面文案、二维码管理）
- 基本配置：2个输入框
- 邮箱配置：显示"暂未实现"
- 支付宝配置：6个输入框（当时显示乱码）
- 页面文案：显示"暂未实现"
- 二维码管理：3个上传按钮

**注意**：这是修复前的状态，修复后应该显示完整的6个标签页和所有配置项。

### 验证清单

修复完成后，应该能看到：

**系统配置标签页：**
- [ ] 网站名称
- [ ] 网站地址
- [ ] 网站标题
- [ ] 作者名称
- [ ] 项目下载地址
- [ ] 支持邮箱
- [ ] 二维码数量
- [ ] 游戏服务器地址

**邮箱配置标签页：**
- [ ] SMTP服务器地址
- [ ] SMTP端口
- [ ] 邮件协议
- [ ] 邮箱账号
- [ ] 邮箱授权码
- [ ] 审核邮件接收者
- [ ] 邮件发送者

**支付配置标签页：**
- [ ] 支付宝个人收款码URL
- [ ] 支付宝UserId
- [ ] 支付宝当面付AppId
- [ ] 支付宝应用私钥
- [ ] 支付宝公钥
- [ ] 支付宝当面付主题
- [ ] 微信收款码URL
- [ ] 微信赞赏码URL

**支付方式标签页：**
- [ ] 支付宝支付（6个配置项）
- [ ] 微信支付（6个配置项）
- [ ] 微信赞赏码（6个配置项）
- [ ] 支付宝当面付（6个配置项）

**安全配置标签页：**
- [ ] IP限流时间
- [ ] 二次验证token值
- [ ] Token过期时间
- [ ] OpenAPI密钥

**页面文案标签页：**
- [ ] 搜索框
- [ ] 9个分组卡片
- [ ] 51个配置项

---

**最终确认**: 如果你现在访问 https://pay.isaint.cc/admin/settings.html 并登录后，应该能看到上述所有内容。如果还有问题，请清除浏览器缓存（Ctrl+Shift+R）后重试。

