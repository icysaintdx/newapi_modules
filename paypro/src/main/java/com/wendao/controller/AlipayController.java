package com.wendao.controller;

import cn.hutool.log.StaticLog;
import com.wendao.common.RedisKeyConstant;
import com.wendao.entity.Order;
import com.wendao.config.PayProConfig;
import com.wendao.model.ResponseVO;
import com.wendao.common.utils.IpInfoUtils;
import com.wendao.common.utils.StringUtils;
import com.wendao.service.OrderService;
import com.wendao.service.SystemConfigService;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePrecreateRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradePrecreateResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author lld
 */
@Controller
public class AlipayController {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private OrderService orderService;

    @Autowired
    PayProConfig payProConfig;

    @Autowired
    private SystemConfigService systemConfigService;

    /**
     * 生成二维码
     * @param pay
     * @param request
     * @return
     * @throws AlipayApiException
     */
    @RequestMapping(value = "/alipay/precreate",method = RequestMethod.POST)
    @ResponseBody
    public ResponseVO<Object> getOrderState(@RequestBody Order pay, HttpServletRequest request) throws AlipayApiException {

        String closeDmfKey = RedisKeyConstant.getAlipayDmfCloseKey();
        String closeDmfReasonKey = RedisKeyConstant.getAlipayDmfCloseReasonKey();
        String isOpenDMF = redisTemplate.opsForValue().get(closeDmfKey);
        String dmfReason = redisTemplate.opsForValue().get(closeDmfReasonKey);
        String msg = "";
        if(StringUtils.isNotBlank(isOpenDMF)){
            msg = dmfReason + "如有疑问请进行反馈";
            return ResponseVO.errorResponse(msg);
        }
        //防炸库验证
        String ip = IpInfoUtils.getIpAddr(request);
        String dmfIpKey = RedisKeyConstant.getRateLimitDmfIpKey(ip);

        orderService.addOrder(pay);
        //记录缓存
        redisTemplate.opsForValue().set(dmfIpKey, "added", payProConfig.getRateLimit().getIpExpire(), TimeUnit.SECONDS);

        // 从数据库读取支付宝配置（优先使用数据库配置，如果不存在则使用application.yml配置）
        String appId = systemConfigService.getConfigValue("alipay.dmf.app.id", payProConfig.getAlipayDmfAppId());
        String appPrivateKey = systemConfigService.getConfigValue("alipay.dmf.app.private.key", payProConfig.getAlipayDmfAppPrivateKey());
        String alipayPublicKey = systemConfigService.getConfigValue("alipay.dmf.public.key", payProConfig.getAlipayDmfPublicKey());
        String subject = systemConfigService.getConfigValue("alipay.dmf.subject", payProConfig.getAlipayDmfSubject());
        String site = systemConfigService.getConfigValue("site", payProConfig.getSite());

        StaticLog.info("支付宝当面付配置 - AppID: {}, Subject: {}, Site: {}", appId, subject, site);

        AlipayClient alipayClient = new DefaultAlipayClient("https://openapi.alipay.com/gateway.do",
                appId, appPrivateKey, "json", "GBK",
                alipayPublicKey, "RSA2");
        AlipayTradePrecreateRequest r = new AlipayTradePrecreateRequest();
        r.setBizContent("{" +
                "\"out_trade_no\":\""+pay.getId()+"\"," +
                "\"total_amount\":"+pay.getMoney()+"," +
                "\"subject\":" + "\"" + subject + "\"" +
                "  }");
        // 设置通知回调链接
        r.setNotifyUrl(site + "/alipay/notify");
        AlipayTradePrecreateResponse response = alipayClient.execute(r);
        if(!response.isSuccess()){
            return ResponseVO.errorResponse("调用支付宝接口生成二维码失败，请向作者反馈");
        }
        Map<String, Object> result = new HashMap<>(16);
        result.put("id", pay.getId());
        result.put("qrCode", response.getQrCode());
        result.put("money",pay.getMoney());
        result.put("payType",pay.getPayType());
        return ResponseVO.successResponse(result);
    }

    /**
     * 查询支付结果
     * @param out_trade_no
     * @return
     * @throws AlipayApiException
     */
    @RequestMapping(value = "/alipay/query/{out_trade_no}",method = RequestMethod.GET)
    @ResponseBody
    public ResponseVO<Object> queryOrderState(@PathVariable String out_trade_no) throws AlipayApiException {

        // 从数据库读取支付宝配置
        String appId = systemConfigService.getConfigValue("alipay.dmf.app.id", payProConfig.getAlipayDmfAppId());
        String appPrivateKey = systemConfigService.getConfigValue("alipay.dmf.app.private.key", payProConfig.getAlipayDmfAppPrivateKey());
        String alipayPublicKey = systemConfigService.getConfigValue("alipay.dmf.public.key", payProConfig.getAlipayDmfPublicKey());

        AlipayClient alipayClient = new DefaultAlipayClient("https://openapi.alipay.com/gateway.do",
                appId, appPrivateKey, "json",
                "GBK", alipayPublicKey, "RSA2");
        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        request.setBizContent("{" +
                "\"out_trade_no\":\""+out_trade_no+"\"" +
                "  }");
        AlipayTradeQueryResponse response = alipayClient.execute(request);
        if(response != null&&response.isSuccess()&&"TRADE_SUCCESS".equals(response.getTradeStatus())){
            orderService.pass(out_trade_no);
            return ResponseVO.successResponse(1);
        }else{
            return ResponseVO.successResponse(0);
        }
    }

    /**
     * 支付宝通知回调（已添加签名验证）
     * @return
     */
    @RequestMapping(value = "/alipay/notify")
    @ResponseBody
    public String notify(HttpServletRequest request) {
        try {
            // 获取所有请求参数
            Map<String, String> params = new HashMap<>();
            Map<String, String[]> requestParams = request.getParameterMap();
            for (String name : requestParams.keySet()) {
                String[] values = requestParams.get(name);
                String valueStr = "";
                for (int i = 0; i < values.length; i++) {
                    valueStr = (i == values.length - 1) ? valueStr + values[i] : valueStr + values[i] + ",";
                }
                params.put(name, valueStr);
            }
            
            StaticLog.info("收到支付宝回调，订单号: {}", params.get("out_trade_no"));

            // 从数据库读取支付宝公钥
            String alipayPublicKey = systemConfigService.getConfigValue("alipay.dmf.public.key", payProConfig.getAlipayDmfPublicKey());

            // 验证支付宝签名
            boolean signVerified = AlipaySignature.rsaCheckV1(
                params,
                alipayPublicKey,
                "UTF-8",
                "RSA2"
            );
            
            if (!signVerified) {
                StaticLog.error("支付宝回调签名验证失败！订单号: {}", params.get("out_trade_no"));
                return "fail";
            }
            
            String tradeStatus = params.get("trade_status");
            String outTradeNo = params.get("out_trade_no");
            
            // 签名验证通过，处理订单
            if("TRADE_SUCCESS".equals(tradeStatus)){
                orderService.pass(outTradeNo);
                StaticLog.info("订单支付成功: {}", outTradeNo);
            }
            
            return "success";
            
        } catch (AlipayApiException e) {
            StaticLog.error("支付宝回调签名验证异常", e);
            return "fail";
        } catch (Exception e) {
            StaticLog.error("处理支付宝回调异常", e);
            return "fail";
        }
    }
}
