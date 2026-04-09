package com.wendao.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.hutool.log.StaticLog;
import com.wendao.common.utils.PushPlusUtils;
import com.wendao.common.utils.ServerChanUtils;
import com.wendao.model.ResponseVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 推送通知测试控制器
 */
@RestController
@RequestMapping("/admin/api/notification")
@Api(tags = "推送通知测试")
public class NotificationTestController {

    @Autowired
    private PushPlusUtils pushPlusUtils;

    @Autowired
    private ServerChanUtils serverChanUtils;

    @Autowired
    private com.wendao.service.EmailService emailService;

    /**
     * 测试PushPlus推送
     */
    @PostMapping("/test/pushplus")
    @ApiOperation("测试PushPlus推送")
    public ResponseVO testPushPlus(@RequestBody Map<String, String> params) {
        try {
            String token = params.get("token");
            if (token == null || token.isEmpty()) {
                return ResponseVO.errorResponse("Token不能为空");
            }

            String title = "🔔 PayPro测试推送";
            String content = buildTestContent();

            boolean success = pushPlusUtils.sendHtmlPush(token, title, content);

            if (success) {
                StaticLog.info("PushPlus测试推送成功");
                return ResponseVO.successResponse("测试推送已发送，请查看微信");
            } else {
                return ResponseVO.errorResponse("推送失败，请检查Token是否正确");
            }
        } catch (Exception e) {
            StaticLog.error("PushPlus测试推送失败", e);
            return ResponseVO.errorResponse("测试失败: " + e.getMessage());
        }
    }

    /**
     * 测试邮件推送
     */
    @PostMapping("/test/email")
    @ApiOperation("测试邮件推送")
    public ResponseVO testEmail(@RequestBody Map<String, String> params) {
        try {
            String email = params.get("email");
            if (email == null || email.isEmpty()) {
                return ResponseVO.errorResponse("邮箱地址不能为空");
            }

            // 发送测试邮件
            String result = emailService.sendTestEmail(email);

            StaticLog.info("邮件测试推送成功: {}", email);
            return ResponseVO.successResponse(result);
        } catch (Exception e) {
            StaticLog.error("邮件测试推送失败", e);
            return ResponseVO.errorResponse("测试失败: " + e.getMessage());
        }
    }

    /**
     * 预览订单推送模板（虚拟数据）
     */
    @GetMapping("/preview/order")
    @ApiOperation("预览订单推送模板")
    public String previewOrderTemplate() {
        return buildMockOrderTemplate();
    }

    /**
     * 预览订单邮件模板（虚拟数据）
     */
    @GetMapping("/preview/email")
    @ApiOperation("预览订单邮件模板")
    public String previewEmailTemplate() {
        return buildMockEmailTemplate();
    }

    /**
     * 测试Server酱推送
     */
    @PostMapping("/test/serverchan")
    @ApiOperation("测试Server酱推送")
    public ResponseVO testServerChan(@RequestBody Map<String, String> params) {
        try {
            String sendkey = params.get("sendkey");
            if (sendkey == null || sendkey.isEmpty()) {
                return ResponseVO.errorResponse("SendKey不能为空");
            }

            String title = "🔔 PayPro测试推送";
            String content = buildTestContentMarkdown();

            boolean success = serverChanUtils.sendPush(sendkey, title, content);

            if (success) {
                StaticLog.info("Server酱测试推送成功");
                return ResponseVO.successResponse("测试推送已发送，请查看微信");
            } else {
                return ResponseVO.errorResponse("推送失败，请检查SendKey是否正确");
            }
        } catch (Exception e) {
            StaticLog.error("Server酱测试推送失败", e);
            return ResponseVO.errorResponse("测试失败: " + e.getMessage());
        }
    }

