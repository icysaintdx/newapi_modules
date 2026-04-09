package com.wendao.service.impl;

import com.wendao.dto.SystemConfigDTO;
import com.wendao.service.ConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

/**
 * 配置管理服务实现
 */
@Service
public class ConfigServiceImpl implements ConfigService {

    private static final Logger log = LoggerFactory.getLogger(ConfigServiceImpl.class);

    private static final String CONFIG_FILE = "/app/config/application.yml";
    private static final String QR_CODE_BASE_PATH = "/app/static/assets/qr/";

    @Value("${paypro.site:}")
    private String site;

    @Value("${paypro.indexTitle:}")
    private String indexTitle;

    @Value("${paypro.title:}")
    private String title;

    @Value("${paypro.name:}")
    private String name;

    @Value("${paypro.email.receiver:}")
    private String emailReceiver;

    @Value("${paypro.email.sender:}")
    private String emailSender;

    @Value("${spring.mail.host:}")
    private String mailHost;

    @Value("${spring.mail.port:465}")
    private Integer mailPort;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${paypro.alipayCustomQrUrl:}")
    private String alipayCustomQrUrl;

    @Value("${paypro.alipayUserId:}")
    private String alipayUserId;

    @Value("${paypro.qrCodeNum:2}")
    private Integer qrCodeNum;

    @Value("${paypro.downloadUrl:}")
    private String downloadUrl;

    @Value("${paypro.supportMail:}")
    private String supportMail;

    @Value("${paypro.openapi.secret:}")
    private String openapiSecret;

    @Value("${paypro.rateLimit.ipExpire:2}")
    private Integer rateLimitIpExpire;

    @Value("${paypro.token.value:}")
    private String tokenValue;

    @Value("${paypro.token.expire:14}")
    private Integer tokenExpire;

    @Override
    public SystemConfigDTO getSystemConfig() {
        SystemConfigDTO config = new SystemConfigDTO();
        config.setSite(site);
        config.setIndexTitle(indexTitle);
        config.setTitle(title);
        config.setName(name);
        config.setEmailReceiver(emailReceiver);
        config.setEmailSender(emailSender);
        config.setMailHost(mailHost);
        config.setMailPort(mailPort);
        config.setMailUsername(mailUsername);
        config.setAlipayCustomQrUrl(alipayCustomQrUrl);
        config.setAlipayUserId(alipayUserId);
        config.setQrCodeNum(qrCodeNum);
        config.setDownloadUrl(downloadUrl);
        config.setSupportMail(supportMail);
        config.setOpenapiSecret(openapiSecret);
        config.setRateLimitIpExpire(rateLimitIpExpire);
        config.setTokenValue(tokenValue);
        config.setTokenExpire(tokenExpire);
        return config;
    }

