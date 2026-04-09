package com.wendao.common.task;

import com.wendao.common.RedisKeyConstant;
import com.wendao.config.PayProConfig;
import com.wendao.entity.Order;
import com.wendao.common.utils.EmailUtils;
import com.wendao.common.utils.StringUtils;
import com.wendao.controller.AlipayController;
import com.wendao.mapper.OrderMapper;
import com.wendao.enums.OrderStatesEnum;
import com.wendao.service.OrderService;
import com.alipay.api.AlipayApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author lld
 */
@Component
public class Jobs {

    final static Logger log= LoggerFactory.getLogger(Jobs.class);

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private EmailUtils emailUtils;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private AlipayController alipayController;

    @Autowired
    PayProConfig payProConfig;

    /**
     * 每日23:55检查是否漏单
     */
    @Scheduled(cron="0 55 23 * * ?")
    public void checkJob() {

        // 检查自动回调是否漏单
        List<Order> dmf = orderMapper.getByStateAndOrderType(0, "DMF");
        dmf.forEach(e -> {
            try {
                alipayController.queryOrderState(e.getId());
            } catch (AlipayApiException e1) {
                log.error(e1.getErrMsg());
            }
        });
    }

    /**
     * 每日凌晨清空除捐赠和审核中以外的数据
     */
    @Scheduled(cron="0 0 0 * * ?")
    @Transactional
    public void cronJob(){

        List<Order> list = orderMapper.getByStateIs(OrderStatesEnum.FAIL_PAY.getState());
        for(Order p : list){
            try {
                orderService.delOrder(p.getId());
            }catch (Exception e){
                log.error("定时删除数据"+p.getId()+"失败");
                e.printStackTrace();
            }
        }

        log.info("定时执行清空驳回和通过不展示的数据完毕");

        //设置未审核或已扫码数据为支付失败
        List<Order> list1 = orderMapper.getByStateIs(OrderStatesEnum.WAIT_PAY.getState());
        list1.addAll(orderMapper.getByStateIs(OrderStatesEnum.SCANE_QR.getState()));
        for(Order p : list1){
            p.setState(OrderStatesEnum.FAIL_PAY.getState());
            p.setUpdateTime(new Date());
            try {
                orderService.updateOrder(p);
            }catch (Exception e){
                log.error("设置未审核数据"+p.getId()+"为支付失败出错");
                e.printStackTrace();
            }
        }

        log.info("定时执行设置未审核数据为支付失败完毕");

        // 定时删除测试订单
        orderMapper.deleteByMoneyLessThanAndState(new BigDecimal("0.11"), 1);

        log.info("定时删除测试订单完毕");
    }

    /**
     * 每日10am统一发送支付失败邮件
     */
    @Scheduled(cron="0 0 10 * * ?")
    public void sendEmailJob(){

        List<Order> list = orderMapper.getByStateIs(OrderStatesEnum.FAIL_PAY.getState());
        for(Order p : list){
            p.setTime(StringUtils.getTimeStamp(p.getCreateTime()));
            if(StringUtils.isNotBlank(p.getEmail())&&EmailUtils.checkEmail(p.getEmail())) {
                emailUtils.sendTemplateMail(payProConfig.getEmail().getSender(), p.getEmail(), "【Order个人收款支付系统】支付失败通知", "pay-fail", p);
            }
        }

        log.info("定时执行统一发送支付失败邮件完毕");
    }

    /**
     * 每5分钟检查一次超时未支付订单，自动取消
     */
    @Scheduled(cron="0 */5 * * * ?")
    @Transactional
    public void cancelExpiredOrders(){
        if (payProConfig.getOrder() == null || payProConfig.getOrder().getTimeoutMinutes() == null) {
            return;
        }

        Long timeoutMinutes = payProConfig.getOrder().getTimeoutMinutes();
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, -timeoutMinutes.intValue());
        Date expireTime = calendar.getTime();

        List<Order> expiredOrders = orderMapper.getExpiredOrders(OrderStatesEnum.WAIT_PAY.getState(), expireTime);
        for(Order order : expiredOrders){
            try {
                order.setState(OrderStatesEnum.FAIL_PAY.getState());
                order.setUpdateTime(new Date());
                orderService.updateOrder(order);
                log.info("自动取消超时订单：{}", order.getId());
            }catch (Exception e){
                log.error("自动取消超时订单"+order.getId()+"失败", e);
            }
        }

        if (!expiredOrders.isEmpty()) {
            log.info("定时执行取消超时订单完毕，共取消{}个订单", expiredOrders.size());
        }
    }

}
