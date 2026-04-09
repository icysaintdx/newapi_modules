-- PayPro 完整系统配置初始化脚本
-- 清空现有配置（可选）
-- TRUNCATE TABLE t_system_config;

-- ============================================
-- 1. 系统基础配置组 (system)
-- ============================================
INSERT INTO `t_system_config` (`config_key`, `config_value`, `config_type`, `config_group`, `description`, `is_sensitive`) VALUES
('site.name', 'PayPro', 'STRING', 'system', '网站名称', 0),
('site.url', 'https://pay.isaint.cc', 'STRING', 'system', '网站地址', 0),
('site.title', 'PayPro个人收款系统', 'STRING', 'system', '网站标题', 0),
('site.author', 'codewendao', 'STRING', 'system', '作者名称', 0),
('site.download_url', 'https://github.com/codewendao/PayPro', 'STRING', 'system', '项目下载地址', 0),
('site.support_email', 'support@example.com', 'STRING', 'system', '支持邮箱', 0),
('site.qrcode_num', '2', 'NUMBER', 'system', '二维码数量', 0)
ON DUPLICATE KEY UPDATE 
    config_value = VALUES(config_value),
    description = VALUES(description),
    config_type = VALUES(config_type),
    config_group = VALUES(config_group),
    is_sensitive = VALUES(is_sensitive);

-- ============================================
-- 2. 邮件配置组 (email)
-- ============================================
INSERT INTO `t_system_config` (`config_key`, `config_value`, `config_type`, `config_group`, `description`, `is_sensitive`) VALUES
-- 邮件服务商选择
('email.provider', 'custom', 'STRING', 'email', '邮件服务商 (custom/resend/brevo/smtp2go/mailersend/qq/163/gmail/outlook)', 0),

-- 基础配置
('email.smtp_host', 'smtp.qq.com', 'STRING', 'email', 'SMTP服务器地址', 0),
('email.smtp_port', '465', 'NUMBER', 'email', 'SMTP端口', 0),
('email.protocol', 'smtps', 'STRING', 'email', '邮件协议 (smtp/smtps)', 0),
('email.username', '', 'STRING', 'email', '邮箱账号/API Key', 1),
('email.password', '', 'STRING', 'email', '邮箱授权码/API Secret', 1),
('email.sender', '', 'STRING', 'email', '邮件发送者', 0),
('email.receiver', '', 'STRING', 'email', '审核邮件接收者', 0),

-- Resend 配置 (3000封/月免费, $20/月 5万封)
('email.resend.api_key', '', 'STRING', 'email', 'Resend API Key', 1),
('email.resend.from', '', 'STRING', 'email', 'Resend 发件人地址', 0),

-- Brevo (原 Sendinblue) 配置 (9000封/月免费, $25/月)
('email.brevo.api_key', '', 'STRING', 'email', 'Brevo API Key', 1),
('email.brevo.from_name', '', 'STRING', 'email', 'Brevo 发件人名称', 0),
('email.brevo.from_email', '', 'STRING', 'email', 'Brevo 发件人邮箱', 0),

-- SMTP2Go 配置 (1000封/月免费, $16/月 1万封)
('email.smtp2go.api_key', '', 'STRING', 'email', 'SMTP2Go API Key', 1),
('email.smtp2go.smtp_user', '', 'STRING', 'email', 'SMTP2Go SMTP用户名', 1),
('email.smtp2go.smtp_pass', '', 'STRING', 'email', 'SMTP2Go SMTP密码', 1),

-- MailerSend 配置 (6000封/月免费, $28/月 1万封)
('email.mailersend.api_key', '', 'STRING', 'email', 'MailerSend API Key', 1),
('email.mailersend.from_email', '', 'STRING', 'email', 'MailerSend 发件人邮箱', 0),
('email.mailersend.from_name', '', 'STRING', 'email', 'MailerSend 发件人名称', 0)

ON DUPLICATE KEY UPDATE
    config_value = VALUES(config_value),
    description = VALUES(description),
    config_type = VALUES(config_type),
    config_group = VALUES(config_group),
    is_sensitive = VALUES(is_sensitive);

