package com.wendao.dto;

import lombok.Data;

/**
 * 系统配置 DTO
 */
@Data
public class SystemConfigDTO {
    
    // 基本配置
    private String site;
    private String indexTitle;
    private String title;
    private String name;
    
    // 邮件配置
    private String emailReceiver;
    private String emailSender;
    private String mailHost;
    private Integer mailPort;
    private String mailUsername;
    private String mailPassword;
    
    // 支付宝配置
    private String alipayCustomQrUrl;
    private String alipayUserId;
    private String alipayDmfAppId;
    private String alipayDmfAppPrivateKey;
    private String alipayDmfPublicKey;
    private String alipayDmfSubject;
    
    // 其他配置
    private Integer qrCodeNum;
    private String downloadUrl;
    private String supportMail;
    private String openapiSecret;
    
    // 限流配置
    private Integer rateLimitIpExpire;
    
    // Token配置
    private String tokenValue;
    private Integer tokenExpire;
}
