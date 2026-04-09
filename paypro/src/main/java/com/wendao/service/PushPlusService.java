package com.wendao.service;

import com.wendao.entity.Order;

/**
 * PushPlus推送服务接口
 */
public interface PushPlusService {

    /**
     * 发送订单审核推送
     * @param order 订单信息
     * @param token 订单Token
     * @param myToken 二次验证Token
     * @return 是否成功
     */
    boolean sendOrderReviewPush(Order order, String token, String myToken);
}
