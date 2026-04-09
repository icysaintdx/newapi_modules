package com.wendao.common;

public class RedisKeyConstant {

    private RedisKeyConstant() {
    }

    public static final String RATE_LIMIT_IP_PREFIX = "rate_limit:ip:";

    public static final String ORDER_TOKEN_PREFIX = "order:token:";

    public static final String RATE_LIMIT_DMF_IP_PREFIX = "rate_limit:dmf:ip:";

    public static final String ALIPAY_DMF_CLOSE_PREFIX = "alipay:dmf:close:";

    public static String getRateLimitIpKey(String ip) {
        return RATE_LIMIT_IP_PREFIX + ip;
    }

    public static String getOrderTokenKey(String orderId) {
        return ORDER_TOKEN_PREFIX + orderId;
    }

    public static String getRateLimitDmfIpKey(String ip) {
        return RATE_LIMIT_DMF_IP_PREFIX + ip;
    }

    public static String getAlipayDmfCloseKey() {
        return ALIPAY_DMF_CLOSE_PREFIX + "enabled";
    }

    public static String getAlipayDmfCloseReasonKey() {
        return ALIPAY_DMF_CLOSE_PREFIX + "reason";
    }

}
