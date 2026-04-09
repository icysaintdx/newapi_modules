package com.wendao.common.utils;

import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.hutool.log.StaticLog;
import org.springframework.stereotype.Component;

/**
 * Server酱推送工具类
 */
@Component
public class ServerChanUtils {

    private static final String SERVER_CHAN_URL = "https://sctapi.ftqq.com/{sendkey}.send";

    /**
     * 发送Server酱推送
     * @param sendkey Server酱SendKey
     * @param title 标题
     * @param desp 内容(支持Markdown)
     * @return 是否成功
     */
    public boolean sendPush(String sendkey, String title, String desp) {
        try {
            String url = SERVER_CHAN_URL.replace("{sendkey}", sendkey);

            // Server酱使用form表单提交
            String response = HttpRequest.post(url)
                    .form("title", title)
                    .form("desp", desp)
                    .timeout(10000)
                    .execute()
                    .body();

            StaticLog.info("Server酱推送响应: {}", response);

            // 根据官方API响应格式判断：成功返回 {"code":0,...} 或 {"data":{"pushid":...}}
            JSONObject result = JSONUtil.parseObj(response);
            // Server酱成功返回code=0，或者有data.pushid字段
            return result.containsKey("code") && result.getInt("code") == 0
                || result.containsKey("data") && result.getJSONObject("data").containsKey("pushid");
        } catch (Exception e) {
            StaticLog.error("Server酱推送失败", e);
            return false;
        }
    }
}
