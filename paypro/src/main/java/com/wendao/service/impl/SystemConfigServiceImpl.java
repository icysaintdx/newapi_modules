package com.wendao.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wendao.entity.SystemConfig;
import com.wendao.mapper.SystemConfigMapper;
import com.wendao.service.SystemConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 系统配置服务实现
 */
@Service
public class SystemConfigServiceImpl implements SystemConfigService {
    
    @Autowired
    private SystemConfigMapper systemConfigMapper;
    
    @Override
    @Cacheable(value = "systemConfig", key = "'allByGroup'")
    public Map<String, List<SystemConfig>> getAllConfigsByGroup() {
        List<SystemConfig> allConfigs = systemConfigMapper.selectList(null);
        return allConfigs.stream()
                .collect(Collectors.groupingBy(SystemConfig::getConfigGroup));
    }
    
    @Override
    @Cacheable(value = "systemConfig", key = "#configKey")
    public String getConfigValue(String configKey) {
        return systemConfigMapper.getValueByKey(configKey);
    }
    
    @Override
    public String getConfigValue(String configKey, String defaultValue) {
        String value = getConfigValue(configKey);
        return value != null ? value : defaultValue;
    }
    
    @Override
    @Transactional
    @CacheEvict(value = "systemConfig", allEntries = true)
    public void saveConfig(String configKey, String configValue) {
        LambdaQueryWrapper<SystemConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemConfig::getConfigKey, configKey);
        SystemConfig config = systemConfigMapper.selectOne(wrapper);
        
        if (config != null) {
            config.setConfigValue(configValue);
            systemConfigMapper.updateById(config);
        } else {
            config = new SystemConfig();
            config.setConfigKey(configKey);
            config.setConfigValue(configValue);
            config.setConfigType("STRING");
            config.setConfigGroup("BASIC");
            systemConfigMapper.insert(config);
        }
    }
    
    @Override
    @Transactional
    @CacheEvict(value = "systemConfig", allEntries = true)
    public void batchSaveConfigs(Map<String, String> configs) {
        for (Map.Entry<String, String> entry : configs.entrySet()) {
            saveConfig(entry.getKey(), entry.getValue());
        }
    }
    
    @Override
    public List<SystemConfig> getConfigsByGroup(String group) {
        LambdaQueryWrapper<SystemConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemConfig::getConfigGroup, group);
        return systemConfigMapper.selectList(wrapper);
    }
}
