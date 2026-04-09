package com.wendao.job;

import cn.hutool.log.StaticLog;
import com.wendao.entity.PayChatMessage;
import com.wendao.mapper.AutoPassPayMapper;
import com.wendao.mapper.PayChatMessageMapper;
import com.wendao.service.OrderService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * @description:
 * @author: lld
 * @version: 1.0
 */
@Component
public class AutoRob {

    private Set<Long> ingId = new HashSet<>();

    @Autowired
    OrderService orderService;

    @Autowired
    AutoPassPayMapper autoPassPayMapper;

    @Autowired
    StringRedisTemplate stringRedisTemplate;

    @Autowired
    PayChatMessageMapper payChatMessageMapper;

//    @Scheduled(fixedRate = 10000)
//    public void auto(){
//
//        String redisKey = "pay:auto_pass_time";
//        String lastTimeStr = stringRedisTemplate.opsForValue().get(redisKey);
//        if (StringUtils.isBlank(lastTimeStr)){
//            lastTimeStr = "2025-01-01 00:00:00";
//        }
//        Date processDate = DateUtil.parse(lastTimeStr);
//
//        // 获取日期字符串 (格式为 yyyy-MM-dd)
//        String yesterday = DateUtil.yesterday().toDateStr();
//        String today = DateUtil.today();
//
//        // 拼接成你需要的范围字符串
//        String timeRange = yesterday + " ~ " + today;
//        // 1. 请求接口
//        String url = "http://127.0.0.1:5030/api/v1/bizlog?time="+timeRange+"&format=json&talker=微信支付1&keyword=个人收款码到账";
//        String result = HttpUtil.get(url);
//
//        // 2. 容错：判断 API 是否返回了有效内容
//        if (StrUtil.isBlank(result)) {
//            System.out.println("警告：API 返回内容为空");
//            return;
//        }
//
//        // 3. 解析 JSON (此时 DTO 里的 time 会自动转为 Date 对象)
//        List<WeChatMsgDTO> list;
//        try {
//            list = JSONUtil.parseArray(result).toList(WeChatMsgDTO.class);
//        } catch (Exception e) {
//            System.err.println("JSON 解析失败: " + e.getMessage());
//            return;
//        }
//
//        if (CollUtil.isEmpty(list)) {
//            System.out.println("没有查询到任何消息");
//            return;
//        }
//
//        int xSeconds = 60; // 这里的 x 按需调整
//        Date now = new Date();
//
//        List<WeChatMsgDTO> filteredList = list.stream()
//                .filter(msg -> {
//                    if (msg.getTime() == null) return false;
//
//                    // 核心逻辑：消息时间必须“晚于” Redis 记录的时间
//                    // 使用 after 方法进行比较
//                    return msg.getTime().after(processDate);
//                })
//                .sorted(Comparator.comparing(WeChatMsgDTO::getTime)) // 建议按时间升序排序，确保处理顺序
//                .collect(Collectors.toList());
//        filteredList.forEach(dto -> {
//            payService.autoPass(dto);
//        });
//
//        Date newestTime = filteredList.get(filteredList.size() - 1).getTime();
//        stringRedisTemplate.opsForValue().set(redisKey, DateUtil.formatDateTime(newestTime));
//    }

    @Scheduled(fixedRate = 10000)
    public void auto() {
        QueryWrapper<PayChatMessage> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(PayChatMessage::getProcessStatus, 0);
        List<PayChatMessage> payChatMessages = payChatMessageMapper.selectList(queryWrapper);
        for (PayChatMessage payChatMessage : payChatMessages) {
            try {
                orderService.autoPass(payChatMessage);
            }catch (Exception e){
                StaticLog.error("处理自动通过时出现异常",e);
                continue;
            }
        }
    }
}
