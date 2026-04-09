package com.wendao.controller;

import com.wendao.model.ResponseVO;
import com.wendao.service.SystemConfigService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 公开配置接口（无需认证）
 */
@RestController
@RequestMapping("/api/config")
@Api(tags = "公开配置接口")
public class PublicConfigController {

    @Autowired
    private SystemConfigService systemConfigService;

    /**
     * 获取前端页面需要的配置
     */
    @GetMapping("/public")
    @ApiOperation("获取公开配置")
    public ResponseVO getPublicConfig() {
        try {
            Map<String, String> config = new HashMap<>();

            // 系统配置
            config.put("siteName", systemConfigService.getConfigValue("site.name", "PayPro"));
            config.put("siteTitle", systemConfigService.getConfigValue("site.title", "PayPro个人收款系统"));
            config.put("siteUrl", systemConfigService.getConfigValue("site.url", "https://pay.isaint.cc"));
            config.put("siteAuthor", systemConfigService.getConfigValue("site.author", "codewendao"));
            config.put("supportEmail", systemConfigService.getConfigValue("site.support_email", "support@example.com"));

            // 加载所有页面文案配置（51个配置项）
            String[] pageTextKeys = {
                "page.common.footer", "page.common.logo", "page.common.secure_payment",
                "page.index.redirect_title", "page.index.loading_badge", "page.index.loading_message",
                "page.index.loading_subtitle", "page.index.btn_goto", "page.index.btn_pay",
                "page.payment.title", "page.payment.header_title", "page.payment.order_number",
                "page.payment.pay_method", "page.payment.pay_id", "page.payment.amount",
                "page.payment.status_success", "page.payment.status_timeout", "page.payment.status_failed",
                "page.payment.btn_back", "page.payment.btn_open_alipay",
                "page.payment.wechat_notice_title", "page.payment.wechat_notice_btn",
                "page.recharge.title", "page.recharge.header_title", "page.recharge.header_subtitle",
                "page.help.title", "page.help.header_subtitle",
                "page.help.tab_recharge", "page.help.tab_history", "page.help.tab_help",
                "page.admin.login.title", "page.admin.index.title", "page.admin.settings.title",
                "page.admin.settings.tab_basic", "page.admin.settings.tab_email",
                "page.admin.settings.tab_payment", "page.admin.settings.tab_page",
                "page.admin.settings.tab_qrcode", "page.admin.settings.tab_security",
                "page.admin.settings.btn_save", "page.admin.settings.logout",
                "page.order.status_0", "page.order.status_1", "page.order.status_2",
                "page.order.status_4", "page.order.status_5",
                "page.success.title", "page.success.message", "page.success.thanks",
                "page.error.title", "page.error.message"
            };

            for (String key : pageTextKeys) {
                String value = systemConfigService.getConfigValue(key);
                if (value != null) {
                    // 转换配置键为驼峰命名（page.common.footer -> pageCommonFooter）
                    String camelKey = key.replace("page.", "").replace(".", "_");
                    config.put(camelKey, value);
                }
            }

            // 充值表单字段配置
            config.put("showDescription", systemConfigService.getConfigValue("recharge.form.show_description", "true"));
            config.put("showEmail", systemConfigService.getConfigValue("recharge.form.show_email", "true"));
            config.put("requireEmail", systemConfigService.getConfigValue("recharge.form.require_email", "true"));

            // 帮助页面联系方式配置
            config.put("showContactInfo", systemConfigService.getConfigValue("help.show_contact_info", "false"));
            config.put("contactEmail", systemConfigService.getConfigValue("help.contact_email", ""));

            return ResponseVO.successResponse(config);
        } catch (Exception e) {
            return ResponseVO.errorResponse("获取配置失败: " + e.getMessage());
        }
    }
}
