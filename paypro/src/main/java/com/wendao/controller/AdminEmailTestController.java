package com.wendao.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.wendao.common.utils.EmailUtils;
import com.wendao.model.ResponseVO;
import com.wendao.service.EmailService;
import com.wendao.service.SystemConfigService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 邮件测试控制器（仅超级管理员可访问）
 */
@RestController
@RequestMapping("/admin/api/email")
@Api(tags = "邮件测试")
@SaCheckRole("SUPER_ADMIN")
public class AdminEmailTestController {

    private static final Logger log = LoggerFactory.getLogger(AdminEmailTestController.class);

    @Autowired
    private EmailService emailService;

    @Autowired
    private SystemConfigService systemConfigService;

    /**
     * 发送测试邮件
     */
    @PostMapping("/test")
    @ApiOperation("发送测试邮件")
    public ResponseVO sendTestEmail(@RequestBody Map<String, String> params) {
        try {
            String testEmail = params.get("testEmail");

            if (testEmail == null || testEmail.isEmpty()) {
                return ResponseVO.errorResponse("请提供测试邮箱地址");
            }

            // 验证邮箱格式
            if (!EmailUtils.checkEmail(testEmail)) {
                return ResponseVO.errorResponse("邮箱格式不正确");
            }

            // 检查邮件配置是否完整
            if (!emailService.isConfigured()) {
                return ResponseVO.errorResponse("邮件配置不完整，请先配置邮箱信息");
            }

            // 发送测试邮件
            log.info("准备发送测试邮件到: {}, 服务商: {}", testEmail, emailService.getProviderType());
            String result = emailService.sendTestEmail(testEmail);

            log.info("测试邮件发送成功: {}", result);
            return ResponseVO.successResponse("测试邮件已发送，请检查收件箱（可能在垃圾邮件中）");

        } catch (Exception e) {
            log.error("发送测试邮件失败", e);
            return ResponseVO.errorResponse("发送失败: " + e.getMessage());
        }
    }

    /**
     * 获取当前邮箱配置状态
     */
    @GetMapping("/status")
    @ApiOperation("获取邮箱配置状态")
    public ResponseVO getEmailStatus() {
        try {
            Map<String, Object> status = new HashMap<>();

            String provider = systemConfigService.getConfigValue("email.provider", "custom");
            String sender = systemConfigService.getConfigValue("email.sender");
            String receiver = systemConfigService.getConfigValue("email.receiver");

            status.put("provider", provider);
            status.put("sender", sender);
            status.put("receiver", receiver);
            status.put("configured", sender != null && !sender.isEmpty());

            return ResponseVO.successResponse(status);
        } catch (Exception e) {
            log.error("获取邮箱配置状态失败", e);
            return ResponseVO.errorResponse("获取状态失败: " + e.getMessage());
        }
    }
}
