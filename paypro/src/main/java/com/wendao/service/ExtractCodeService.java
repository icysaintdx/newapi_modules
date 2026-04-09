package com.wendao.service;

import com.wendao.entity.ExtractCode;

/**
 * 提取码服务接口
 */
public interface ExtractCodeService {

    /**
     * 为订单生成提取码
     *
     * @param orderId 订单ID
     * @param redemptionCodeId 兑换码ID
     * @param redemptionCode 兑换码内容
     * @return 生成的提取码
     */
    String generateExtractCode(String orderId, Long redemptionCodeId, String redemptionCode);

    /**
     * 验证并使用提取码
     *
     * @param code 提取码
     * @return 兑换码内容，如果提取码无效返回null
     */
    String useExtractCode(String code);

    /**
     * 查询提取码信息（不标记为已使用）
     *
     * @param code 提取码
     * @return 提取码信息
     */
    ExtractCode getExtractCodeInfo(String code);
}
