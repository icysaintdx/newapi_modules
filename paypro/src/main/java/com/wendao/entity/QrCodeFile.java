package com.wendao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 二维码文件管理实体
 */
@Data
@TableName("t_qrcode_file")
public class QrCodeFile {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 支付类型（alipay/wechat/alipay_dmf/wechat_zs）
     */
    private String payType;

    /**
     * 金额
     */
    private BigDecimal amount;

    /**
     * 二维码编号
     */
    private Integer qrNum;

    /**
     * 文件路径
     */
    private String filePath;

    /**
     * 原始文件名
     */
    private String fileName;

    /**
     * 是否启用
     */
    private Boolean enabled;

    private Date createTime;

    private Date updateTime;
}
