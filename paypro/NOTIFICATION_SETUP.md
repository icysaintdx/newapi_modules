# 多渠道通知配置指南

## 功能说明

多渠道通知系统支持**PushPlus微信推送**、**Server酱推送**和**邮件通知**三种方式，自动选择最优渠道发送订单审核通知。

### 核心优势
- ✅ **智能降级**: PushPlus → Server酱 → 邮件，自动切换
- ✅ **额度管理**: 自动统计每日推送次数，超额自动切换渠道
- ✅ **移动端操作**: 微信收到推送，点击按钮即可处理
- ✅ **零成本**: 免费额度足够个人使用
- ✅ **双场景支持**: NewAPI充值 + 购卡发卡都支持

### 推送策略

```
优先级: PushPlus微信 > Server酱 > 邮件

逻辑:
1. 每日前190条 → PushPlus微信推送
2. 第191-195条 → Server酱推送
3. 超过195条或所有渠道失败 → 邮件托底
```

---

## 一、PushPlus配置

### 1. 获取Token

1. 访问 [PushPlus官网](https://www.pushplus.plus)
2. 使用微信扫码登录
3. 进入"发送消息" → "一对一消息"
4. 复制你的Token

### 2. 配置PayPro

编辑 `application.yml` 或使用环境变量:

```yaml
paypro:
  pushplus:
    token: your_pushplus_token_here
    enabled: true
    daily-limit: 190  # 每日限额（预留10条缓冲）
```

或使用环境变量（推荐）:

```bash
# docker-compose.yml
environment:
  - PUSHPLUS_TOKEN=your_pushplus_token_here
  - PUSHPLUS_ENABLED=true
  - PUSHPLUS_DAILY_LIMIT=190
```

### 3. PushPlus免费额度

- **微信推送**: 200条/天
- **Webhook**: 无限制（需自建接收端，不适合我们）
- **邮件推送**: 200条/天（不推荐，我们有自己的邮件）

---

## 二、Server酱配置

### 1. 获取SendKey

1. 访问 [Server酱官网](https://sctapi.ftqq.com)
2. 使用微信扫码登录
3. 进入"SendKey" → 复制你的SendKey

### 2. 配置PayPro

```yaml
paypro:
  serverchan:
    sendkey: your_serverchan_sendkey_here
    enabled: true
    daily-limit: 5  # 免费版每日限额
```

或使用环境变量:

```bash
environment:
  - SERVERCHAN_SENDKEY=your_sendkey_here
  - SERVERCHAN_ENABLED=true
  - SERVERCHAN_DAILY_LIMIT=5
```

### 3. Server酱免费额度

- **免费版**: 5条/天
- **付费版**: 500条/天（5元/月）

---

## 三、邮件配置（托底）

邮件配置已存在，作为最终托底方案:

```yaml
paypro:
  email:
    receiver: admin@example.com
    sender: noreply@example.com
```

---

## 四、完整配置示例

### docker-compose.yml

```yaml
services:
  paypro:
    environment:
      # PushPlus配置
      - PUSHPLUS_TOKEN=your_pushplus_token
      - PUSHPLUS_ENABLED=true
      - PUSHPLUS_DAILY_LIMIT=190
      
      # Server酱配置
      - SERVERCHAN_SENDKEY=your_serverchan_key
      - SERVERCHAN_ENABLED=true
      - SERVERCHAN_DAILY_LIMIT=5
      
      # 邮件配置（已有）
      - SPRING_MAIL_USERNAME=your_email@qq.com
      - SPRING_MAIL_PASSWORD=your_auth_code
```

---

## 五、推送消息效果

### PushPlus微信推送（HTML格式）

```
💰 新购卡订单 - ¥10.00
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

💡 提示: 收款码永久有效，无需担心超时
```

### Server酱推送（Markdown格式）

```markdown
## 💰 收到新订单

**订单类型:** 购卡订单
**金额:** ¥10.00
**商品:** GPT-4 兑换码
**用户邮箱:** user@example.com
**支付方式:** 微信支付
**支付备注:** 123456
**创建时间:** 2026-04-08 15:30:00

---

[✅ 确认收款](https://your-domain.com/order/pass?...)
[❌ 拒绝订单](https://your-domain.com/order/back?...)

💡 提示: 收款码永久有效，无需担心超时
```

---

## 六、使用流程

### 场景1: 购卡订单

```
用户购买商品 
  ↓
系统创建订单
  ↓
多渠道推送到你的微信
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
多渠道推送到你的微信
  ↓
你点击"确认收款"
  ↓
系统回调NewAPI充值
```

---

## 七、订单超时逻辑（重要）

### 核心问题

```
收款码永久有效 ≠ 订单永久有效

问题：
- 用户创建订单后不支付，订单30分钟"过期"
- 但收款码还能用，用户1小时后支付了
- 系统收到钱但找不到有效订单
```

### 解决方案：软过期 + 延长查询

```java
// OrderServiceImpl.java:432
// 查询最近7天的订单（包括已过期的）
// 原因：收款码永久有效，用户可能延迟支付

查询逻辑:
1. 查询最近7天的订单
2. 包括"待支付"和"已过期"状态
3. 按创建时间倒序，最新的优先
4. 匹配到订单后正常处理
```

### 过期提醒的作用

- **前端显示**: 提示用户尽快支付
- **不影响支付**: 过期后仍可支付，系统会匹配到订单
- **避免混乱**: 7天后订单不再匹配，避免误匹配

---

## 八、额度统计机制

### Redis计数器

```
Key格式: notification:pushplus:count:2026-04-08
Value: 当日已推送次数
过期时间: 1天

逻辑:
1. 推送前检查计数器
2. 超过限额则切换下一渠道
3. 推送成功后计数器+1
```

### 查看当日额度

```bash
# 进入Redis
docker exec -it paypro-redis redis-cli

# 查看PushPlus今日使用量
GET notification:pushplus:count:2026-04-08

# 查看Server酱今日使用量
GET notification:serverchan:count:2026-04-08
```

---

## 九、故障排查

### 推送未收到

1. **检查配置**
```bash
docker logs paypro | grep "PushPlus\|Server酱"
```

2. **检查Token/SendKey是否正确**
```bash
# 查看环境变量
docker exec paypro env | grep PUSHPLUS
docker exec paypro env | grep SERVERCHAN
```

3. **检查额度**
```bash
# 进入Redis查看计数
docker exec -it paypro-redis redis-cli
GET notification:pushplus:count:2026-04-08
```

4. **手动测试推送**
```bash
# 测试PushPlus
curl -X POST "http://www.pushplus.plus/send" \
  -H "Content-Type: application/json" \
  -d '{"token":"your_token","title":"测试","content":"测试内容","template":"html"}'

# 测试Server酱
curl -X POST "https://sctapi.ftqq.com/your_sendkey.send" \
  -d "title=测试&desp=测试内容"
```

### 点击按钮无反应

1. 检查 `paypro.site` 配置是否正确
2. 确认服务器可从公网访问
3. 检查Token是否已过期(14天有效期)

### 邮件托底未触发

- 检查邮件配置是否正确
- 查看日志: `docker logs paypro | grep "邮件"`

---

## 十、推荐配置

### 个人使用（订单量<50/天）

```yaml
pushplus:
  enabled: true
  daily-limit: 190
serverchan:
  enabled: false  # 不需要
```

### 中等流量（订单量50-200/天）

```yaml
pushplus:
  enabled: true
  daily-limit: 190
serverchan:
  enabled: true
  daily-limit: 5
```

### 大流量（订单量>200/天）

建议升级Server酱付费版或考虑其他方案:
- 支付宝当面付（完全自动化）
- Android Hook监控（24小时自动化）

---

## 十一、安全建议

1. **保护Token/SendKey**: 不要提交到Git
2. **使用环境变量**: 通过docker-compose.yml配置
3. **定期检查额度**: 避免超额后无通知
4. **邮件托底**: 始终保持邮件配置可用

---

## 十二、技术实现

相关代码文件:
- `PushPlusUtils.java` - PushPlus推送工具
- `ServerChanUtils.java` - Server酱推送工具
- `MultiChannelNotificationService.java` - 多渠道通知服务接口
- `MultiChannelNotificationServiceImpl.java` - 多渠道通知服务实现
- `OrderServiceImpl.java` - 订单服务(集成通知)
- `OrderTimeoutJob.java` - 订单超时检查定时任务

---

## 十三、更新日志

- 2026-04-08: 初始版本发布
  - 支持PushPlus微信推送
  - 支持Server酱推送
  - 支持邮件托底
  - 支持智能降级
  - 支持额度管理
  - 优化订单超时逻辑（支持收款码永久有效）
