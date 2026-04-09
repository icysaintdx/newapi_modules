package com.wendao.service.impl;

import cn.hutool.log.StaticLog;
import com.wendao.common.RedisKeyConstant;
import com.wendao.common.utils.EmailUtils;
import com.wendao.common.utils.PushPlusUtils;
import com.wendao.common.utils.ServerChanUtils;
import com.wendao.config.PayProConfig;
import com.wendao.entity.Order;
import com.wendao.entity.Product;
import com.wendao.mapper.ProductMapper;
import com.wendao.service.MultiChannelNotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * 多渠道通知服务实现
 * 优先级: PushPlus微信 > Server酱 > 邮件
 */
@Service
public class MultiChannelNotificationServiceImpl implements MultiChannelNotificationService {

    @Autowired
    private PushPlusUtils pushPlusUtils;

    @Autowired
    private ServerChanUtils serverChanUtils;

    @Autowired
    private EmailUtils emailUtils;

    @Autowired
    private PayProConfig payProConfig;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String PUSHPLUS_COUNT_KEY = "notification:pushplus:count:";
    private static final String SERVERCHAN_COUNT_KEY = "notification:serverchan:count:";

    @Override
    public boolean sendOrderNotification(Order order, String token, String myToken) {
        String today = new SimpleDateFormat("yyyy-MM-dd").format(new Date());

        // 1. 尝试PushPlus微信推送
        if (tryPushPlus(order, token, myToken, today)) {
            StaticLog.info("订单通知已通过PushPlus发送: orderId={}", order.getId());
            return true;
        }

        // 2. 尝试Server酱推送
        if (tryServerChan(order, token, myToken, today)) {
            StaticLog.info("订单通知已通过Server酱发送: orderId={}", order.getId());
            return true;
        }

        // 3. 邮件托底
        return sendEmailNotification(order);
    }

    /**
     * 尝试PushPlus推送
     */
    private boolean tryPushPlus(Order order, String token, String myToken, String today) {
        // 检查是否启用
        if (!payProConfig.getPushplus().getEnabled()) {
            return false;
        }

        String pushToken = payProConfig.getPushplus().getToken();
        if (pushToken == null || pushToken.isEmpty()) {
            return false;
        }

        // 检查今日额度
        String countKey = PUSHPLUS_COUNT_KEY + today;
        String countStr = redisTemplate.opsForValue().get(countKey);
        int count = countStr != null ? Integer.parseInt(countStr) : 0;
        int dailyLimit = payProConfig.getPushplus().getDailyLimit() != null
            ? payProConfig.getPushplus().getDailyLimit() : 190;

        if (count >= dailyLimit) {
            StaticLog.warn("PushPlus今日额度已用完: {}/{}", count, dailyLimit);
            return false;
        }

        try {
            // 构建推送内容
            String title = buildPushTitle(order);
            String content = buildPushContent(order, token, myToken);

            // 发送推送
            boolean success = pushPlusUtils.sendHtmlPush(pushToken, title, content);

            if (success) {
                // 增加计数
                redisTemplate.opsForValue().increment(countKey);
                redisTemplate.expire(countKey, 1, TimeUnit.DAYS);
                StaticLog.info("PushPlus推送成功: orderId={}, 今日已用: {}/{}",
                    order.getId(), count + 1, dailyLimit);
            }

            return success;
        } catch (Exception e) {
            StaticLog.error("PushPlus推送异常: orderId=" + order.getId(), e);
            return false;
        }
    }

