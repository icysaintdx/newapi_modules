-- 推送通知配置SQL
-- 直接在数据库中插入配置，无需重新编译

-- PushPlus配置
INSERT INTO t_system_config (config_key, config_value, config_group, description, is_sensitive, create_time, update_time)
VALUES
('pushplus.enabled', 'false', 'notification', 'PushPlus推送是否启用', 0, NOW(), NOW()),
('pushplus.token', '', 'notification', 'PushPlus Token', 1, NOW(), NOW()),
('pushplus.daily_limit', '190', 'notification', 'PushPlus每日推送限额', 0, NOW(), NOW()),

-- Server酱配置
('serverchan.enabled', 'false', 'notification', 'Server酱推送是否启用', 0, NOW(), NOW()),
('serverchan.sendkey', '', 'notification', 'Server酱 SendKey', 1, NOW(), NOW()),
('serverchan.daily_limit', '5', 'notification', 'Server酱每日推送限额', 0, NOW(), NOW())

ON DUPLICATE KEY UPDATE
    config_value = VALUES(config_value),
    update_time = NOW();