    /**
     * 构建测试推送内容(HTML格式)
     */
    private String buildTestContent() {
        StringBuilder html = new StringBuilder();
        html.append("<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;'>");
        html.append("<h3 style='color: #333;'>✅ 推送配置测试成功</h3>");
        html.append("<p>恭喜！您的推送通知配置正常工作。</p>");
        html.append("<hr style='border: none; border-top: 1px solid #eee; margin: 20px 0;'>");
        html.append("<p><b>测试时间:</b> " + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()) + "</p>");
        html.append("<p><b>系统:</b> PayPro 支付系统</p>");
        html.append("<p style='font-size: 12px; color: #999; margin-top: 20px;'>这是一条测试消息，实际订单通知会包含完整的订单信息和操作按钮。</p>");
        html.append("</div>");
        return html.toString();
    }

    /**
     * 构建测试推送内容(Markdown格式)
     */
    private String buildTestContentMarkdown() {
        StringBuilder md = new StringBuilder();
        md.append("## ✅ 推送配置测试成功\n\n");
        md.append("恭喜！您的推送通知配置正常工作。\n\n");
        md.append("---\n\n");
        md.append("**测试时间:** " + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()) + "\n\n");
        md.append("**系统:** PayPro 支付系统\n\n");
        md.append("这是一条测试消息，实际订单通知会包含完整的订单信息和操作按钮。\n");
        return md.toString();
    }

    /**
     * 构建虚拟订单推送模板（用于预览）
     */
    private String buildMockOrderTemplate() {
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
        html.append("<p style='color: #ff4d4f; font-size: 42px; font-weight: bold; margin: 0; line-height: 1;'>¥188.00</p>");
        html.append("</div>");

        // 订单详情卡片
        html.append("<div style='background: white; padding: 24px;'>");
        html.append("<table style='width: 100%; border-collapse: collapse;'>");

        // 订单类型
        html.append("<tr><td style='padding: 12px 0; border-bottom: 1px solid #f0f0f0;'>");
        html.append("<span style='color: #999; font-size: 14px;'>订单类型</span></td>");
        html.append("<td style='padding: 12px 0; border-bottom: 1px solid #f0f0f0; text-align: right;'><span style='color: #333; font-size: 14px; font-weight: 500;'>购卡订单</span></td></tr>");

        // 商品名称
        html.append("<tr><td style='padding: 12px 0; border-bottom: 1px solid #f0f0f0;'>");
        html.append("<span style='color: #999; font-size: 14px;'>商品名称</span></td>");
        html.append("<td style='padding: 12px 0; border-bottom: 1px solid #f0f0f0; text-align: right;'><span style='color: #333; font-size: 14px; font-weight: 500;'>Claude API 充值卡 - 100美元</span></td></tr>");

        // 支付方式
        html.append("<tr><td style='padding: 12px 0; border-bottom: 1px solid #f0f0f0;'>");
        html.append("<span style='color: #999; font-size: 14px;'>支付方式</span></td>");
        html.append("<td style='padding: 12px 0; border-bottom: 1px solid #f0f0f0; text-align: right;'><span style='color: #333; font-size: 14px; font-weight: 500;'>微信支付</span></td></tr>");

        // 用户邮箱
        html.append("<tr><td style='padding: 12px 0; border-bottom: 1px solid #f0f0f0;'>");
        html.append("<span style='color: #999; font-size: 14px;'>用户邮箱</span></td>");
        html.append("<td style='padding: 12px 0; border-bottom: 1px solid #f0f0f0; text-align: right;'><span style='color: #333; font-size: 14px;'>user@example.com</span></td></tr>");

        // 用户昵称
        html.append("<tr><td style='padding: 12px 0; border-bottom: 1px solid #f0f0f0;'>");
        html.append("<span style='color: #999; font-size: 14px;'>用户昵称</span></td>");
        html.append("<td style='padding: 12px 0; border-bottom: 1px solid #f0f0f0; text-align: right;'><span style='color: #333; font-size: 14px;'>张三</span></td></tr>");

        // 支付备注
        html.append("<tr><td style='padding: 12px 0; border-bottom: 1px solid #f0f0f0;'>");
        html.append("<span style='color: #999; font-size: 14px;'>支付备注</span></td>");
        html.append("<td style='padding: 12px 0; border-bottom: 1px solid #f0f0f0; text-align: right;'><span style='color: #ff4d4f; font-size: 14px; font-weight: bold;'>8888</span></td></tr>");

        // 订单编号
        html.append("<tr><td style='padding: 12px 0; border-bottom: 1px solid #f0f0f0;'>");
        html.append("<span style='color: #999; font-size: 14px;'>订单编号</span></td>");
        html.append("<td style='padding: 12px 0; border-bottom: 1px solid #f0f0f0; text-align: right;'><span style='color: #666; font-size: 12px; font-family: monospace;'>#20260408001</span></td></tr>");

        // 创建时间
        html.append("<tr><td style='padding: 12px 0;'>");
        html.append("<span style='color: #999; font-size: 14px;'>创建时间</span></td>");
        html.append("<td style='padding: 12px 0; text-align: right;'><span style='color: #666; font-size: 14px;'>2026-04-08 19:52:30</span></td></tr>");

        html.append("</table>");
        html.append("</div>");

        // 操作按钮区域
        html.append("<div style='background: white; padding: 24px; border-radius: 0 0 12px 12px;'>");
        html.append("<div style='display: flex; gap: 12px; margin-bottom: 16px;'>");
        html.append("<a href='#' style='flex: 1; display: block; padding: 14px 0; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; text-decoration: none; border-radius: 8px; text-align: center; font-size: 16px; font-weight: 600; box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);'>✅ 确认收款</a>");
        html.append("<a href='#' style='flex: 1; display: block; padding: 14px 0; background: #fff; color: #666; text-decoration: none; border-radius: 8px; text-align: center; font-size: 16px; font-weight: 600; border: 2px solid #e8e8e8;'>❌ 拒绝订单</a>");
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
     * 构建虚拟订单邮件模板（用于预览）
     */
    private String buildMockEmailTemplate() {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>");
        html.append("<html>");
        html.append("<head><meta charset='UTF-8'><meta name='viewport' content='width=device-width, initial-scale=1.0'></head>");
        html.append("<body style='margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, \"Segoe UI\", Roboto, \"Helvetica Neue\", Arial, sans-serif; background-color: #f5f5f5;'>");

        html.append("<div style='max-width: 600px; margin: 40px auto; background: white; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 12px rgba(0,0,0,0.1);'>");

        // 头部
        html.append("<div style='background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); padding: 32px 24px; text-align: center;'>");
        html.append("<h1 style='color: white; margin: 0; font-size: 28px; font-weight: 600;'>💰 收到新订单</h1>");
        html.append("<p style='color: rgba(255,255,255,0.9); margin: 12px 0 0 0; font-size: 16px;'>订单待审核处理</p>");
        html.append("</div>");

        // 金额区域
        html.append("<div style='padding: 40px 24px; text-align: center; background: #fafafa; border-bottom: 2px solid #eee;'>");
        html.append("<p style='color: #999; font-size: 14px; margin: 0 0 12px 0; text-transform: uppercase; letter-spacing: 1px;'>订单金额</p>");
        html.append("<p style='color: #ff4d4f; font-size: 48px; font-weight: bold; margin: 0; line-height: 1;'>¥188.00</p>");
        html.append("</div>");

        // 订单详情
        html.append("<div style='padding: 32px 24px;'>");
        html.append("<h2 style='color: #333; font-size: 18px; margin: 0 0 20px 0; padding-bottom: 12px; border-bottom: 2px solid #f0f0f0;'>订单详情</h2>");

        html.append("<table style='width: 100%; border-collapse: collapse;'>");

        // 订单类型
        html.append("<tr><td style='padding: 16px 0; border-bottom: 1px solid #f5f5f5;'>");
        html.append("<span style='color: #999; font-size: 14px;'>订单类型</span></td>");
        html.append("<td style='padding: 16px 0; border-bottom: 1px solid #f5f5f5; text-align: right;'>");
        html.append("<span style='color: #333; font-size: 15px; font-weight: 600;'>购卡订单</span></td></tr>");

        // 商品名称
        html.append("<tr><td style='padding: 16px 0; border-bottom: 1px solid #f5f5f5;'>");
        html.append("<span style='color: #999; font-size: 14px;'>商品名称</span></td>");
        html.append("<td style='padding: 16px 0; border-bottom: 1px solid #f5f5f5; text-align: right;'>");
        html.append("<span style='color: #333; font-size: 15px; font-weight: 600;'>Claude API 充值卡 - 100美元</span></td></tr>");

        // 支付方式
        html.append("<tr><td style='padding: 16px 0; border-bottom: 1px solid #f5f5f5;'>");
        html.append("<span style='color: #999; font-size: 14px;'>支付方式</span></td>");
        html.append("<td style='padding: 16px 0; border-bottom: 1px solid #f5f5f5; text-align: right;'>");
        html.append("<span style='color: #333; font-size: 15px; font-weight: 600;'>微信支付</span></td></tr>");

        // 用户邮箱
        html.append("<tr><td style='padding: 16px 0; border-bottom: 1px solid #f5f5f5;'>");
        html.append("<span style='color: #999; font-size: 14px;'>用户邮箱</span></td>");
        html.append("<td style='padding: 16px 0; border-bottom: 1px solid #f5f5f5; text-align: right;'>");
        html.append("<span style='color: #666; font-size: 14px;'>user@example.com</span></td></tr>");

        // 用户昵称
        html.append("<tr><td style='padding: 16px 0; border-bottom: 1px solid #f5f5f5;'>");
        html.append("<span style='color: #999; font-size: 14px;'>用户昵称</span></td>");
        html.append("<td style='padding: 16px 0; border-bottom: 1px solid #f5f5f5; text-align: right;'>");
        html.append("<span style='color: #666; font-size: 14px;'>张三</span></td></tr>");

        // 支付备注
        html.append("<tr><td style='padding: 16px 0; border-bottom: 1px solid #f5f5f5;'>");
        html.append("<span style='color: #999; font-size: 14px;'>支付备注</span></td>");
        html.append("<td style='padding: 16px 0; border-bottom: 1px solid #f5f5f5; text-align: right;'>");
        html.append("<span style='color: #ff4d4f; font-size: 15px; font-weight: bold;'>8888</span></td></tr>");

        // 订单编号
        html.append("<tr><td style='padding: 16px 0; border-bottom: 1px solid #f5f5f5;'>");
        html.append("<span style='color: #999; font-size: 14px;'>订单编号</span></td>");
        html.append("<td style='padding: 16px 0; border-bottom: 1px solid #f5f5f5; text-align: right;'>");
        html.append("<span style='color: #666; font-size: 13px; font-family: monospace;'>#20260408001</span></td></tr>");

        // 创建时间
        html.append("<tr><td style='padding: 16px 0;'>");
        html.append("<span style='color: #999; font-size: 14px;'>创建时间</span></td>");
        html.append("<td style='padding: 16px 0; text-align: right;'>");
        html.append("<span style='color: #666; font-size: 14px;'>2026-04-08 20:05:30</span></td></tr>");

        html.append("</table>");
        html.append("</div>");

        // 操作按钮
        html.append("<div style='padding: 32px 24px; background: #fafafa;'>");
        html.append("<table width='100%' cellpadding='0' cellspacing='0'><tr>");
        html.append("<td style='padding-right: 8px;'>");
        html.append("<a href='#' style='display: block; padding: 16px 0; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; text-decoration: none; border-radius: 8px; text-align: center; font-size: 16px; font-weight: 600;'>✅ 确认收款</a>");
        html.append("</td>");
        html.append("<td style='padding-left: 8px;'>");
        html.append("<a href='#' style='display: block; padding: 16px 0; background: white; color: #666; text-decoration: none; border-radius: 8px; text-align: center; font-size: 16px; font-weight: 600; border: 2px solid #e8e8e8;'>❌ 拒绝订单</a>");
        html.append("</td>");
        html.append("</tr></table>");
        html.append("</div>");

        // 提示信息
        html.append("<div style='padding: 24px; background: #fff9e6; border-top: 1px solid #ffe58f;'>");
        html.append("<p style='margin: 0; color: #8c6d1f; font-size: 14px; line-height: 1.8; text-align: center;'>");
        html.append("⏰ 订单将在30分钟后自动过期<br>");
        html.append("点击按钮后将立即处理订单");
        html.append("</p>");
        html.append("</div>");

        // 页脚
        html.append("<div style='padding: 24px; text-align: center; background: #f5f5f5; border-top: 1px solid #eee;'>");
        html.append("<p style='margin: 0; color: #999; font-size: 12px;'>此邮件由 PayPro 支付系统自动发送，请勿直接回复</p>");
        html.append("<p style='margin: 8px 0 0 0; color: #999; font-size: 12px;'>© 2026 PayPro. All rights reserved.</p>");
        html.append("</div>");

        html.append("</div>");
        html.append("</body>");
        html.append("</html>");

        return html.toString();
    }
}
