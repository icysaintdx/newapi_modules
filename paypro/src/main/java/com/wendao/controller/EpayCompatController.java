package com.wendao.controller;

import cn.hutool.crypto.SecureUtil;
import com.wendao.entity.Order;
import com.wendao.model.ResponseVO;
import com.wendao.service.OrderService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import com.wendao.common.RedisKeyConstant;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 易支付兼容接口
 * 让 New-API 可以直接使用 PayPro 作为易支付服务
 */
@Controller
@Api(tags = "易支付兼容接口")
public class EpayCompatController {

    private static final Logger log = LoggerFactory.getLogger(EpayCompatController.class);

    @Autowired
    private OrderService orderService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private com.wendao.service.SystemConfigService systemConfigService;

    @Autowired
    private com.wendao.mapper.AmountMappingMapper amountMappingMapper;

    @Value("${paypro.site}")
    private String site;

    @Value("${paypro.openapi.secret}")
    private String apiSecret;

    /**
     * 易支付创建订单接口
     * 兼容 New-API 的易支付调用
     */
    @RequestMapping(value = "/submit.php", method = RequestMethod.POST)
    @ResponseBody
    @ApiOperation(value = "易支付创建订单（兼容接口）")
    public String submitOrder(
            @RequestParam String pid,
            @RequestParam String type,
            @RequestParam("out_trade_no") String outTradeNo,
            @RequestParam("notify_url") String notifyUrl,
            @RequestParam String name,
            @RequestParam String money,
            @RequestParam(required = false, defaultValue = "pc") String device,
            @RequestParam("sign_type") String signType,
            @RequestParam("return_url") String returnUrl,
            @RequestParam String sign,
            HttpServletRequest request
    ) {
        try {
            log.info("收到易支付订单请求: pid={}, type={}, out_trade_no={}, money={}", 
                    pid, type, outTradeNo, money);

            // 验证签名
            Map<String, String> params = new HashMap<>();
            params.put("pid", pid);
            params.put("type", type);
            params.put("out_trade_no", outTradeNo);
            params.put("notify_url", notifyUrl);
            params.put("name", name);
            params.put("money", money);
            params.put("device", device);
            params.put("sign_type", signType);
            params.put("return_url", returnUrl);

            String calculatedSign = generateEpaySign(params, apiSecret);
            if (!calculatedSign.equals(sign)) {
                log.warn("签名验证失败: 期望={}, 实际={}", calculatedSign, sign);
                return buildErrorResponse("签名验证失败");
            }

            // 查询金额映射，获取实际支付金额
            BigDecimal chargeAmount = new BigDecimal(money);
            List<com.wendao.entity.AmountMapping> mappings = amountMappingMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.wendao.entity.AmountMapping>()
                    .eq(com.wendao.entity.AmountMapping::getPayType, type)
                    .eq(com.wendao.entity.AmountMapping::getChargeAmount, chargeAmount)
                    .eq(com.wendao.entity.AmountMapping::getEnabled, true)
                    .orderByAsc(com.wendao.entity.AmountMapping::getPriority)
            );

            // 随机选择一个实际支付金额
            BigDecimal actualAmount = chargeAmount;
            if (!mappings.isEmpty()) {
                actualAmount = mappings.get(new java.util.Random().nextInt(mappings.size())).getActualAmount();
            }

            // 检查是否有对应金额的二维码文件
            String formattedActualAmount = String.format("%.2f", actualAmount);
            String qrFilePath = String.format("/app/static/qr/%s/%s_fixed_%s_001.jpg", type, type, formattedActualAmount);
            String qrFilePathPng = String.format("/app/static/qr/%s/%s_fixed_%s_001.png", type, type, formattedActualAmount);

            java.io.File qrFileJpg = new java.io.File(qrFilePath);
            java.io.File qrFilePng = new java.io.File(qrFilePathPng);

            if (!qrFileJpg.exists() && !qrFilePng.exists()) {
                log.error("没有找到对应金额的二维码: payType={}, amount={}", type, formattedActualAmount);
                return buildErrorResponse("不支持自定义金额充值，请选择固定额度充值");
            }

            // 创建订单
            Order order = new Order();
            order.setId(outTradeNo);
            order.setMoney(chargeAmount);  // 用户充值金额
            order.setPayType(type);
            order.setNickName(name);
            order.setNotifyUrl(notifyUrl);
            order.setDevice(request.getHeader("user-agent"));
            order.setCustom(false);
            order.setMobile(device.equals("mobile"));

            // 保存订单（会自动生成 payNum）
            orderService.addOrder(order);

            // 重新查询订单以获取生成的 payNum
            Order savedOrder = orderService.getOrderById(outTradeNo);

            // 易支付接口使用固定金额二维码，利用金额映射功能
            String paymentUrl = site + "/payment.html" +
                    "?orderId=" + outTradeNo +
                    "&money=" + money +  // 用户充值金额
                    "&payType=" + type +
                    "&payNum=" + (savedOrder.getPayNum() != null ? savedOrder.getPayNum() : "") +
                    "&customerQr=false" +
                    "&actualAmount=" + formattedActualAmount +  // 实际支付金额（映射后）
                    "&qrCode=undefined" +
                    "&payQrNum=1" +
                    "&useLocalQrCode=false";

            log.info("订单创建成功，跳转到支付页面: {}", paymentUrl);

            // 返回重定向HTML
            return buildRedirectHtml(paymentUrl);

        } catch (Exception e) {
            log.error("创建订单失败", e);
            return buildErrorResponse("创建订单失败: " + e.getMessage());
        }
    }

    /**
     * 易支付回调验证接口（已加强安全验证）
     * 用于 New-API 验证回调签名
     */
    @RequestMapping(value = "/api/epay/verify", method = {RequestMethod.GET, RequestMethod.POST})
    @ResponseBody
    @ApiOperation(value = "易支付回调验证（兼容接口）")
    public String verifyCallback(
            @RequestParam(required = false) String type,
            @RequestParam(value = "trade_no", required = false) String tradeNo,
            @RequestParam(value = "out_trade_no", required = false) String outTradeNo,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String money,
            @RequestParam(value = "trade_status", required = false) String tradeStatus,
            @RequestParam(required = false) String sign
    ) {
        try {
            log.info("收到易支付回调验证: out_trade_no={}, money={}, trade_status={}", 
                    outTradeNo, money, tradeStatus);

            // 构建参数
            Map<String, String> params = new HashMap<>();
            if (type != null) params.put("type", type);
            if (tradeNo != null) params.put("trade_no", tradeNo);
            if (outTradeNo != null) params.put("out_trade_no", outTradeNo);
            if (name != null) params.put("name", name);
            if (money != null) params.put("money", money);
            if (tradeStatus != null) params.put("trade_status", tradeStatus);

            // 1. 验证签名
            String calculatedSign = generateEpaySign(params, apiSecret);
            if (!calculatedSign.equals(sign)) {
                log.warn("回调签名验证失败: 期望={}, 实际={}", calculatedSign, sign);
                return "fail";
            }

            // 2. 查询订单，验证金额
            Order order = orderService.getOrderById(outTradeNo);
            if (order == null) {
                log.error("订单不存在: {}", outTradeNo);
                return "fail";
            }

            // 3. 验证金额是否在映射范围内（防止篡改金额）
            if (money != null) {
                BigDecimal actualMoney = new BigDecimal(money);
                BigDecimal orderMoney = order.getMoney();

                // 查询该订单金额的所有映射金额
                List<com.wendao.entity.AmountMapping> mappings = amountMappingMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.wendao.entity.AmountMapping>()
                        .eq(com.wendao.entity.AmountMapping::getPayType, order.getPayType())
                        .eq(com.wendao.entity.AmountMapping::getChargeAmount, orderMoney)
                        .eq(com.wendao.entity.AmountMapping::getEnabled, true)
                );

                // 检查实际支付金额是否在映射范围内
                boolean isValidAmount = false;
                if (mappings.isEmpty()) {
                    // 没有映射，必须完全匹配订单金额
                    isValidAmount = orderMoney.compareTo(actualMoney) == 0;
                } else {
                    // 有映射，检查是否在映射列表中
                    isValidAmount = orderMoney.compareTo(actualMoney) == 0 ||
                                   mappings.stream().anyMatch(m -> m.getActualAmount().compareTo(actualMoney) == 0);
                }

                if (!isValidAmount) {
                    log.error("订单金额验证失败: orderId={}, 订单金额={}, 实际支付={}, 映射数量={}",
                            outTradeNo, orderMoney, actualMoney, mappings.size());
                    return "fail";
                }

                log.info("订单金额验证通过: orderId={}, 订单金额={}, 实际支付={}",
                        outTradeNo, orderMoney, actualMoney);
            }

            // 4. 检查订单状态（防止重复处理）
            if (order.getState() != null && order.getState() == 1) {
                log.warn("订单已处理，跳过重复回调: {}", outTradeNo);
                return "success";  // 已处理过，返回成功避免重复回调
            }

            // 5. 更新订单状态
            if ("TRADE_SUCCESS".equals(tradeStatus)) {
                orderService.pass(outTradeNo);
                log.info("订单支付成功，已通过验证: {}", outTradeNo);
                return "success";
            }

            return "success";

        } catch (Exception e) {
            log.error("回调验证失败", e);
            return "fail";
        }
    }

    /**
     * 生成易支付签名
     * 算法：过滤空值和sign/sign_type -> 排序 -> 拼接 -> MD5(拼接字符串+key)
     */
    private String generateEpaySign(Map<String, String> params, String key) {
        // 过滤参数
        List<String> keys = new ArrayList<>();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            String k = entry.getKey();
            String v = entry.getValue();
            if (v != null && !v.isEmpty() && 
                !k.equals("sign") && !k.equals("sign_type")) {
                keys.add(k);
            }
        }

        // 排序
        Collections.sort(keys);

        // 拼接
        StringBuilder sb = new StringBuilder();
        for (String k : keys) {
            sb.append(k).append("=").append(params.get(k)).append("&");
        }
        // 移除最后的 &
        if (sb.length() > 0) {
            sb.setLength(sb.length() - 1);
        }

        // MD5(拼接字符串+key)
        String signStr = sb.toString() + key;
        log.debug("签名字符串: {}", signStr);
        
        return SecureUtil.md5(signStr);
    }

    /**
     * 获取支付方式是否使用本地二维码配置
     */
    private Boolean getUseLocalQrCode(String payType) {
        String configKey = "payment.method." + payType + ".use_local_qr";
        String value = systemConfigService.getConfigValue(configKey);
        return value != null && "true".equalsIgnoreCase(value);
    }

    /**
     * 生成订单访问令牌
     */
    private String generateToken(String orderId) {
        return SecureUtil.md5(orderId + apiSecret).substring(0, 16);
    }

    /**
     * 构建重定向HTML
     */
    private String buildRedirectHtml(String url) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<meta charset='UTF-8'>" +
                "<title>跳转支付页面</title>" +
                "</head>" +
                "<body>" +
                "<script>window.location.href='" + url + "';</script>" +
                "<p>正在跳转到支付页面...</p>" +
                "<p>如果没有自动跳转，请<a href='" + url + "'>点击这里</a></p>" +
                "</body>" +
                "</html>";
    }

    /**
     * 构建错误响应
     */
    private String buildErrorResponse(String message) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<meta charset='UTF-8'>" +
                "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                "<title>充值提示</title>" +
                "<style>" +
                "* { margin: 0; padding: 0; box-sizing: border-box; }" +
                "body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%); min-height: 100vh; display: flex; align-items: center; justify-content: center; padding: 20px; }" +
                ".container { background: rgba(255, 255, 255, 0.05); backdrop-filter: blur(10px); border: 1px solid rgba(255, 255, 255, 0.1); border-radius: 20px; padding: 40px 30px; max-width: 500px; width: 100%; box-shadow: 0 20px 60px rgba(0,0,0,0.5); text-align: center; }" +
                ".icon { width: 80px; height: 80px; margin: 0 auto 20px; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 40px; }" +
                "h1 { color: #fff; font-size: 24px; margin-bottom: 15px; font-weight: 600; }" +
                ".message { color: rgba(255, 255, 255, 0.8); font-size: 16px; line-height: 1.6; margin-bottom: 30px; }" +
                ".tip { background: rgba(255, 255, 255, 0.05); border-left: 4px solid #667eea; padding: 15px; border-radius: 8px; text-align: left; margin-bottom: 25px; }" +
                ".tip-title { color: #667eea; font-weight: 600; margin-bottom: 8px; font-size: 14px; }" +
                ".tip-content { color: rgba(255, 255, 255, 0.7); font-size: 14px; line-height: 1.5; }" +
                ".btn { display: inline-block; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 12px 40px; border-radius: 25px; text-decoration: none; font-weight: 500; transition: transform 0.2s, box-shadow 0.2s; cursor: pointer; border: none; font-size: 16px; }" +
                ".btn:hover { transform: translateY(-2px); box-shadow: 0 10px 20px rgba(102, 126, 234, 0.4); }" +
                ".btn:active { transform: translateY(0); }" +
                ".note { color: rgba(255, 255, 255, 0.5); font-size: 12px; margin-top: 15px; }" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div class='container'>" +
                "<div class='icon'>💡</div>" +
                "<h1>温馨提示</h1>" +
                "<div class='message'>" + message + "</div>" +
                "<div class='tip'>" +
                "<div class='tip-title'>💰 支持的充值额度</div>" +
                "<div class='tip-content'>当前支持：10元、20元、50元、100元、300元、500元<br>请在充值页面选择固定额度进行充值</div>" +
                "</div>" +
                "<button class='btn' onclick='window.close()'>关闭页面</button>" +
                "<div class='note'>未生成支付订单，可直接关闭此页面</div>" +
                "</div>" +
                "</body>" +
                "</html>";
    }
}
