package com.wendao.service;

import com.wendao.entity.SystemConfig;
import java.util.List;
import java.util.Map;

/**
 * 系统配置服务接口
 */
public interface SystemConfigService {
    
    /**
     * 获取所有配置（按分组）
     */
    Map<String, List<SystemConfig>> getAllConfigsByGroup();
    
    /**
     * 根据配置键获取配置值
     */
    String getConfigValue(String configKey);
    
    /**
     * 根据配置键获取配置值，如果不存在返回默认值
     */
    String getConfigValue(String configKey, String defaultValue);
    
    /**
     * 保存或更新配置
     */
    void saveConfig(String configKey, String configValue);
    
    /**
     * 批量保存配置
     */
    void batchSaveConfigs(Map<String, String> configs);
    
    /**
     * 根据分组获取配置
     */
    List<SystemConfig> getConfigsByGroup(String group);
}
