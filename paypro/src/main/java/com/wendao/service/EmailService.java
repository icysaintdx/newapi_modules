package com.wendao.service;

import java.util.Map;

/**
 * 邮件发送服务接口
 * 支持多种邮件服务商
 */
public interface EmailService {

    /**
     * 发送测试邮件
     * @param toEmail 收件人邮箱
     * @return 发送结果消息
     * @throws Exception 发送失败时抛出异常
     */
    String sendTestEmail(String toEmail) throws Exception;

    /**
     * 发送模板邮件
     * @param toEmail 收件人邮箱
     * @param subject 邮件主题
     * @param templateName 模板名称
     * @param variables 模板变量
     * @throws Exception 发送失败时抛出异常
     */
    void sendTemplateEmail(String toEmail, String subject, String templateName, Map<String, Object> variables) throws Exception;

    /**
     * 获取当前配置的邮件服务商类型
     * @return 服务商类型
     */
    String getProviderType();

    /**
     * 验证邮件配置是否完整
     * @return 配置是否完整
     */
    boolean isConfigured();
}
