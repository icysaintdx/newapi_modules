-- 提取码表
CREATE TABLE IF NOT EXISTS `t_extract_code` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `code` varchar(6) NOT NULL COMMENT '提取码（6位数字）',
    `order_id` varchar(255) NOT NULL COMMENT '关联订单ID',
    `redemption_code_id` bigint(20) DEFAULT NULL COMMENT '关联兑换码ID',
    `redemption_code` varchar(100) DEFAULT NULL COMMENT '兑换码内容（冗余存储）',
    `status` tinyint(1) NOT NULL DEFAULT 0 COMMENT '状态：0-未使用，1-已使用',
    `used_time` datetime DEFAULT NULL COMMENT '使用时间',
    `expire_time` datetime NOT NULL COMMENT '过期时间（24小时）',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code` (`code`),
    KEY `idx_order_id` (`order_id`),
    KEY `idx_status_expire` (`status`, `expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='提取码表';
