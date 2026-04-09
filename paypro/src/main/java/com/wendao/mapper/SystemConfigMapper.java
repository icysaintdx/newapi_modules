package com.wendao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wendao.entity.SystemConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 系统配置Mapper
 */
@Mapper
public interface SystemConfigMapper extends BaseMapper<SystemConfig> {
    
    /**
     * 根据配置键获取配置值
     */
    @Select("SELECT config_value FROM t_system_config WHERE config_key = #{configKey}")
    String getValueByKey(@Param("configKey") String configKey);
}
