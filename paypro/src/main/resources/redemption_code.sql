-- ==========================================
-- 兑换码管理系统 - 数据库表结构
-- ==========================================

-- 兑换码表
CREATE TABLE IF NOT EXISTS `t_redemption_code` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `code` varchar(100) NOT NULL COMMENT '兑换码',
    `product_id` int(11) NOT NULL COMMENT '关联商品ID',
    `status` tinyint(1) NOT NULL DEFAULT 0 COMMENT '状态：0-未使用，1-已使用，2-已锁定',
    `order_id` varchar(255) DEFAULT NULL COMMENT '关联订单ID（已使用时）',
    `used_time` datetime DEFAULT NULL COMMENT '使用时间',
    `lock_time` datetime DEFAULT NULL COMMENT '锁定时间（防止并发）',
    `lock_token` varchar(64) DEFAULT NULL COMMENT '锁定令牌（UUID）',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `batch_id` varchar(64) DEFAULT NULL COMMENT '批次ID（批量导入时的批次标识）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code` (`code`),
    KEY `idx_product_status` (`product_id`, `status`),
    KEY `idx_order_id` (`order_id`),
    KEY `idx_lock_time` (`lock_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='兑换码表';

-- 扩展商品表，添加兑换码相关字段
ALTER TABLE `t_product`
ADD COLUMN `code_type` varchar(50) DEFAULT NULL COMMENT '兑换码类型：NEWAPI等',
ADD COLUMN `total_codes` int(11) DEFAULT 0 COMMENT '总兑换码数量',
ADD COLUMN `used_codes` int(11) DEFAULT 0 COMMENT '已使用兑换码数量',
ADD COLUMN `stock_alert` int(11) DEFAULT 10 COMMENT '库存预警阈值';

-- 订单表添加兑换码字段
ALTER TABLE `t_order`
ADD COLUMN `redemption_code_id` bigint(20) DEFAULT NULL COMMENT '关联的兑换码ID',
ADD COLUMN `code_sent` tinyint(1) DEFAULT 0 COMMENT '兑换码是否已发送：0-未发送，1-已发送',
ADD COLUMN `code_sent_time` datetime DEFAULT NULL COMMENT '兑换码发送时间',
ADD COLUMN `download_url` varchar(500) DEFAULT NULL COMMENT '下载链接（如果有）';

-- 发卡日志表（用于审计和防止重复发卡）
CREATE TABLE IF NOT EXISTS `t_card_delivery_log` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `order_id` varchar(255) NOT NULL COMMENT '订单ID',
    `redemption_code_id` bigint(20) NOT NULL COMMENT '兑换码ID',
    `code` varchar(100) NOT NULL COMMENT '兑换码内容',
    `email` varchar(100) NOT NULL COMMENT '收件人邮箱',
    `status` tinyint(1) NOT NULL COMMENT '发送状态：0-失败，1-成功',
    `error_msg` varchar(500) DEFAULT NULL COMMENT '错误信息',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `idempotent_key` varchar(128) NOT NULL COMMENT '幂等键（订单ID+时间戳哈希）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_idempotent_key` (`idempotent_key`),
    KEY `idx_order_id` (`order_id`),
    KEY `idx_code_id` (`redemption_code_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='发卡日志表';

-- 创建索引以优化查询性能
CREATE INDEX idx_batch_id ON t_redemption_code(batch_id);
CREATE INDEX idx_create_time ON t_redemption_code(create_time);
