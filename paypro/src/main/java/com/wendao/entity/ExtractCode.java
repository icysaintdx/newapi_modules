package com.wendao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 提取码实体
 */
@Data
@TableName("t_extract_code")
public class ExtractCode implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 提取码（6位数字）
     */
    private String code;

    /**
     * 关联订单ID
     */
    private String orderId;

    /**
     * 关联兑换码ID
     */
    private Long redemptionCodeId;

    /**
     * 兑换码内容（冗余存储，方便快速查询）
     */
    private String redemptionCode;

    /**
     * 状态：0-未使用，1-已使用
     */
    private Integer status;

    /**
     * 使用时间
     */
    private Date usedTime;

    /**
     * 过期时间（24小时）
     */
    private Date expireTime;

    /**
     * 创建时间
     */
    private Date createTime;
}
