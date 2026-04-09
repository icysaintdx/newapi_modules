package com.wendao.service.impl;

import com.wendao.service.EmailService;
import com.wendao.service.SystemConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import javax.mail.internet.MimeMessage;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * 邮件发送服务实现
 * 支持多种邮件服务商的动态配置
 */
@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);

    @Autowired
    private SystemConfigService systemConfigService;

    @Autowired
    private TemplateEngine templateEngine;

    @Override
    public String sendTestEmail(String toEmail) throws Exception {
        String provider = getProviderType();
        String sender = systemConfigService.getConfigValue("email.sender");

        if (sender == null || sender.trim().isEmpty()) {
            throw new Exception("未配置邮件发送者，请先在邮箱配置中设置 email.sender");
        }

        // 创建测试数据
        Map<String, Object> testData = new HashMap<>();
        testData.put("content", "这是一封来自 PayPro 系统的测试邮件。如果您收到此邮件，说明邮件配置成功！");
        testData.put("time", new java.util.Date().toString());

        // 发送邮件
        sendTemplateEmail(toEmail, "PayPro 邮件测试", "email-test", testData);

        return "测试邮件已发送到 " + toEmail;
    }

    @Override
    public void sendTemplateEmail(String toEmail, String subject, String templateName, Map<String, Object> variables) throws Exception {
        log.info("开始发送邮件到: {}, 主题: {}", toEmail, subject);

        // 创建动态配置的 JavaMailSender
        JavaMailSenderImpl mailSender = createMailSender();

        // 创建邮件消息
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        String sender = systemConfigService.getConfigValue("email.sender");
        helper.setFrom(sender);
        helper.setTo(toEmail);
        helper.setSubject(subject);

        // 渲染模板
        Context context = new Context();
        context.setVariable("title", subject);
        if (variables != null) {
            variables.forEach(context::setVariable);
        }
        String content = templateEngine.process(templateName, context);
        helper.setText(content, true);

        // 发送邮件
        mailSender.send(message);
        log.info("邮件发送成功到: {}", toEmail);
    }

    @Override
    public String getProviderType() {
        return systemConfigService.getConfigValue("email.provider", "custom");
    }

    @Override
    public boolean isConfigured() {
        String sender = systemConfigService.getConfigValue("email.sender");
        String provider = getProviderType();

        if (sender == null || sender.trim().isEmpty()) {
            return false;
        }

        // 根据不同的服务商检查必要的配置
        switch (provider) {
            case "resend":
                return systemConfigService.getConfigValue("email.resend.api_key") != null;
            case "brevo":
                return systemConfigService.getConfigValue("email.brevo.smtp_user") != null
                        && systemConfigService.getConfigValue("email.brevo.smtp_key") != null;
            case "smtp2go":
                return systemConfigService.getConfigValue("email.smtp2go.smtp_user") != null
                        && systemConfigService.getConfigValue("email.smtp2go.smtp_pass") != null;
            case "mailersend":
                return systemConfigService.getConfigValue("email.mailersend.smtp_user") != null
                        && systemConfigService.getConfigValue("email.mailersend.smtp_pass") != null;
            case "qq":
            case "163":
            case "gmail":
            case "outlook":
            case "custom":
                return systemConfigService.getConfigValue("email.username") != null
                        && systemConfigService.getConfigValue("email.password") != null;
            default:
                return false;
        }
    }

    /**
     * 根据配置创建 JavaMailSender
     */
    private JavaMailSenderImpl createMailSender() throws Exception {
        String provider = getProviderType();
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();

        log.info("创建邮件发送器，服务商: {}", provider);

        switch (provider) {
            case "resend":
                configureResend(mailSender);
                break;
            case "brevo":
                configureBrevo(mailSender);
                break;
            case "smtp2go":
                configureSmtp2Go(mailSender);
                break;
            case "mailersend":
                configureMailerSend(mailSender);
                break;
            case "qq":
                configureQQ(mailSender);
                break;
            case "163":
                configure163(mailSender);
                break;
            case "gmail":
                configureGmail(mailSender);
                break;
            case "outlook":
                configureOutlook(mailSender);
                break;
            case "custom":
            default:
                configureCustom(mailSender);
                break;
        }

        return mailSender;
    }

    private void configureResend(JavaMailSenderImpl mailSender) {
        // Resend SMTP 配置（官方文档：https://resend.com/docs/send-with-smtp）
        // 服务器: smtp.resend.com
        // 用户名: resend（固定值）
        // 密码: 你的 Resend API Key
        // 端口: 587 (STARTTLS) 或 465 (SMTPS)

        String host = systemConfigService.getConfigValue("email.resend.smtp_host", "smtp.resend.com");
        int port = Integer.parseInt(systemConfigService.getConfigValue("email.resend.smtp_port", "587"));

        mailSender.setHost(host);
        mailSender.setPort(port);
        mailSender.setUsername("resend"); // Resend 固定使用 "resend" 作为用户名
        mailSender.setPassword(systemConfigService.getConfigValue("email.resend.api_key"));

        Properties props = mailSender.getJavaMailProperties();

        if (port == 465 || port == 2465) {
            // SMTPS - 隐式 SSL/TLS
            props.put("mail.transport.protocol", "smtps");
            props.put("mail.smtp.ssl.enable", "true");
        } else {
            // STARTTLS - 显式 SSL/TLS
            props.put("mail.transport.protocol", "smtp");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.starttls.required", "true");
        }

        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.connectiontimeout", "5000");
        props.put("mail.smtp.timeout", "5000");
        props.put("mail.smtp.writetimeout", "5000");

        log.info("Resend SMTP 配置: host={}, port={}, user=resend", host, port);
    }

    private void configureBrevo(JavaMailSenderImpl mailSender) {
        // Brevo SMTP 配置
        // 服务器: smtp-relay.brevo.com
        // 端口: 587 (STARTTLS)
        // 用户名: 你的 Brevo SMTP 登录邮箱 (格式: xxxxx@smtp-brevo.com)
        // 密码: 你的 Brevo SMTP Key (格式: xsmtpsib-...)

        String host = systemConfigService.getConfigValue("email.brevo.smtp_host", "smtp-relay.brevo.com");
        int port = Integer.parseInt(systemConfigService.getConfigValue("email.brevo.smtp_port", "587"));

        mailSender.setHost(host);
        mailSender.setPort(port);
        mailSender.setUsername(systemConfigService.getConfigValue("email.brevo.smtp_user"));
        mailSender.setPassword(systemConfigService.getConfigValue("email.brevo.smtp_key"));

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "true");
        props.put("mail.smtp.connectiontimeout", "5000");
        props.put("mail.smtp.timeout", "5000");
        props.put("mail.smtp.writetimeout", "5000");

        log.info("Brevo SMTP 配置: host={}, port={}, user={}", host, port, systemConfigService.getConfigValue("email.brevo.smtp_user"));
    }

    private void configureSmtp2Go(JavaMailSenderImpl mailSender) {
        mailSender.setHost("mail.smtp2go.com");
        mailSender.setPort(587);
        mailSender.setUsername(systemConfigService.getConfigValue("email.smtp2go.smtp_user"));
        mailSender.setPassword(systemConfigService.getConfigValue("email.smtp2go.smtp_pass"));

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
    }

    private void configureMailerSend(JavaMailSenderImpl mailSender) {
        mailSender.setHost("smtp.mailersend.net");
        mailSender.setPort(587);
        mailSender.setUsername(systemConfigService.getConfigValue("email.mailersend.smtp_user"));
        mailSender.setPassword(systemConfigService.getConfigValue("email.mailersend.smtp_pass"));

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
    }

    private void configureQQ(JavaMailSenderImpl mailSender) {
        mailSender.setHost("smtp.qq.com");
        mailSender.setPort(465);
        mailSender.setUsername(systemConfigService.getConfigValue("email.username"));
        mailSender.setPassword(systemConfigService.getConfigValue("email.password"));

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtps");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.ssl.enable", "true");
    }

    private void configure163(JavaMailSenderImpl mailSender) {
        mailSender.setHost("smtp.163.com");
        mailSender.setPort(465);
        mailSender.setUsername(systemConfigService.getConfigValue("email.username"));
        mailSender.setPassword(systemConfigService.getConfigValue("email.password"));

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtps");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.ssl.enable", "true");
    }

    private void configureGmail(JavaMailSenderImpl mailSender) {
        mailSender.setHost("smtp.gmail.com");
        mailSender.setPort(587);
        mailSender.setUsername(systemConfigService.getConfigValue("email.username"));
        mailSender.setPassword(systemConfigService.getConfigValue("email.password"));

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "true");
    }

    private void configureOutlook(JavaMailSenderImpl mailSender) {
        mailSender.setHost("smtp-mail.outlook.com");
        mailSender.setPort(587);
        mailSender.setUsername(systemConfigService.getConfigValue("email.username"));
        mailSender.setPassword(systemConfigService.getConfigValue("email.password"));

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
    }

    private void configureCustom(JavaMailSenderImpl mailSender) {
        String host = systemConfigService.getConfigValue("email.smtp_host", "smtp.qq.com");
        int port = Integer.parseInt(systemConfigService.getConfigValue("email.smtp_port", "465"));
        String protocol = systemConfigService.getConfigValue("email.protocol", "smtps");

        mailSender.setHost(host);
        mailSender.setPort(port);
        mailSender.setUsername(systemConfigService.getConfigValue("email.username"));
        mailSender.setPassword(systemConfigService.getConfigValue("email.password"));

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", protocol);
        props.put("mail.smtp.auth", "true");

        if ("smtps".equals(protocol) || port == 465) {
            props.put("mail.smtp.ssl.enable", "true");
        } else {
            props.put("mail.smtp.starttls.enable", "true");
        }
    }
}