-- ============================================
-- 3. 支付宝配置组 (payment)
-- ============================================
INSERT INTO `t_system_config` (`config_key`, `config_value`, `config_type`, `config_group`, `description`, `is_sensitive`) VALUES
('alipay.custom.qr.url', '', 'STRING', 'payment', '支付宝个人收款码URL', 0),
('alipay.user.id', '', 'STRING', 'payment', '支付宝UserId', 0),
('alipay.dmf.app.id', '', 'STRING', 'payment', '支付宝当面付AppId', 1),
('alipay.dmf.app.private.key', '', 'TEXT', 'payment', '支付宝应用私钥', 1),
('alipay.dmf.public.key', '', 'TEXT', 'payment', '支付宝公钥', 1),
('alipay.dmf.subject', '向PayPro作者捐赠', 'STRING', 'payment', '支付宝当面付主题', 0),
('wechat.qr.url', '', 'STRING', 'payment', '微信收款码URL', 0),
('wechat.zs.qr.url', '', 'STRING', 'payment', '微信赞赏码URL', 0)
ON DUPLICATE KEY UPDATE 
    config_value = VALUES(config_value),
    description = VALUES(description),
    config_type = VALUES(config_type),
    config_group = VALUES(config_group),
    is_sensitive = VALUES(is_sensitive);

-- ============================================
-- 4. 支付方式配置组 (payment_method)
-- ============================================
INSERT INTO `t_system_config` (`config_key`, `config_value`, `config_type`, `config_group`, `description`, `is_sensitive`) VALUES
('payment.method.alipay.enabled', 'true', 'BOOLEAN', 'payment_method', '启用支付宝支付', 0),
('payment.method.alipay.name', '支付宝支付', 'STRING', 'payment_method', '支付宝支付名称', 0),
('payment.method.alipay.description', '免输备注，手动收款', 'STRING', 'payment_method', '支付宝支付描述', 0),
('payment.method.alipay.allow_night', 'true', 'BOOLEAN', 'payment_method', '支付宝允许夜间支付', 0),
('payment.method.alipay.use_local_qr', 'true', 'BOOLEAN', 'payment_method', '支付宝使用本地二维码', 0),
('payment.method.alipay.sort', '1', 'NUMBER', 'payment_method', '支付宝显示顺序', 0),

('payment.method.wechat.enabled', 'true', 'BOOLEAN', 'payment_method', '启用微信支付', 0),
('payment.method.wechat.name', '微信支付', 'STRING', 'payment_method', '微信支付名称', 0),
('payment.method.wechat.description', '需备注，自动确认收款', 'STRING', 'payment_method', '微信支付描述', 0),
('payment.method.wechat.allow_night', 'true', 'BOOLEAN', 'payment_method', '微信允许夜间支付', 0),
('payment.method.wechat.use_local_qr', 'true', 'BOOLEAN', 'payment_method', '微信使用本地二维码', 0),
('payment.method.wechat.sort', '2', 'NUMBER', 'payment_method', '微信显示顺序', 0),

('payment.method.wechat_zs.enabled', 'true', 'BOOLEAN', 'payment_method', '启用微信赞赏码', 0),
('payment.method.wechat_zs.name', '微信赞赏码', 'STRING', 'payment_method', '微信赞赏码名称', 0),
('payment.method.wechat_zs.description', '需备注，自动确认收款', 'STRING', 'payment_method', '微信赞赏码描述', 0),
('payment.method.wechat_zs.allow_night', 'false', 'BOOLEAN', 'payment_method', '微信赞赏码允许夜间支付', 0),
('payment.method.wechat_zs.use_local_qr', 'true', 'BOOLEAN', 'payment_method', '微信赞赏码使用本地二维码', 0),
('payment.method.wechat_zs.sort', '3', 'NUMBER', 'payment_method', '微信赞赏码显示顺序', 0),

