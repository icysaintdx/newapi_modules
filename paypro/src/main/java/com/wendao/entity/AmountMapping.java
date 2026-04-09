package com.wendao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 金额映射实体
 */
@Data
@TableName("t_amount_mapping")
public class AmountMapping {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 支付类型（alipay/wechat/alipay_dmf/wechat_zs）
     */
    private String payType;

    /**
     * 充值金额（用户要充值的金额）
     */
    private BigDecimal chargeAmount;

    /**
     * 实际收款金额（实际使用的收款码金额）
     */
    private BigDecimal actualAmount;

    /**
     * 优先级（数字越小优先级越高）
     */
    private Integer priority;

    /**
     * 是否启用
     */
    private Boolean enabled;

    private Date createTime;

    private Date updateTime;
}
