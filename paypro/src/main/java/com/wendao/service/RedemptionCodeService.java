package com.wendao.service;

import com.wendao.entity.RedemptionCode;

/**
 * 兑换码服务接口
 */
public interface RedemptionCodeService {

    /**
     * 为订单分配兑换码（安全、幂等）
     *
     * @param orderId 订单ID
     * @param productId 商品ID
     * @param email 收件人邮箱
     * @return 分配的兑换码，如果失败返回null
     */
    RedemptionCode allocateCodeForOrder(String orderId, Integer productId, String email);

    /**
     * 批量导入兑换码
     *
     * @param productId 商品ID
     * @param codes 兑换码列表
     * @param batchId 批次ID
     * @return 成功导入的数量
     */
    int batchImportCodes(Integer productId, java.util.List<String> codes, String batchId);

    /**
     * 获取商品可用库存
     *
     * @param productId 商品ID
     * @return 可用库存数量
     */
    int getAvailableStock(Integer productId);

    /**
     * 释放超时锁定的兑换码（定时任务调用）
     *
     * @param timeoutMinutes 超时分钟数
     * @return 释放的数量
     */
    int releaseTimeoutLocks(int timeoutMinutes);
}
