package com.wendao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author: Chackylee
 * @description:
 * @create: 2026-01-28 16:00
 **/
@Data
@TableName("t_pay_chat_message")
public class PayChatMessage {
    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    /**
     * 序列号
     */
    private Long seq;

    /**
     * 消息时间
     */
    private Date time;

    /**
     * 聊天对象
     */
    private String talker;

    /**
     * 聊天对象名称
     */
    private String talkerName;

    /**
     * 是否为聊天室
     * 0-否，1-是
     */
    private Integer isChatRoom;

    /**
     * 发送者
     */
    private String sender;

    /**
     * 发送者名称
     */
    private String senderName;

    /**
     * 是否为自己发送
     * 0-否，1-是
     */
    private Integer isSelf;

    /**
     * 消息类型
     */
    private Integer type;

    /**
     * 消息子类型
     */
    private Integer subType;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 附加内容
     */
    private String contents;

    /**
     * 平台类型
     */
    private String platformType;

    /**
     * 处理状态
     * 可根据业务定义具体状态值  0未处理 1已处理 2处理失败
     */
    private Integer processStatus;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 支付ID
     */
    private String orderId;

    public String extractAmount() {
        if (content == null) return null;
        // 使用之前定义的正则：匹配“付款方备注”之后到换行符之前的内容
        Pattern pattern = Pattern.compile("¥?(\\d+(?:\\.\\d{1,2})?)");
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            String amount = matcher.group(1);
            System.out.println("提取金额: " + amount);
        }
        return "";
    }
}