('payment.method.alipay_dmf.enabled', 'true', 'BOOLEAN', 'payment_method', '启用支付宝当面付', 0),
('payment.method.alipay_dmf.name', '支付宝当面付', 'STRING', 'payment_method', '支付宝当面付名称', 0),
('payment.method.alipay_dmf.description', '官方产品，免备注自动收款', 'STRING', 'payment_method', '支付宝当面付描述', 0),
('payment.method.alipay_dmf.allow_night', 'true', 'BOOLEAN', 'payment_method', '支付宝当面付允许夜间支付', 0),
('payment.method.alipay_dmf.use_local_qr', 'false', 'BOOLEAN', 'payment_method', '支付宝当面付使用本地二维码', 0),
('payment.method.alipay_dmf.sort', '4', 'NUMBER', 'payment_method', '支付宝当面付显示顺序', 0)
ON DUPLICATE KEY UPDATE 
    config_value = VALUES(config_value),
    description = VALUES(description),
    config_type = VALUES(config_type),
    config_group = VALUES(config_group),
    is_sensitive = VALUES(is_sensitive);

-- ============================================
-- 5. 安全配置组 (security)
-- ============================================
INSERT INTO `t_system_config` (`config_key`, `config_value`, `config_type`, `config_group`, `description`, `is_sensitive`) VALUES
('security.rate_limit.ip_expire', '2', 'NUMBER', 'security', 'IP限流时间(秒)', 0),
('security.token.value', '123', 'STRING', 'security', '二次验证token值', 1),
('security.token.expire', '14', 'NUMBER', 'security', 'Token过期时间(天)', 0),
('security.openapi.secret', 'newapi_paypro_secret_2026', 'STRING', 'security', 'OpenAPI密钥', 1)
ON DUPLICATE KEY UPDATE 
    config_value = VALUES(config_value),
    description = VALUES(description),
    config_type = VALUES(config_type),
    config_group = VALUES(config_group),
    is_sensitive = VALUES(is_sensitive);

-- ============================================
-- 6. 页面文案配置组 (page_text)
-- ============================================
INSERT INTO `t_system_config` (`config_key`, `config_value`, `config_type`, `config_group`, `description`, `is_sensitive`) VALUES
-- 通用文案
('page.common.footer', '© 2026 PayPro System. Designed for Creators.', 'STRING', 'page_text', '页脚版权信息', 0),
('page.common.logo', 'PayPro', 'STRING', 'page_text', '系统Logo文字', 0),
('page.common.secure_payment', '安全支付', 'STRING', 'page_text', '安全支付标识', 0),

-- 首页文案
('page.index.redirect_title', 'PayPro - 跳转中', 'STRING', 'page_text', '跳转页标题', 0),
('page.index.loading_badge', '系统启动中', 'STRING', 'page_text', '加载徽章文字', 0),
('page.index.loading_message', '正在为您准备最佳体验', 'STRING', 'page_text', '加载提示信息', 0),
('page.index.loading_subtitle', '个人支付从未如此简单', 'STRING', 'page_text', '加载副标题', 0),
('page.index.btn_goto', '立即前往', 'STRING', 'page_text', '立即前往按钮', 0),
('page.index.btn_pay', '直接支付', 'STRING', 'page_text', '直接支付按钮', 0),

-- 支付页面文案
('page.payment.title', '支付中心 - PayPro', 'STRING', 'page_text', '支付页面标题', 0),
('page.payment.header_title', '支付中心', 'STRING', 'page_text', '支付页面头部标题', 0),
('page.payment.order_number', '订单号', 'STRING', 'page_text', '订单号标签', 0),
('page.payment.pay_method', '支付方式', 'STRING', 'page_text', '支付方式标签', 0),
('page.payment.pay_id', '标识号', 'STRING', 'page_text', '标识号标签', 0),
('page.payment.amount', '支付金额', 'STRING', 'page_text', '支付金额标签', 0),
('page.payment.status_success', '支付成功', 'STRING', 'page_text', '支付成功提示', 0),
('page.payment.status_timeout', '订单已超时', 'STRING', 'page_text', '订单超时提示', 0),
('page.payment.status_failed', '订单失败', 'STRING', 'page_text', '订单失败提示', 0),
('page.payment.btn_back', '返回捐赠', 'STRING', 'page_text', '返回按钮', 0),
('page.payment.btn_open_alipay', '打开支付宝（手机点击有效）', 'STRING', 'page_text', '打开支付宝按钮', 0),
('page.payment.wechat_notice_title', '请在备注中输入支付标识号', 'STRING', 'page_text', '微信支付提示标题', 0),
('page.payment.wechat_notice_btn', '知道了', 'STRING', 'page_text', '微信提示确认按钮', 0),

