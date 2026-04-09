package com.wendao.job;

import cn.hutool.log.StaticLog;
import com.wendao.entity.Order;
import com.wendao.enums.OrderStatesEnum;
import com.wendao.mapper.OrderMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

/**
 * 订单超时检查定时任务
 */
@Component
public class OrderTimeoutJob {

    @Autowired
    private OrderMapper orderMapper;

    /**
     * 每5分钟检查一次过期订单
     */
    @Scheduled(fixedRate = 300000)
    public void checkExpiredOrders() {
        try {
            Date now = new Date();

            // 查询待支付且已过期的订单
            QueryWrapper<Order> queryWrapper = new QueryWrapper<>();
            queryWrapper.lambda()
                    .eq(Order::getState, OrderStatesEnum.WAIT_PAY.getState())
                    .isNotNull(Order::getExpireTime)
                    .lt(Order::getExpireTime, now);

            List<Order> expiredOrders = orderMapper.selectList(queryWrapper);

            if (expiredOrders.isEmpty()) {
                return;
            }

            StaticLog.info("发现{}个过期订单，开始处理", expiredOrders.size());

            // 批量更新为过期状态
            for (Order order : expiredOrders) {
                order.setState(OrderStatesEnum.EXPIRED.getState());
                order.setUpdateTime(now);
                orderMapper.updateById(order);
                StaticLog.info("订单已过期: orderId={}, expireTime={}",
                        order.getId(), order.getExpireTime());
            }

        } catch (Exception e) {
            StaticLog.error("检查过期订单异常", e);
        }
    }
}
