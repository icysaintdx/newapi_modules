package com.wendao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 兑换码实体
 */
@Data
@TableName("t_redemption_code")
public class RedemptionCode implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 兑换码
     */
    private String code;

    /**
     * 关联商品ID
     */
    private Integer productId;

    /**
     * 状态：0-未使用，1-已使用，2-已锁定
     */
    private Integer status;

    /**
     * 关联订单ID（已使用时）
     */
    private String orderId;

    /**
     * 使用时间
     */
    private Date usedTime;

    /**
     * 锁定时间（防止并发）
     */
    private Date lockTime;

    /**
     * 锁定令牌（UUID）
     */
    private String lockToken;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 批次ID（批量导入时的批次标识）
     */
    private String batchId;
}