    /**
     * 尝试Server酱推送
     */
    private boolean tryServerChan(Order order, String token, String myToken, String today) {
        // 检查是否启用
        if (!payProConfig.getServerchan().getEnabled()) {
            return false;
        }

        String sendkey = payProConfig.getServerchan().getSendkey();
        if (sendkey == null || sendkey.isEmpty()) {
            return false;
        }

        // 检查今日额度
        String countKey = SERVERCHAN_COUNT_KEY + today;
        String countStr = redisTemplate.opsForValue().get(countKey);
        int count = countStr != null ? Integer.parseInt(countStr) : 0;
        int dailyLimit = payProConfig.getServerchan().getDailyLimit() != null
            ? payProConfig.getServerchan().getDailyLimit() : 5;

        if (count >= dailyLimit) {
            StaticLog.warn("Server酱今日额度已用完: {}/{}", count, dailyLimit);
            return false;
        }

        try {
            // 构建推送内容（Markdown格式）
            String title = buildPushTitle(order);
            String content = buildServerChanContent(order, token, myToken);

            // 发送推送
            boolean success = serverChanUtils.sendPush(sendkey, title, content);

            if (success) {
                // 增加计数
                redisTemplate.opsForValue().increment(countKey);
                redisTemplate.expire(countKey, 1, TimeUnit.DAYS);
                StaticLog.info("Server酱推送成功: orderId={}, 今日已用: {}/{}",
                    order.getId(), count + 1, dailyLimit);
            }

            return success;
        } catch (Exception e) {
            StaticLog.error("Server酱推送异常: orderId=" + order.getId(), e);
            return false;
        }
    }

    /**
     * 邮件通知托底
     */
    private boolean sendEmailNotification(Order order) {
        try {
            emailUtils.sendTemplateMail(
                payProConfig.getEmail().getSender(),
                payProConfig.getEmail().getReceiver(),
                "【" + payProConfig.getTitle() + "】待审核处理",
                "payment-review",
                order
            );
            StaticLog.info("邮件通知发送成功: orderId={}", order.getId());
            return true;
        } catch (Exception e) {
            StaticLog.error("邮件通知发送失败: orderId=" + order.getId(), e);
            return false;
        }
    }

    /**
     * 构建推送标题
     */
    private String buildPushTitle(Order order) {
        String orderType = "PRODUCT".equals(order.getOrderSource()) ? "购卡订单" : "充值订单";
        return String.format("💰 新%s - ¥%.2f", orderType, order.getMoney());
    }

    /**
     * 构建PushPlus推送内容（HTML格式）
     */
    private String buildPushContent(Order order, String token, String myToken) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String createTime = sdf.format(order.getCreateTime());

        String orderTypeText = "PRODUCT".equals(order.getOrderSource()) ? "购卡订单" : "NewAPI充值";
        String productInfo = "";
        if ("PRODUCT".equals(order.getOrderSource()) && order.getProductId() != null) {
            Product product = productMapper.selectById(order.getProductId());
            if (product != null) {
                productInfo = String.format("<p><b>商品:</b> %s</p>", product.getProductName());
            }
        }

        String payTypeText = getPayTypeText(order.getPayType());
        String confirmUrl = String.format("%s/order/pass?id=%s&token=%s&myToken=%s",
                payProConfig.getSite(), order.getId(), token, myToken);
        String rejectUrl = String.format("%s/order/back?id=%s&token=%s&myToken=%s",
                payProConfig.getSite(), order.getId(), token, myToken);

        StringBuilder html = new StringBuilder();
        html.append("<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;'>");
        html.append("<h3 style='color: #333; border-bottom: 2px solid #07c160; padding-bottom: 10px;'>💰 收到新订单</h3>");
        html.append(String.format("<p><b>订单类型:</b> %s</p>", orderTypeText));
        html.append(String.format("<p><b>金额:</b> <span style='color: #ff4d4f; font-size: 18px; font-weight: bold;'>¥%.2f</span></p>", order.getMoney()));
        html.append(productInfo);

        if (order.getEmail() != null && !order.getEmail().isEmpty()) {
            html.append(String.format("<p><b>用户邮箱:</b> %s</p>", order.getEmail()));
        }
        if (order.getNickName() != null && !order.getNickName().isEmpty()) {
            html.append(String.format("<p><b>用户昵称:</b> %s</p>", order.getNickName()));
        }

        html.append(String.format("<p><b>支付方式:</b> %s</p>", payTypeText));

