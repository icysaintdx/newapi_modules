package com.wendao.service;

import com.wendao.entity.Order;

/**
 * 多渠道通知服务接口
 */
public interface MultiChannelNotificationService {

    /**
     * 发送订单通知（自动选择最优渠道）
     * @param order 订单信息
     * @param token 订单Token
     * @param myToken 二次验证Token
     * @return 是否成功
     */
    boolean sendOrderNotification(Order order, String token, String myToken);
}
