package com.wendao.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wendao.common.utils.StringUtils;
import com.wendao.entity.Order;
import com.wendao.enums.OrderStatesEnum;
import com.wendao.mapper.OrderMapper;
import com.wendao.model.ResponseVO;
import com.wendao.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/api/order")
public class AdminOrderController {

    private static final Logger log = LoggerFactory.getLogger(AdminOrderController.class);

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderService orderService;

    @GetMapping("/list")
    public ResponseVO list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String orderId,
            @RequestParam(required = false) String payNum,
            @RequestParam(required = false) Integer state,
            @RequestParam(required = false) String payType) {
        
        Page<Order> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Order> queryWrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.isNotBlank(orderId)) {
            queryWrapper.like(Order::getId, orderId);
        }
        if (StringUtils.isNotBlank(payNum)) {
            queryWrapper.like(Order::getPayNum, payNum);
        }
        if (state != null) {
            queryWrapper.eq(Order::getState, state);
        }
        if (StringUtils.isNotBlank(payType)) {
            queryWrapper.eq(Order::getPayType, payType);
        }
        
        queryWrapper.orderByDesc(Order::getCreateTime);
        
        IPage<Order> result = orderMapper.selectPage(pageParam, queryWrapper);
        
        Map<String, Object> data = new HashMap<>();
        data.put("list", result.getRecords());
        data.put("total", result.getTotal());
        data.put("page", page);
        data.put("size", size);
        
        return ResponseVO.successResponse(data);
    }

    @GetMapping("/detail/{id}")
    public ResponseVO detail(@PathVariable String id) {
        Order order = orderMapper.selectById(id);
        if (order == null) {
            return ResponseVO.errorResponse("订单不存在");
        }
        return ResponseVO.successResponse(order);
    }

    @PostMapping("/updateStatus")
    public ResponseVO updateStatus(@RequestBody UpdateStatusRequest request) {
        Order order = orderMapper.selectById(request.getOrderId());
        if (order == null) {
            return ResponseVO.errorResponse("订单不存在");
        }
        
        Order updateOrder = new Order();
        updateOrder.setId(request.getOrderId());
        updateOrder.setState(request.getState());
        updateOrder.setUpdateTime(new Date());
        
        int result = orderMapper.updateById(updateOrder);
        if (result > 0) {
            return ResponseVO.successResponse("状态更新成功");
        } else {
            return ResponseVO.errorResponse("状态更新失败");
        }
    }

    @GetMapping("/statistics")
    public ResponseVO statistics() {
        // 计算当前数据
        LambdaQueryWrapper<Order> totalWrapper = new LambdaQueryWrapper<>();
        Long total = orderMapper.selectCount(totalWrapper);
        
        LambdaQueryWrapper<Order> paidWrapper = new LambdaQueryWrapper<>();
        paidWrapper.eq(Order::getState, OrderStatesEnum.SUCCESS_PAY.getState());
        Long paid = orderMapper.selectCount(paidWrapper);
        
        LambdaQueryWrapper<Order> unpaidWrapper = new LambdaQueryWrapper<>();
        unpaidWrapper.eq(Order::getState, OrderStatesEnum.WAIT_PAY.getState());
        Long unpaid = orderMapper.selectCount(unpaidWrapper);
        
        // 计算昨天数据
        Date today = new Date();
        Date yesterday = new Date(today.getTime() - 24 * 60 * 60 * 1000);
        
        LambdaQueryWrapper<Order> yesterdayTotalWrapper = new LambdaQueryWrapper<>();
        yesterdayTotalWrapper.lt(Order::getCreateTime, yesterday);
        Long yesterdayTotal = orderMapper.selectCount(yesterdayTotalWrapper);
        
        LambdaQueryWrapper<Order> yesterdayPaidWrapper = new LambdaQueryWrapper<>();
        yesterdayPaidWrapper.eq(Order::getState, OrderStatesEnum.SUCCESS_PAY.getState());
        yesterdayPaidWrapper.lt(Order::getCreateTime, yesterday);
        Long yesterdayPaid = orderMapper.selectCount(yesterdayPaidWrapper);
        
        LambdaQueryWrapper<Order> yesterdayUnpaidWrapper = new LambdaQueryWrapper<>();
        yesterdayUnpaidWrapper.eq(Order::getState, OrderStatesEnum.WAIT_PAY.getState());
        yesterdayUnpaidWrapper.lt(Order::getCreateTime, yesterday);
        Long yesterdayUnpaid = orderMapper.selectCount(yesterdayUnpaidWrapper);
        
        // 计算趋势
        int totalTrend = calculateTrend(total, yesterdayTotal);
        int paidTrend = calculateTrend(paid, yesterdayPaid);
        int unpaidTrend = calculateTrend(unpaid, yesterdayUnpaid);
        
        Map<String, Object> data = new HashMap<>();
        data.put("total", total);
        data.put("paid", paid);
        data.put("unpaid", unpaid);
        data.put("totalTrend", totalTrend);
        data.put("paidTrend", paidTrend);
        data.put("unpaidTrend", unpaidTrend);
        
        return ResponseVO.successResponse(data);
    }
    
    private int calculateTrend(Long current, Long previous) {
        if (previous == null || previous == 0) {
            return 100; // 默认增长100%
        }
        return (int) ((current - previous) * 100 / previous);
    }

    /**
     * 手动确认订单支付
     */
    @PostMapping("/confirm/{orderId}")
    public ResponseVO confirmOrder(@PathVariable String orderId) {
        try {
            Order order = orderMapper.selectById(orderId);
            if (order == null) {
                return ResponseVO.errorResponse("订单不存在");
            }

            // 检查订单状态
            if (order.getState().equals(OrderStatesEnum.SUCCESS_PAY.getState())) {
                return ResponseVO.errorResponse("订单已确认，无需重复操作");
            }

            if (!order.getState().equals(OrderStatesEnum.WAIT_PAY.getState())
                && !order.getState().equals(OrderStatesEnum.SCANE_QR.getState())) {
                return ResponseVO.errorResponse("订单状态不允许确认");
            }

            // 调用订单确认逻辑
            int result = orderService.pass(orderId);
            if (result > 0) {
                log.info("管理员手动确认订单成功: orderId={}", orderId);
                return ResponseVO.successResponse("订单确认成功");
            } else {
                return ResponseVO.errorResponse("订单确认失败");
            }
        } catch (Exception e) {
            log.error("手动确认订单失败: orderId={}, error={}", orderId, e.getMessage(), e);
            return ResponseVO.errorResponse("订单确认失败: " + e.getMessage());
        }
    }

    /**
     * 获取超时未确认的订单列表（超过15分钟）
     */
    @GetMapping("/timeout")
    public ResponseVO getTimeoutOrders() {
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

            // 计算每个订单的超时时长（分钟）
            List<Map<String, Object>> result = timeoutOrders.stream().map(order -> {
                Map<String, Object> item = new HashMap<>();
                item.put("order", order);
                long timeoutMinutes = (System.currentTimeMillis() - order.getCreateTime().getTime()) / (60 * 1000);
                item.put("timeoutMinutes", timeoutMinutes);
                return item;
            }).collect(Collectors.toList());

            Map<String, Object> data = new HashMap<>();
            data.put("list", result);
            data.put("total", result.size());

            return ResponseVO.successResponse(data);
        } catch (Exception e) {
            log.error("获取超时订单失败: error={}", e.getMessage(), e);
            return ResponseVO.errorResponse("获取超时订单失败");
        }
    }

    @lombok.Data
    public static class UpdateStatusRequest {
        private String orderId;
        private Integer state;
    }
}