        if (order.getPayNum() != null && ("wechat".equals(order.getPayType()) || "wechat_zs".equals(order.getPayType()))) {
            html.append(String.format("<p><b>支付备注:</b> <span style='color: #ff4d4f; font-weight: bold;'>%s</span></p>", order.getPayNum()));
        }

        html.append(String.format("<p><b>创建时间:</b> %s</p>", createTime));
        html.append("<hr style='border: none; border-top: 1px solid #eee; margin: 20px 0;'>");

        html.append("<div style='text-align: center; margin: 30px 0;'>");
        html.append(String.format(
                "<a href='%s' style='display: inline-block; padding: 12px 30px; background: #07c160; color: white; text-decoration: none; border-radius: 5px; margin: 5px; font-size: 16px; font-weight: bold;'>✅ 确认收款</a>",
                confirmUrl));
        html.append(String.format(
                "<a href='%s' style='display: inline-block; padding: 12px 30px; background: #fa5151; color: white; text-decoration: none; border-radius: 5px; margin: 5px; font-size: 16px; font-weight: bold;'>❌ 拒绝订单</a>",
                rejectUrl));
        html.append("</div>");

        html.append("<p style='font-size: 12px; color: #999; text-align: center;'>💡 提示: 收款码永久有效，无需担心超时</p>");
        html.append("</div>");

        return html.toString();
    }

    /**
     * 构建Server酱推送内容（Markdown格式）
     */
    private String buildServerChanContent(Order order, String token, String myToken) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String createTime = sdf.format(order.getCreateTime());

        String orderTypeText = "PRODUCT".equals(order.getOrderSource()) ? "购卡订单" : "NewAPI充值";
        String productName = "";
        if ("PRODUCT".equals(order.getOrderSource()) && order.getProductId() != null) {
            Product product = productMapper.selectById(order.getProductId());
            if (product != null) {
                productName = product.getProductName();
            }
        }

        String payTypeText = getPayTypeText(order.getPayType());
        String confirmUrl = String.format("%s/order/pass?id=%s&token=%s&myToken=%s",
                payProConfig.getSite(), order.getId(), token, myToken);
        String rejectUrl = String.format("%s/order/back?id=%s&token=%s&myToken=%s",
                payProConfig.getSite(), order.getId(), token, myToken);

        StringBuilder md = new StringBuilder();
        md.append("## 💰 收到新订单\n\n");
        md.append(String.format("**订单类型:** %s\n\n", orderTypeText));
        md.append(String.format("**金额:** ¥%.2f\n\n", order.getMoney()));

        if (!productName.isEmpty()) {
            md.append(String.format("**商品:** %s\n\n", productName));
        }
        if (order.getEmail() != null && !order.getEmail().isEmpty()) {
            md.append(String.format("**用户邮箱:** %s\n\n", order.getEmail()));
        }

        md.append(String.format("**支付方式:** %s\n\n", payTypeText));

        if (order.getPayNum() != null && ("wechat".equals(order.getPayType()) || "wechat_zs".equals(order.getPayType()))) {
            md.append(String.format("**支付备注:** %s\n\n", order.getPayNum()));
        }

        md.append(String.format("**创建时间:** %s\n\n", createTime));
        md.append("---\n\n");
        md.append(String.format("[✅ 确认收款](%s)\n\n", confirmUrl));
        md.append(String.format("[❌ 拒绝订单](%s)\n\n", rejectUrl));
        md.append("💡 提示: 收款码永久有效，无需担心超时\n");

        return md.toString();
    }

    /**
     * 获取支付方式显示名称
     */
    private String getPayTypeText(String payType) {
        if (payType == null) return "未知";
        switch (payType) {
            case "alipay":
                return "支付宝支付";
            case "wechat":
                return "微信支付";
            case "wechat_zs":
                return "微信赞赏码";
            case "alipay_dmf":
                return "支付宝当面付";
            default:
                return payType;
        }
    }
}
