# PushPlus推送配置指南

## 功能说明

PushPlus推送功能可以将订单审核通知实时推送到你的微信，支持移动端一键确认/拒绝订单。

### 核心优势
- ✅ **移动端操作**: 微信收到推送，点击按钮即可处理
- ✅ **实时通知**: 订单创建后立即推送
- ✅ **零成本**: PushPlus免费额度足够使用
- ✅ **双场景支持**: NewAPI充值 + 购卡发卡都支持
- ✅ **自动过期**: 30分钟未处理自动标记过期

## 配置步骤

### 第一步: 获取PushPlus Token

1. 访问 [PushPlus官网](https://www.pushplus.plus)
2. 使用微信扫码登录
3. 进入"发送消息" → "一对一消息"
4. 复制你的Token(格式类似: `xxxxxxxxxxxxxxxxxxxxxx`)

### 第二步: 配置PayPro

编辑 `application.yml` 或通过环境变量配置:

```yaml
paypro:
  pushplus:
    token: your_pushplus_token_here  # 填入你的PushPlus Token
    enabled: true                     # 启用推送功能
```

或使用环境变量(推荐):

```bash
# docker-compose.yml
environment:
  - PUSHPLUS_TOKEN=your_pushplus_token_here
  - PUSHPLUS_ENABLED=true
```

### 第三步: 重启服务

```bash
cd /docker/newapi-enhancements/paypro
docker-compose restart paypro
```

### 第四步: 测试推送

创建一个测试订单，你应该会在微信收到推送消息，包含:
- 订单详细信息
- "✅ 确认收款" 按钮
- "❌ 拒绝订单" 按钮

## 推送消息示例

```
💰 收到新订单
━━━━━━━━━━━━━━━━━━━━
订单类型: 购卡订单
金额: ¥10.00
商品: GPT-4 兑换码
用户邮箱: user@example.com
支付方式: 微信支付
支付备注: 123456
创建时间: 2026-04-08 15:30:00
━━━━━━━━━━━━━━━━━━━━

[✅ 确认收款]  [❌ 拒绝订单]

⏰ 订单将在30分钟后自动过期
```

## 使用流程

### 场景1: 购卡订单
```
用户购买商品 
  ↓
系统创建订单
  ↓
PushPlus推送到你的微信
  ↓
你点击"确认收款"
  ↓
系统自动发卡(生成提取码 + 发送邮件)
```

### 场景2: NewAPI充值
```
NewAPI发起充值
  ↓
系统创建订单
  ↓
PushPlus推送到你的微信
  ↓
你点击"确认收款"
  ↓
系统回调NewAPI充值
```

## 订单超时机制

- 订单创建时自动设置30分钟过期时间
- 定时任务每5分钟检查一次过期订单
- 过期订单自动标记为"已过期"状态
- 用户会看到订单过期提示

## 安全机制

1. **Token验证**: 每个确认链接包含一次性Token
2. **二次验证**: 需要myToken二次验证
3. **防重复**: 订单确认后Token立即失效
4. **状态检查**: 已处理/已失败的订单无法重复操作

## 故障排查

### 推送未收到

1. 检查Token是否正确配置
2. 检查 `pushplus.enabled` 是否为 `true`
3. 查看日志: `docker logs paypro | grep PushPlus`
4. 确认PushPlus账号状态正常

### 点击按钮无反应

1. 检查 `paypro.site` 配置是否正确
2. 确认服务器可从公网访问
3. 检查Token是否已过期(14天有效期)

### 推送成功但邮件未发送

- PushPlus推送和邮件通知是独立的
- 邮件失败不影响推送功能
- 检查邮件配置是否正确

## 高级配置

### 自定义过期时间

编辑 `application.yml`:

```yaml
paypro:
  token:
    expire: 14  # Token有效期(天)
  order:
    timeoutMinutes: 30  # 订单超时时间(分钟)
```

### 禁用邮件通知(仅使用PushPlus)

如果你只想用PushPlus推送，不想收邮件:

```yaml
paypro:
  email:
    receiver: ""  # 留空即可
```

## PushPlus免费额度

- 每天200条消息
- 对于个人使用完全足够
- 超出额度可升级付费版

## 注意事项

1. **保护Token**: 不要泄露你的PushPlus Token
2. **网络要求**: 服务器需要能访问 `www.pushplus.plus`
3. **微信限制**: 确保微信已关注PushPlus公众号
4. **链接有效期**: 确认链接14天内有效

## 与现有邮件通知的关系

- PushPlus推送和邮件通知**同时工作**
- 互不影响，可以都启用
- 推荐: 启用PushPlus，保留邮件作为备份

## 技术实现

相关代码文件:
- `PushPlusUtils.java` - 推送工具类
- `PushPlusService.java` - 推送服务接口
- `PushPlusServiceImpl.java` - 推送服务实现
- `OrderServiceImpl.java` - 订单服务(集成推送)
- `OrderTimeoutJob.java` - 订单超时检查定时任务

## 更新日志

- 2026-04-08: 初始版本发布
  - 支持订单审核推送
  - 支持移动端一键确认/拒绝
  - 支持订单自动过期
