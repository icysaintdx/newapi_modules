package com.wendao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 系统配置实体类
 */
@Data
@TableName("t_system_config")
public class SystemConfig implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 配置键
     */
    private String configKey;
    
    /**
     * 配置值
     */
    private String configValue;
    
    /**
     * 配置类型：STRING, NUMBER, BOOLEAN, JSON
     */
    private String configType;
    
    /**
     * 配置分组：BASIC, EMAIL, ALIPAY, PAYMENT, PAGE
     */
    private String configGroup;
    
    /**
     * 配置描述
     */
    private String description;
    
    /**
     * 是否敏感信息
     */
    private Boolean isSensitive;
    
    /**
     * 创建时间
     */
    private Date createTime;
    
    /**
     * 更新时间
     */
    private Date updateTime;
}
