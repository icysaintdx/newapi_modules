package com.wendao.common.utils;

import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.hutool.log.StaticLog;
import org.springframework.stereotype.Component;

/**
 * PushPlus推送工具类
 */
@Component
public class PushPlusUtils {

    private static final String PUSH_PLUS_URL = "http://www.pushplus.plus/send";

    /**
     * 发送PushPlus推送
     * @param token PushPlus Token
     * @param title 标题
     * @param content 内容(支持HTML)
     * @param template 模板类型(html/txt/json/markdown)
     * @return 是否成功
     */
    public boolean sendPush(String token, String title, String content, String template) {
        try {
            JSONObject params = new JSONObject();
            params.put("token", token);
            params.put("title", title);
            params.put("content", content);
            params.put("template", template);

            String response = HttpRequest.post(PUSH_PLUS_URL)
                    .body(params.toString())
                    .header("Content-Type", "application/json")
                    .timeout(10000)
                    .execute()
                    .body();

            StaticLog.info("PushPlus推送响应: {}", response);

            // 根据官方API响应格式判断：成功返回 {"code":200,...}
            JSONObject result = JSONUtil.parseObj(response);
            return result.getInt("code") == 200;
        } catch (Exception e) {
            StaticLog.error("PushPlus推送失败", e);
            return false;
        }
    }

    /**
     * 发送HTML格式推送
     */
    public boolean sendHtmlPush(String token, String title, String htmlContent) {
        return sendPush(token, title, htmlContent, "html");
    }
}
