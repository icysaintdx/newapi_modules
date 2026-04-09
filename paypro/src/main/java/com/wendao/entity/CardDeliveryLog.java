package com.wendao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 发卡日志实体（用于审计和防止重复发卡）
 */
@Data
@TableName("t_card_delivery_log")
public class CardDeliveryLog implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 订单ID
     */
    private String orderId;

    /**
     * 兑换码ID
     */
    private Long redemptionCodeId;

    /**
     * 兑换码内容
     */
    private String code;

    /**
     * 收件人邮箱
     */
    private String email;

    /**
     * 发送状态：0-失败，1-成功
     */
    private Integer status;

    /**
     * 错误信息
     */
    private String errorMsg;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 幂等键（订单ID+时间戳哈希）
     */
    private String idempotentKey;
}
