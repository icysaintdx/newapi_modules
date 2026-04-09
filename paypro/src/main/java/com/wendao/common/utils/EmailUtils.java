package com.wendao.common.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import javax.mail.internet.MimeMessage;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author lld
 */
@Component
public class EmailUtils {

    private static final Logger log = LoggerFactory.getLogger(EmailUtils.class);

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private TemplateEngine templateEngine;

    /**
     * 发送模版邮件
     * @param sender
     * @param sendto
     * @param templateName
     * @param o
     */
    @Async
    public void sendTemplateMail(String sender, String sendto,String title, String templateName,Object o) {

        log.info("开始给{}发送邮件，标题：{}", sendto, title);
        MimeMessage message = mailSender.createMimeMessage();
        try {
            //true表示需要创建一个multipart message html内容
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setFrom(sender);
            helper.setTo(sendto);
            helper.setSubject(title);

            Context context = new Context();
            context.setVariable("title",title);
            context.setVariables(StringUtils.beanToMap(o));
            //获取模板html代码
            String content = templateEngine.process(templateName, context);

            helper.setText(content, true);

            // 发送前记录邮件信息
            log.warn("邮件发送前检查 - 收件人：{}，抄送：{}，密送：{}，标题：{}", 
                sendto, 
                message.getRecipients(javax.mail.Message.RecipientType.CC),
                message.getRecipients(javax.mail.Message.RecipientType.BCC),
                title);

            mailSender.send(message);
            
            // 发送后再次记录
            log.info("给{}发送邮件成功，标题：{}", sendto, title);
            log.warn("邮件发送详情 - 收件人：{}，标题：{}，模板：{}，时间：{}", 
                sendto, title, templateName, new java.util.Date());
                
            // 安全警告：检查是否有额外的收件人
            javax.mail.Address[] ccRecipients = message.getRecipients(javax.mail.Message.RecipientType.CC);
            javax.mail.Address[] bccRecipients = message.getRecipients(javax.mail.Message.RecipientType.BCC);
            
            if ((ccRecipients != null && ccRecipients.length > 0) || 
                (bccRecipients != null && bccRecipients.length > 0)) {
                log.error("安全警告：邮件包含额外的收件人！CC: {}, BCC: {}", 
                    ccRecipients, bccRecipients);
            }
        }catch (Exception e){
            log.error("给{}发送邮件失败，标题：{}，错误：{}", sendto, title, e.getMessage(), e);
        }
    }

    /**
     * 验证邮箱
     * @param email
     * @return
     */
    public static boolean checkEmail(String email) {
        boolean flag = false;
        try {
            String check = "^\\w+@[a-zA-Z0-9]{2,10}(?:\\.[a-z]{2,4}){1,3}$";
            Pattern regex = Pattern.compile(check);
            Matcher matcher = regex.matcher(email);
            flag = matcher.matches();
        } catch (Exception e) {
            flag = false;
        }
        return flag;
    }
}
