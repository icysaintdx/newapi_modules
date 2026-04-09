# 安全迁移测试指南

本文档指导你如何安全地测试 `newapi-enhancements` 项目，同时保留旧目录作为备份。

## 当前状态

```
/docker/
├── new-api/              # ✅ 保留 - New-API 原始容器
├── channel-status/       # ⚠️  保留作为备份 - 旧的监控模块
├── paypro/              # ⚠️  保留作为备份 - 旧的支付模块
└── newapi-enhancements/ # ✨ 新的统一项目（测试中）
```

## 迁移策略

**阶段 1：测试部署（当前）**
- 停止旧容器
- 使用 `newapi-enhancements` 部署新容器
- 测试所有功能
- 如果有问题，立即回滚到旧目录

**阶段 2：确认稳定（测试通过后）**
- 运行 1-2 天，确认无问题
- 备份旧目录
- 删除旧目录

## 测试步骤

### 1. 备份当前状态

```bash
# 备份旧目录（以防万一）
cd /docker
tar -czf backup-old-dirs-$(date +%Y%m%d).tar.gz channel-status paypro

# 备份数据库
docker exec postgres pg_dump -U root new-api > /tmp/new-api-backup.sql
docker exec paypro-mysql mysqldump -uroot -ppaypro123456 pay > /tmp/paypro-backup.sql
```

### 2. 停止旧容器（不删除）

```bash
# 只停止，不删除容器和数据
docker stop channel-status paypro paypro-mysql
```

### 3. 使用新项目部署

```bash
cd /docker/newapi-enhancements

# 先构建 channel-status 镜像
bash scripts/deploy-status.sh

# 部署支付系统（使用 docker-compose）
cd paypro
docker-compose up -d
cd ..
```

### 4. 验证部署

```bash
# 检查容器状态
docker ps | grep -E "channel-status|paypro"

# 应该看到：
# - channel-status (运行中)
# - paypro (运行中)
# - paypro-mysql (运行中)

# 测试各个服务
curl http://localhost:2086  # 渠道状态
curl http://localhost:8889  # 支付系统
```

### 5. 功能测试清单

- [ ] 渠道状态页能正常访问
- [ ] 渠道状态页显示正确的数据
- [ ] 支付系统能正常访问
- [ ] 支付系统数据库连接正常
- [ ] 主题切换器正常工作（需要配置 nginx）
- [ ] 所有容器日志无错误

### 6. 如果出现问题 - 快速回滚

```bash
# 停止新容器
docker stop channel-status paypro paypro-mysql
docker rm channel-status paypro paypro-mysql

# 启动旧容器
cd /docker/channel-status
docker start channel-status

cd /docker/paypro
docker-compose up -d
```

## 新旧对比

### 旧配置（备份）
```
/docker/channel-status/
├── Dockerfile
├── main.go
└── channel-status (编译后的二进制)

/docker/paypro/
├── docker-compose.yml
├── Dockerfile
└── src/
```

### 新配置（测试中）
```
/docker/newapi-enhancements/
├── channel-status/
│   ├── Dockerfile
│   ├── main.go
│   └── README.md
├── paypro/
│   ├── docker-compose.yml
│   ├── Dockerfile
│   ├── src/
│   └── newapi-integration/
├── nginx/
├── scripts/
└── README.md
```

## 关键差异

### 1. 网络配置

**旧配置：**
- paypro 使用独立网络 `paypro-network`
- channel-status 连接到 `new-api_new-api-network`

**新配置：**
- 所有服务统一连接到 `new-api_new-api-network`
- 更好的服务间通信

### 2. 数据库配置

**旧配置：**
- paypro-mysql 密码：`paypro123456`
- 数据库名：`pay`

**新配置：**
- paypro-mysql 密码：`123456`（需要修改）
- 数据库名：`paypro`（需要修改）

⚠️ **重要：需要调整数据库配置以匹配旧配置**

## 需要修改的配置

### 修改 1：paypro docker-compose.yml

```bash
cd /docker/newapi-enhancements/paypro
nano docker-compose.yml
```

修改以下内容以匹配旧配置：
```yaml
environment:
  MYSQL_ROOT_PASSWORD: paypro123456  # 改为旧密码
  MYSQL_DATABASE: pay                # 改为旧数据库名
```

### 修改 2：连接到 new-api 网络

确保 paypro 的 docker-compose.yml 连接到正确的网络：

```yaml
networks:
  paypro-network:
    external: true
    name: new-api_new-api-network  # 连接到 new-api 网络
```

## 测试成功标准

✅ 所有容器正常运行 24 小时无错误
✅ 渠道状态页数据准确
✅ 支付系统功能正常
✅ 日志无异常错误
✅ 性能无明显下降

## 测试通过后

```bash
# 1. 再运行几天确认稳定

# 2. 删除旧容器（保留数据卷）
docker rm channel-status paypro paypro-mysql

# 3. 重命名旧目录（不删除，以防万一）
cd /docker
mv channel-status channel-status.backup
mv paypro paypro.backup

# 4. 一个月后确认无问题，删除备份
rm -rf channel-status.backup paypro.backup
```

## 回滚计划

如果测试失败，按以下步骤回滚：

1. 停止新容器
2. 恢复数据库备份（如果数据被修改）
3. 启动旧容器
4. 检查日志，找出问题原因
5. 修复 `newapi-enhancements` 中的问题
6. 重新测试

## 常见问题

### Q: 数据会丢失吗？
A: 不会。我们使用 Docker volume 持久化数据，停止容器不会删除数据。

### Q: 如果测试失败怎么办？
A: 立即停止新容器，启动旧容器。旧目录和数据都还在。

### Q: 需要停机多久？
A: 大约 5-10 分钟（停止旧容器 → 启动新容器）。

### Q: nginx 配置需要改吗？
A: 不需要。容器名称和端口都保持一致。

## 下一步

完成测试后，查看 `DEPLOYMENT.md` 了解如何在新服务器上从零部署。
