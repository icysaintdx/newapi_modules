package com.wendao.service.impl;

import cn.hutool.log.StaticLog;
import com.wendao.common.utils.PushPlusUtils;
import com.wendao.config.PayProConfig;
import com.wendao.entity.Order;
import com.wendao.entity.Product;
import com.wendao.mapper.ProductMapper;
import com.wendao.service.PushPlusService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;

/**
 * PushPlus推送服务实现
 */
@Service
public class PushPlusServiceImpl implements PushPlusService {

    @Autowired
    private PushPlusUtils pushPlusUtils;

    @Autowired
    private PayProConfig payProConfig;

    @Autowired
    private ProductMapper productMapper;

    @Override
    public boolean sendOrderReviewPush(Order order, String token, String myToken) {
        // 检查是否启用PushPlus
        if (!payProConfig.getPushplus().getEnabled()) {
            StaticLog.info("PushPlus推送未启用，跳过推送");
            return false;
        }

        String pushToken = payProConfig.getPushplus().getToken();
        if (pushToken == null || pushToken.isEmpty()) {
            StaticLog.warn("PushPlus Token未配置");
            return false;
        }

        try {
            // 构建HTML推送内容
            String htmlContent = buildOrderReviewHtml(order, token, myToken);

            // 发送推送
            String title = buildPushTitle(order);
            boolean success = pushPlusUtils.sendHtmlPush(pushToken, title, htmlContent);

            if (success) {
                StaticLog.info("PushPlus推送成功: orderId={}", order.getId());
            } else {
                StaticLog.error("PushPlus推送失败: orderId={}", order.getId());
            }

            return success;
        } catch (Exception e) {
            StaticLog.error("PushPlus推送异常: orderId=" + order.getId(), e);
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
     * 构建订单审核HTML内容 - 参考PushPlus官方支付场景模板
     */
    private String buildOrderReviewHtml(Order order, String token, String myToken) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String createTime = sdf.format(order.getCreateTime());

        // 获取订单类型和商品信息
        String orderTypeText = "PRODUCT".equals(order.getOrderSource()) ? "购卡订单" : "NewAPI充值";
        String productName = "";
        if ("PRODUCT".equals(order.getOrderSource()) && order.getProductId() != null) {
            Product product = productMapper.selectById(order.getProductId());
            if (product != null) {
                productName = product.getProductName();
            }
        }

        // 获取支付方式显示名称
        String payTypeText = getPayTypeText(order.getPayType());

        // 构建确认和拒绝链接
        String confirmUrl = String.format("%s/order/pass?id=%s&token=%s&myToken=%s",
                payProConfig.getSite(), order.getId(), token, myToken);
        String rejectUrl = String.format("%s/order/back?id=%s&token=%s&myToken=%s",
                payProConfig.getSite(), order.getId(), token, myToken);

        // 构建HTML内容 - 使用卡片式设计
        StringBuilder html = new StringBuilder();
        html.append("<div style='font-family: -apple-system, BlinkMacSystemFont, \"Segoe UI\", Roboto, \"Helvetica Neue\", Arial, sans-serif; max-width: 600px; margin: 0 auto; background: #f5f5f5; padding: 20px;'>");

        // 头部卡片
        html.append("<div style='background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); border-radius: 12px 12px 0 0; padding: 24px; text-align: center;'>");
        html.append("<h2 style='color: white; margin: 0; font-size: 24px; font-weight: 600;'>💰 收到新订单</h2>");
        html.append("<p style='color: rgba(255,255,255,0.9); margin: 8px 0 0 0; font-size: 14px;'>订单待审核处理</p>");
        html.append("</div>");

        // 金额卡片
        html.append("<div style='background: white; padding: 32px 24px; text-align: center; border-bottom: 1px solid #eee;'>");
        html.append("<p style='color: #999; font-size: 14px; margin: 0 0 8px 0;'>订单金额</p>");
        html.append(String.format("<p style='color: #ff4d4f; font-size: 42px; font-weight: bold; margin: 0; line-height: 1;'>¥%.2f</p>", order.getMoney()));
        html.append("</div>");

        // 订单详情卡片
        html.append("<div style='background: white; padding: 24px;'>");
        html.append("<table style='width: 100%; border-collapse: collapse;'>");

        // 订单类型
        html.append("<tr><td style='padding: 12px 0; border-bottom: 1px solid #f0f0f0;'>");
        html.append("<span style='color: #999; font-size: 14px;'>订单类型</span></td>");
        html.append(String.format("<td style='padding: 12px 0; border-bottom: 1px solid #f0f0f0; text-align: right;'><span style='color: #333; font-size: 14px; font-weight: 500;'>%s</span></td></tr>", orderTypeText));

        // 商品名称（如果有）
        if (!productName.isEmpty()) {
            html.append("<tr><td style='padding: 12px 0; border-bottom: 1px solid #f0f0f0;'>");
            html.append("<span style='color: #999; font-size: 14px;'>商品名称</span></td>");
            html.append(String.format("<td style='padding: 12px 0; border-bottom: 1px solid #f0f0f0; text-align: right;'><span style='color: #333; font-size: 14px; font-weight: 500;'>%s</span></td></tr>", productName));
        }

        // 支付方式
        html.append("<tr><td style='padding: 12px 0; border-bottom: 1px solid #f0f0f0;'>");
        html.append("<span style='color: #999; font-size: 14px;'>支付方式</span></td>");
        html.append(String.format("<td style='padding: 12px 0; border-bottom: 1px solid #f0f0f0; text-align: right;'><span style='color: #333; font-size: 14px; font-weight: 500;'>%s</span></td></tr>", payTypeText));

        // 用户邮箱
        if (order.getEmail() != null && !order.getEmail().isEmpty()) {
            html.append("<tr><td style='padding: 12px 0; border-bottom: 1px solid #f0f0f0;'>");
            html.append("<span style='color: #999; font-size: 14px;'>用户邮箱</span></td>");
            html.append(String.format("<td style='padding: 12px 0; border-bottom: 1px solid #f0f0f0; text-align: right;'><span style='color: #333; font-size: 14px;'>%s</span></td></tr>", order.getEmail()));
        }

        // 用户昵称
        if (order.getNickName() != null && !order.getNickName().isEmpty()) {
            html.append("<tr><td style='padding: 12px 0; border-bottom: 1px solid #f0f0f0;'>");
            html.append("<span style='color: #999; font-size: 14px;'>用户昵称</span></td>");
            html.append(String.format("<td style='padding: 12px 0; border-bottom: 1px solid #f0f0f0; text-align: right;'><span style='color: #333; font-size: 14px;'>%s</span></td></tr>", order.getNickName()));
        }

        // 支付备注（重要提示）
        if (order.getPayNum() != null && ("wechat".equals(order.getPayType()) || "wechat_zs".equals(order.getPayType()))) {
            html.append("<tr><td style='padding: 12px 0; border-bottom: 1px solid #f0f0f0;'>");
            html.append("<span style='color: #999; font-size: 14px;'>支付备注</span></td>");
            html.append(String.format("<td style='padding: 12px 0; border-bottom: 1px solid #f0f0f0; text-align: right;'><span style='color: #ff4d4f; font-size: 14px; font-weight: bold;'>%s</span></td></tr>", order.getPayNum()));
        }

        // 订单编号
        html.append("<tr><td style='padding: 12px 0; border-bottom: 1px solid #f0f0f0;'>");
        html.append("<span style='color: #999; font-size: 14px;'>订单编号</span></td>");
        html.append(String.format("<td style='padding: 12px 0; border-bottom: 1px solid #f0f0f0; text-align: right;'><span style='color: #666; font-size: 12px; font-family: monospace;'>#%s</span></td></tr>", order.getId()));

        // 创建时间
        html.append("<tr><td style='padding: 12px 0;'>");
        html.append("<span style='color: #999; font-size: 14px;'>创建时间</span></td>");
        html.append(String.format("<td style='padding: 12px 0; text-align: right;'><span style='color: #666; font-size: 14px;'>%s</span></td></tr>", createTime));

        html.append("</table>");
        html.append("</div>");

        // 操作按钮区域
        html.append("<div style='background: white; padding: 24px; border-radius: 0 0 12px 12px;'>");
        html.append("<div style='display: flex; gap: 12px; margin-bottom: 16px;'>");
        html.append(String.format(
                "<a href='%s' style='flex: 1; display: block; padding: 14px 0; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; text-decoration: none; border-radius: 8px; text-align: center; font-size: 16px; font-weight: 600; box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);'>✅ 确认收款</a>",
                confirmUrl));
        html.append(String.format(
                "<a href='%s' style='flex: 1; display: block; padding: 14px 0; background: #fff; color: #666; text-decoration: none; border-radius: 8px; text-align: center; font-size: 16px; font-weight: 600; border: 2px solid #e8e8e8;'>❌ 拒绝订单</a>",
                rejectUrl));
        html.append("</div>");

        // 提示信息
        html.append("<div style='background: #fff9e6; border-left: 3px solid #faad14; padding: 12px; border-radius: 4px;'>");
        html.append("<p style='margin: 0; color: #8c6d1f; font-size: 13px; line-height: 1.6;'>⏰ 订单将在30分钟后自动过期<br>点击按钮后将立即处理订单</p>");
        html.append("</div>");
        html.append("</div>");

        html.append("</div>");

        return html.toString();
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
