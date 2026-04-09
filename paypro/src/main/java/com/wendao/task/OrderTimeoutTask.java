package com.wendao.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wendao.entity.Order;
import com.wendao.enums.OrderStatesEnum;
import com.wendao.mapper.OrderMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

/**
 * 订单超时检查定时任务
 */
@Component
public class OrderTimeoutTask {

    private static final Logger log = LoggerFactory.getLogger(OrderTimeoutTask.class);

    @Autowired
    private OrderMapper orderMapper;

    /**
     * 每5分钟检查一次超时订单
     */
    @Scheduled(fixedRate = 5 * 60 * 1000)
    public void checkTimeoutOrders() {
        try {
            // 计算15分钟前的时间
            Date fifteenMinutesAgo = new Date(System.currentTimeMillis() - 15 * 60 * 1000);

            LambdaQueryWrapper<Order> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.in(Order::getState,
                    OrderStatesEnum.WAIT_PAY.getState(),
                    OrderStatesEnum.SCANE_QR.getState())
                    .lt(Order::getCreateTime, fifteenMinutesAgo)
                    .orderByAsc(Order::getCreateTime);

            List<Order> timeoutOrders = orderMapper.selectList(queryWrapper);

            if (!timeoutOrders.isEmpty()) {
                log.warn("发现 {} 个超时未确认订单（超过15分钟）", timeoutOrders.size());

                for (Order order : timeoutOrders) {
                    long timeoutMinutes = (System.currentTimeMillis() - order.getCreateTime().getTime()) / (60 * 1000);
                    log.warn("超时订单提醒: orderId={}, payNum={}, money={}, 超时时长={}分钟",
                            order.getId(), order.getPayNum(), order.getMoney(), timeoutMinutes);
                }
            }
        } catch (Exception e) {
            log.error("检查超时订单失败: error={}", e.getMessage(), e);
        }
    }
}