    @Override
    public void updateSystemConfig(SystemConfigDTO config) {
        try {
            // 读取现有配置文件
            File configFile = new File(CONFIG_FILE);
            if (!configFile.exists()) {
                configFile = new File("src/main/resources/application.yml");
            }

            Yaml yaml = new Yaml();
            Map<String, Object> data;
            
            try (FileInputStream fis = new FileInputStream(configFile)) {
                data = yaml.load(fis);
            }

            // 更新配置
            Map<String, Object> paypro = (Map<String, Object>) data.getOrDefault("paypro", new HashMap<>());
            
            if (config.getSite() != null) paypro.put("site", config.getSite());
            if (config.getIndexTitle() != null) paypro.put("indexTitle", config.getIndexTitle());
            if (config.getTitle() != null) paypro.put("title", config.getTitle());
            if (config.getName() != null) paypro.put("name", config.getName());
            if (config.getQrCodeNum() != null) paypro.put("qrCodeNum", config.getQrCodeNum());
            if (config.getDownloadUrl() != null) paypro.put("downloadUrl", config.getDownloadUrl());
            if (config.getSupportMail() != null) paypro.put("supportMail", config.getSupportMail());
            
            // 邮件配置
            Map<String, Object> email = (Map<String, Object>) paypro.getOrDefault("email", new HashMap<>());
            if (config.getEmailReceiver() != null) email.put("receiver", config.getEmailReceiver());
            if (config.getEmailSender() != null) email.put("sender", config.getEmailSender());
            paypro.put("email", email);
            
            // OpenAPI配置
            Map<String, Object> openapi = (Map<String, Object>) paypro.getOrDefault("openapi", new HashMap<>());
            if (config.getOpenapiSecret() != null) openapi.put("secret", config.getOpenapiSecret());
            paypro.put("openapi", openapi);
            
            // 限流配置
            Map<String, Object> rateLimit = (Map<String, Object>) paypro.getOrDefault("rateLimit", new HashMap<>());
            if (config.getRateLimitIpExpire() != null) rateLimit.put("ipExpire", config.getRateLimitIpExpire());
            paypro.put("rateLimit", rateLimit);
            
            // Token配置
            Map<String, Object> token = (Map<String, Object>) paypro.getOrDefault("token", new HashMap<>());
            if (config.getTokenValue() != null) token.put("value", config.getTokenValue());
            if (config.getTokenExpire() != null) token.put("expire", config.getTokenExpire());
            paypro.put("token", token);
            
            // 支付宝配置
            if (config.getAlipayCustomQrUrl() != null) paypro.put("alipayCustomQrUrl", config.getAlipayCustomQrUrl());
            if (config.getAlipayUserId() != null) paypro.put("alipayUserId", config.getAlipayUserId());
            if (config.getAlipayDmfAppId() != null) paypro.put("alipayDmfAppId", config.getAlipayDmfAppId());
            if (config.getAlipayDmfAppPrivateKey() != null) paypro.put("alipayDmfAppPrivateKey", config.getAlipayDmfAppPrivateKey());
            if (config.getAlipayDmfPublicKey() != null) paypro.put("alipayDmfPublicKey", config.getAlipayDmfPublicKey());
            if (config.getAlipayDmfSubject() != null) paypro.put("alipayDmfSubject", config.getAlipayDmfSubject());
            
            data.put("paypro", paypro);
            
            // 更新邮件配置
            Map<String, Object> spring = (Map<String, Object>) data.getOrDefault("spring", new HashMap<>());
            Map<String, Object> mail = (Map<String, Object>) spring.getOrDefault("mail", new HashMap<>());
            if (config.getMailHost() != null) mail.put("host", config.getMailHost());
            if (config.getMailPort() != null) mail.put("port", config.getMailPort());
            if (config.getMailUsername() != null) mail.put("username", config.getMailUsername());
            if (config.getMailPassword() != null && !config.getMailPassword().isEmpty()) {
                mail.put("password", config.getMailPassword());
            }
            spring.put("mail", mail);
            data.put("spring", spring);

            // 写回配置文件
            DumperOptions options = new DumperOptions();
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            options.setPrettyFlow(true);
            Yaml yamlWriter = new Yaml(options);
            
            try (FileWriter writer = new FileWriter(configFile)) {
                yamlWriter.dump(data, writer);
            }
            
            log.info("系统配置已更新");
        } catch (Exception e) {
            log.error("更新系统配置失败", e);
            throw new RuntimeException("更新配置失败: " + e.getMessage());
        }
    }

    @Override
    public String uploadQrCode(MultipartFile file, String payType) throws Exception {
        try {
            // 确定目标路径
            String targetDir = QR_CODE_BASE_PATH + payType + "/";
            Path dirPath = Paths.get(targetDir);
            
            // 如果目录不存在，尝试使用项目路径
            if (!Files.exists(dirPath)) {
                targetDir = "src/main/resources/static/assets/qr/" + payType + "/";
                dirPath = Paths.get(targetDir);
            }
            
            // 创建目录（如果不存在）
            Files.createDirectories(dirPath);
            
            // 保存文件为 custom.png
            Path targetPath = dirPath.resolve("custom.png");
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            
            log.info("二维码上传成功: {}", targetPath);
            return targetPath.toString();
        } catch (Exception e) {
            log.error("上传二维码失败", e);
            throw new Exception("上传失败: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> getPaymentMethods() {
        try {
            File configFile = new File(CONFIG_FILE);
            if (!configFile.exists()) {
                configFile = new File("src/main/resources/application.yml");
            }

            Yaml yaml = new Yaml();
            Map<String, Object> data;
            
            try (FileInputStream fis = new FileInputStream(configFile)) {
                data = yaml.load(fis);
            }

            Map<String, Object> paypro = (Map<String, Object>) data.get("paypro");
            if (paypro != null) {
                return (Map<String, Object>) paypro.get("payMethods");
            }
            return new HashMap<>();
        } catch (Exception e) {
            log.error("获取支付方式配置失败", e);
            return new HashMap<>();
        }
    }

    @Override
    public void updatePaymentMethods(Map<String, Object> config) {
        try {
            File configFile = new File(CONFIG_FILE);
            if (!configFile.exists()) {
                configFile = new File("src/main/resources/application.yml");
            }

            Yaml yaml = new Yaml();
            Map<String, Object> data;
            
            try (FileInputStream fis = new FileInputStream(configFile)) {
                data = yaml.load(fis);
            }

            Map<String, Object> paypro = (Map<String, Object>) data.get("paypro");
            if (paypro != null) {
                paypro.put("payMethods", config.get("payMethods"));
            }

            // 写回配置文件
            DumperOptions options = new DumperOptions();
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            options.setPrettyFlow(true);
            Yaml yamlWriter = new Yaml(options);
            
            try (FileWriter writer = new FileWriter(configFile)) {
                yamlWriter.dump(data, writer);
            }
            
            log.info("支付方式配置已更新");
        } catch (Exception e) {
            log.error("更新支付方式配置失败", e);
            throw new RuntimeException("更新配置失败: " + e.getMessage());
        }
    }
}