-- 充值页面文案
('page.recharge.title', '捐赠中心 - PayPro', 'STRING', 'page_text', '充值页面标题', 0),
('page.recharge.header_title', '捐赠中心', 'STRING', 'page_text', '充值页面头部标题', 0),
('page.recharge.header_subtitle', '支持我们的工作，让项目持续发展', 'STRING', 'page_text', '充值页面副标题', 0),

-- 帮助中心文案
('page.help.title', '帮助中心 - PayPro', 'STRING', 'page_text', '帮助中心标题', 0),
('page.help.header_subtitle', '解决您的支付疑问，提供专业的技术支持', 'STRING', 'page_text', '帮助中心副标题', 0),
('page.help.tab_recharge', '捐赠', 'STRING', 'page_text', '捐赠标签', 0),
('page.help.tab_history', '捐赠记录', 'STRING', 'page_text', '捐赠记录标签', 0),
('page.help.tab_help', '帮助中心', 'STRING', 'page_text', '帮助中心标签', 0),

-- 后台管理文案
('page.admin.login.title', '后台管理登录 - PayPro', 'STRING', 'page_text', '登录页标题', 0),
('page.admin.index.title', '后台管理 - PayPro', 'STRING', 'page_text', '控制台标题', 0),
('page.admin.settings.title', '系统配置 - PayPro', 'STRING', 'page_text', '系统配置标题', 0),
('page.admin.settings.tab_basic', '基本配置', 'STRING', 'page_text', '基本配置标签', 0),
('page.admin.settings.tab_email', '邮箱配置', 'STRING', 'page_text', '邮箱配置标签', 0),
('page.admin.settings.tab_payment', '支付配置', 'STRING', 'page_text', '支付配置标签', 0),
('page.admin.settings.tab_page', '页面文案', 'STRING', 'page_text', '页面文案标签', 0),
('page.admin.settings.tab_qrcode', '二维码管理', 'STRING', 'page_text', '二维码管理标签', 0),
('page.admin.settings.tab_security', '安全配置', 'STRING', 'page_text', '安全配置标签', 0),
('page.admin.settings.btn_save', '保存配置', 'STRING', 'page_text', '保存按钮', 0),
('page.admin.settings.logout', '退出登录', 'STRING', 'page_text', '退出登录按钮', 0),

-- 订单状态文案
('page.order.status_0', '待支付', 'STRING', 'page_text', '待支付状态', 0),
('page.order.status_1', '已支付', 'STRING', 'page_text', '已支付状态', 0),
('page.order.status_2', '已失败', 'STRING', 'page_text', '已失败状态', 0),
('page.order.status_4', '已扫码', 'STRING', 'page_text', '已扫码状态', 0),
('page.order.status_5', '已超时', 'STRING', 'page_text', '已超时状态', 0),

-- 成功/失败页面
('page.success.title', '操作成功', 'STRING', 'page_text', '成功页标题', 0),
('page.success.message', '恭喜您，支付成功！', 'STRING', 'page_text', '成功提示', 0),
('page.success.thanks', '十分感谢您的捐赠！', 'STRING', 'page_text', '感谢语', 0),
('page.error.title', '操作失败', 'STRING', 'page_text', '失败页标题', 0),
('page.error.message', '操作失败，请重试', 'STRING', 'page_text', '失败提示', 0)
ON DUPLICATE KEY UPDATE 
    config_value = VALUES(config_value),
    description = VALUES(description),
    config_type = VALUES(config_type),
    config_group = VALUES(config_group),
    is_sensitive = VALUES(is_sensitive);
