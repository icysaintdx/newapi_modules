package com.wendao.service;

import com.wendao.dto.SystemConfigDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 配置管理服务接口
 */
public interface ConfigService {
    
    /**
     * 获取系统配置
     */
    SystemConfigDTO getSystemConfig();
    
    /**
     * 更新系统配置
     */
    void updateSystemConfig(SystemConfigDTO config);
    
    /**
     * 上传收款二维码
     */
    String uploadQrCode(MultipartFile file, String payType) throws Exception;
    
    /**
     * 获取支付方式配置
     */
    Map<String, Object> getPaymentMethods();
    
    /**
     * 更新支付方式配置
     */
    void updatePaymentMethods(Map<String, Object> config);
}
