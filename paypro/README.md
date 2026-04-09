# PayPro 支付系统

支持支付宝当面付的自动充值系统，与 New-API 集成。

## 功能特性

- ✅ 支付宝当面付（扫码支付）
- ✅ 自动充值到 New-API 账户
- ✅ 支付通知（邮件）
- ✅ 订单管理
- ✅ 支付回调处理
- ✅ 独立部署，不修改 New-API

## 技术栈

- Java 8
- Spring Boot 2.x
- MySQL 5.7
- Redis
- 支付宝开放平台 SDK

## 部署

### 前置要求

1. 支付宝当面付账号（需要企业认证）
2. MySQL 5.7+
3. Redis
4. 邮件服务器（用于发送通知）

### 快速部署

```bash
cd /docker/newapi-enhancements
bash scripts/deploy-paypro.sh
```

### 手动部署

```bash
cd /docker/newapi-enhancements/paypro

# 1. 配置 application.yml（见下方配置说明）
nano src/main/resources/application.yml

# 2. 使用 docker-compose 部署
docker-compose up -d

# 3. 验证
docker ps | grep paypro
curl http://localhost:8889
```

## 配置

### 1. 支付宝配置

编辑 `src/main/resources/application.yml`：

```yaml
paypro:
  # 支付宝收款二维码 URL
  alipayCustomQrUrl: https://qr.alipay.com/xxx
  
  # 支付宝 userId（扫描项目中的"获取支付宝userId.jpg"获取）
  alipayUserId: 2088xxxxxxxxxx
  
  # 支付宝当面付配置
  alipayDfmAppId: 2021xxxxxxxxxx
  alipayDfmAppPrivateKey: MIIEvQIBADANBgkqhkiG9w0BAQEFAASC...
  alipayDfmPublicKey: MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8A...
  
  # 网站域名（用于生成支付链接）
  site: https://your-domain.com
  
  # 管理员邮箱（接收支付通知）
  receiver: admin@your-domain.com
  
  # 发件邮箱
  sender: noreply@your-domain.com
```

### 2. 数据库配置

```yaml
spring:
  datasource:
    url: jdbc:mysql://paypro-mysql:3306/paypro?useUnicode=true&characterEncoding=utf8
    username: root
    password: 123456
```

### 3. Redis 配置

```yaml
spring:
  redis:
    host: redis
    port: 6379
    password: ""  # 如果有密码
```

### 4. 邮件配置

```yaml
spring:
  mail:
    host: smtp.qq.com
    port: 587
    username: your-email@qq.com
    password: your-auth-code  # QQ邮箱授权码
```

## 支付宝当面付申请

详细教程见：`支付宝当面付申请教程及公钥私钥配置说明.docx`

### 简要步骤

1. 登录支付宝开放平台：https://open.alipay.com
2. 创建应用（选择"网页/移动应用"）
3. 配置应用信息
4. 添加"当面付"功能
5. 配置应用公钥（上传你生成的公钥）
6. 获取支付宝公钥
7. 提交审核
8. 审核通过后获得 AppId

### 生成密钥对

```bash
# 使用支付宝提供的工具生成 RSA2 密钥对
# 下载地址：https://opendocs.alipay.com/common/02kipl

# 或使用 OpenSSL
openssl genrsa -out app_private_key.pem 2048
openssl rsa -in app_private_key.pem -pubout -out app_public_key.pem
```

## 与 New-API 集成

### 1. 复制集成文件

```bash
cp /docker/newapi-enhancements/paypro/newapi-integration/topup_paypro.go \
   /docker/new-api/controller/
```

### 2. 重新编译 New-API

如果 New-API 是从源码编译的：

```bash
cd /docker/new-api
# 重新编译
```

如果使用 Docker 镜像，需要自己构建包含集成代码的镜像。

### 3. 配置 New-API

在 New-API 的配置中添加支付回调 URL：

```
PAYPRO_CALLBACK_URL=http://paypro:8889/callback
```

## Nginx 配置

```nginx
# 支付系统代理
location /pay {
    proxy_pass http://paypro:8889;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    
    # 支付回调需要较长超时
    proxy_connect_timeout 120s;
    proxy_send_timeout 120s;
    proxy_read_timeout 120s;
}

# 支付回调（允许支付宝访问）
location /pay/callback {
    proxy_pass http://paypro:8889/callback;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    allow all;
}
```

## API 接口

详细 API 文档见：`OPEN_API.md`

### 创建订单

```bash
POST /api/order/create
Content-Type: application/json

{
  "userId": "user123",
  "amount": 100.00,
  "description": "充值100元"
}
```

### 查询订单

```bash
GET /api/order/query?orderId=xxx
```

### 支付回调

```bash
POST /callback
Content-Type: application/x-www-form-urlencoded

# 支付宝回调参数
```

## 数据库初始化

首次部署时，执行 SQL 初始化脚本：

```bash
docker exec -i paypro-mysql mysql -uroot -p123456 paypro < init_all_configs.sql
```

## 故障排查

### 支付失败

1. 检查支付宝配置是否正确
2. 查看日志：`docker logs paypro`
3. 确认支付宝当面付功能已开通
4. 检查密钥配置是否正确

### 回调不触发

1. 确认回调 URL 可从公网访问
2. 检查支付宝开放平台的回调配置
3. 查看 nginx 日志
4. 检查防火墙设置

### 数据库连接失败

1. 确认 MySQL 容器运行正常
2. 检查数据库连接配置
3. 确认容器在同一 Docker 网络

### Redis 连接失败

1. 确认 Redis 容器运行正常
2. 检查 Redis 连接配置
3. 如果 Redis 有密码，确认密码正确

## 开发

### 本地开发

```bash
# 1. 启动 MySQL 和 Redis
docker-compose up -d paypro-mysql redis

# 2. 配置 application-dev.yml

# 3. 运行
mvn spring-boot:run

# 4. 访问
curl http://localhost:8889
```

### 编译

```bash
mvn clean package
```

生成的 JAR 文件：`target/paypro-0.0.1-SNAPSHOT.jar`

## 安全建议

1. **生产环境必须使用 HTTPS**
2. **保护好支付宝密钥** - 不要提交到 Git
3. **限制回调 IP** - 只允许支付宝服务器 IP
4. **使用强密码** - 数据库、Redis 等
5. **定期备份数据库**
6. **监控异常订单**

## 监控

### 日志位置

- 应用日志：`docker logs paypro`
- MySQL 日志：`docker logs paypro-mysql`

### 关键指标

- 订单创建成功率
- 支付成功率
- 回调处理时间
- 数据库连接池状态

## 更新

```bash
# 1. 拉取最新代码
cd /docker/newapi-enhancements
git pull

# 2. 重新部署
bash scripts/deploy-paypro.sh
```

## 卸载

```bash
# 停止并删除容器
docker-compose down

# 删除数据（可选）
docker volume rm paypro_mysql_data
```

## 原始配置说明

原始的配置说明保存在 `README.old.md` 中，包含详细的配置步骤。

## 获取帮助

- 支付宝开放平台文档：https://opendocs.alipay.com
- 当面付接口文档：https://opendocs.alipay.com/open/194/105072
- 提交 Issue
