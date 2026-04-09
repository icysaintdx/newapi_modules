package com.wendao.service.impl;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import com.wendao.entity.AutoPassPay;
import com.wendao.entity.Order;
import com.wendao.entity.PayChatMessage;
import com.wendao.entity.RedemptionCode;
import com.wendao.entity.ExtractCode;
import com.wendao.exception.ApiException;
import com.wendao.mapper.AutoPassPayMapper;
import com.wendao.mapper.OrderMapper;
import com.wendao.mapper.PayChatMessageMapper;
import com.wendao.mapper.ProductMapper;
import com.wendao.dto.MsgContentsDTO;
import com.wendao.dto.WeChatMsgDTO;
import com.wendao.enums.OrderStatesEnum;
import com.wendao.model.req.GetOrderListReq;
import com.wendao.model.req.OpenApiOrderReq;
import com.wendao.model.resp.AddOrderResp;
import com.wendao.model.resp.CountResp;
import com.wendao.model.resp.OpenApiOrderResp;
import com.wendao.service.OrderService;
import com.wendao.service.SystemConfigService;
import com.wendao.entity.AmountMapping;
import com.wendao.mapper.AmountMappingMapper;
import cn.hutool.core.date.DateUtil;
import java.util.Collections;
import java.util.stream.Collectors;
import cn.hutool.core.lang.Snowflake;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.wendao.config.PayProConfig;
import com.wendao.entity.Product;
import com.wendao.model.ResponseVO;
import com.wendao.common.RedisKeyConstant;
import com.wendao.common.utils.*;
import com.wendao.model.req.OrderReq;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wendao.utils.OpenApiSignUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.Arrays;

/**
 * @author lld
 */
@Service
public class OrderServiceImpl implements OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    StringRedisTemplate redisTemplate;

    @Autowired
    OrderService thisService;

    @Autowired
    private EmailUtils emailUtils;

    @Value("${GameUrl}")
    private String gameUrl;

    @Autowired
    ProductMapper productMapper;

    @Autowired
    PayProConfig payProConfig;

    @Autowired
    AutoPassPayMapper autoPassPayMapper;

    @Autowired
    Snowflake snowflake;

    @Autowired
    PayChatMessageMapper payChatMessageMapper;

    @Autowired
    OpenApiSignUtil openApiSignUtil;

    @Autowired
    com.wendao.service.RedemptionCodeService redemptionCodeService;

    @Autowired
    com.wendao.service.ExtractCodeService extractCodeService;

    @Autowired
    private ResourceLoader resourceLoader;

    @Autowired
    private SystemConfigService systemConfigService;

    @Autowired
    private AmountMappingMapper amountMappingMapper;

    @Autowired
    private com.wendao.service.MultiChannelNotificationService multiChannelNotificationService;

    @Override
    public Order getOrderById(String id) {
        Order byId = orderMapper.selectById(id);
        byId.setTime(StringUtils.getTimeStamp(byId.getCreateTime()));
        return byId;
    }

    @Override
    public int addOrder(Order pay) {
        // 只在 ID 为空时生成新的 UUID
        if (pay.getId() == null || pay.getId().isEmpty()) {
            pay.setId(UUID.randomUUID().toString().replace("-", ""));
        }
        // 总是生成 payNum（支付标识号）
        if (pay.getPayNum() == null || pay.getPayNum().isEmpty()) {
            pay.setPayNum(StringUtils.getRandomNum());
        }
        pay.setCreateTime(new Date());
        pay.setState(OrderStatesEnum.WAIT_PAY.getState());
        orderMapper.insert(pay);
        return 1;
    }

    @Override
    public int updateOrder(Order pay) {
        pay.setUpdateTime(new Date());
        orderMapper.updateById(pay);
        return 1;
    }

    @Override
    public int changeOrderState(String id, Integer state) {

        Order pay = getOrderById(id);
        pay.setState(state);
        pay.setUpdateTime(new Date());
        orderMapper.updateById(pay);
        return 1;
    }

    @Override
    public int delOrder(String id) {
        orderMapper.deleteById(id);
        return 1;
    }

    @Override
    public CountResp statistic(Integer type, String start, String end) {

        CountResp count = new CountResp();
        if (type == -1) {
            // 总
            count.setAmount(orderMapper.countAllMoney());
            count.setAlipayDmf(orderMapper.countAllMoneyByType("alipay_dmf"));
            count.setWeixin(orderMapper.countAllMoneyByType("wechat"));
            count.setAlipay(orderMapper.countAllMoneyByType("alipay"));
            return count;
        }
        Date startDate = null, endDate = null;
        if (type == 0) {
            // 今天
            startDate = DateUtils.getDayBegin();
            endDate = DateUtils.getDayEnd();
        }
        if (type == 6) {
            // 昨天
            startDate = DateUtils.getBeginDayOfYesterday();
            endDate = DateUtils.getEndDayOfYesterDay();
        } else if (type == 1) {
            // 本周
            startDate = DateUtils.getBeginDayOfWeek();
            endDate = DateUtils.getEndDayOfWeek();
        } else if (type == 2) {
            // 本月
            startDate = DateUtils.getBeginDayOfMonth();
            endDate = DateUtils.getEndDayOfMonth();
        } else if (type == 3) {
            // 本年
            startDate = DateUtils.getBeginDayOfYear();
            endDate = DateUtils.getEndDayOfYear();
        } else if (type == 4) {
            // 上周
            startDate = DateUtils.getBeginDayOfLastWeek();
            endDate = DateUtils.getEndDayOfLastWeek();
        } else if (type == 5) {
            // 上个月
            startDate = DateUtils.getBeginDayOfLastMonth();
            endDate = DateUtils.getEndDayOfLastMonth();
        } else if (type == -2) {
            // 自定义
            startDate = DateUtils.parseStartDate(start);
            endDate = DateUtils.parseEndDate(end);
        }
        count.setAmount(orderMapper.countMoney(startDate, endDate));
        count.setAlipayDmf(orderMapper.countMoneyByType("alipay_dmf", startDate, endDate));
        count.setWeixin(orderMapper.countMoneyByType("wechat", startDate, endDate));
        count.setAlipay(orderMapper.countMoneyByType("alipay", startDate, endDate));
        return count;
    }

    @Override
    @Transactional
    public ResponseVO<AddOrderResp> addOrder(OrderReq req, HttpServletRequest request) {

        if(StringUtils.isBlank(String.valueOf(req.getMoney()))){
            return ResponseVO.errorResponse("请填写完整信息和正确金额");
        }

        if (req.getMoney() == null || req.getMoney().compareTo(new BigDecimal("0")) <= 0) {
            return ResponseVO.errorResponse("金额必须大于0");
        }

        if (req.getMoney().compareTo(new BigDecimal("100000")) > 0) {
            return ResponseVO.errorResponse("金额超出限制，单笔订单不能超过100000元");
        }

        String ip = IpInfoUtils.getIpAddr(request);

        String ipKey = RedisKeyConstant.getRateLimitIpKey(ip);
        if (redisTemplate.opsForValue().get(ipKey) != null) {
            return ResponseVO.errorResponse("提交过于频繁，请稍后再试");
        }

        // 查询金额映射，获取实际支付金额
        BigDecimal chargeAmount = req.getMoney();
        List<AmountMapping> mappings = amountMappingMapper.selectList(
            new LambdaQueryWrapper<AmountMapping>()
                .eq(AmountMapping::getPayType, req.getPayType())
                .eq(AmountMapping::getChargeAmount, chargeAmount)
                .eq(AmountMapping::getEnabled, true)
                .orderByAsc(AmountMapping::getPriority)
        );

        // 随机选择一个实际支付金额（声明为final，在整个方法中可用）
        final BigDecimal actualAmount;
        if (!mappings.isEmpty()) {
            actualAmount = mappings.get(new Random().nextInt(mappings.size())).getActualAmount();
        } else {
            actualAmount = chargeAmount;
        }

        Order entity = new Order();
        BeanUtils.copyProperties(req, entity);

        // 添加调试日志
        log.info("订单创建开始: payType={}, amount={}, custom={}", req.getPayType(), req.getMoney(), req.getCustom());

        try {
            if (req.getCustom() != null && !req.getCustom()) {
                // 非自定义金额，获取可用的二维码序号
                List<Integer> availableQrNumbers = getAvailableQrNumbers(req.getPayType(), actualAmount);

                if (!availableQrNumbers.isEmpty()) {
                    // 从可用序号中随机选择一个
                    int i = availableQrNumbers.get(new Random().nextInt(availableQrNumbers.size()));
                    entity.setPayQrNum(i);
                } else {
                    // 如果没有找到可用的二维码，返回错误
                    log.error("没有找到对应金额的二维码: payType={}, amount={}", req.getPayType(), actualAmount);
                    return ResponseVO.errorResponse("不支持自定义金额充值，请选择固定额度充值");
                }
            } else if (req.getCustom() != null && req.getCustom()) {
                // 自定义金额，检查是否有对应的二维码
                log.info("检查自定义金额二维码: payType={}, amount={}, custom={}", req.getPayType(), actualAmount, req.getCustom());
                List<Integer> availableQrNumbers = getAvailableQrNumbers(req.getPayType(), actualAmount);
                log.info("找到的二维码序号列表: {}", availableQrNumbers);
                if (availableQrNumbers.isEmpty()) {
                    log.error("自定义金额没有对应的二维码: payType={}, amount={}", req.getPayType(), actualAmount);
                    return ResponseVO.errorResponse("不支持自定义金额充值，请选择固定额度充值");
                }
                // 随机选择一个序号
                int i = availableQrNumbers.get(new Random().nextInt(availableQrNumbers.size()));
                entity.setPayQrNum(i);
            } else {
                // 没有指定 custom 参数，默认为非自定义金额
                log.info("未指定custom参数或custom=false: payType={}, amount={}, custom={}", req.getPayType(), actualAmount, req.getCustom());
            }

            entity.setPayNum(StringUtils.getRandomNum());

            thisService.addOrder(entity);
        } catch (Exception e) {
            log.error(e.toString());
            return ResponseVO.errorResponse("添加捐赠支付订单失败");
        }
        //记录缓存
        redisTemplate.opsForValue().set(ipKey, "added", payProConfig.getRateLimit().getIpExpire(), TimeUnit.SECONDS);

        //给管理员发送审核邮件和PushPlus推送
        String tokenAdmin = UUID.randomUUID().toString();
        String orderTokenKey = RedisKeyConstant.getOrderTokenKey(entity.getId());
        redisTemplate.opsForValue().set(orderTokenKey, tokenAdmin, payProConfig.getToken().getExpire(), TimeUnit.DAYS);
        entity = getAdminUrl(entity, entity.getId(), tokenAdmin, payProConfig.getToken().getValue());

        // 发送多渠道通知（PushPlus > Server酱 > 邮件）
        try {
            multiChannelNotificationService.sendOrderNotification(entity, tokenAdmin, payProConfig.getToken().getValue());
        } catch (Exception e) {
            log.error("通知发送失败: {}", e.getMessage());
        }

        AddOrderResp addPayResp = new AddOrderResp();
        addPayResp.setId(entity.getId());
        addPayResp.setPayNum(entity.getPayNum());
        addPayResp.setPayType(entity.getPayType());
        addPayResp.setMoney(entity.getMoney());
        addPayResp.setCustom(entity.getCustom());
        addPayResp.setPayQrNum(entity.getPayQrNum());
        addPayResp.setActualAmount(actualAmount);  // 设置实际支付金额
        return ResponseVO.successResponse(addPayResp);
    }

    /**
     * 处理充值业务,直接调用游戏的接口。订单状态在游戏的地方修改
     * 已添加防重复处理和详细日志
     */
    @Override
    @Transactional
    public synchronized int pass(String id) {
        Order pay = orderMapper.selectById(id);
        
        // 检查订单是否存在
        if (pay == null) {
            log.error("订单不存在: {}", id);
            throw new RuntimeException("订单不存在");
        }
        
        // 记录订单当前状态
        log.info("处理订单支付: orderId={}, currentState={}, orderSource={}", 
                id, pay.getState(), pay.getOrderSource());

        // 检查订单状态，防止重复处理
        if (pay.getState().equals(OrderStatesEnum.FAIL_PAY.getState())) {
            log.warn("订单已失败，无法通过审核: {}", id);
            throw new RuntimeException("订单已失败，无法通过审核");
        }
        if (pay.getState().equals(OrderStatesEnum.SUCCESS_PAY.getState())) {
            log.warn("订单已完成，拒绝重复处理: {}", id);
            return 1;  // 已处理过，直接返回成功，避免抛异常导致回调失败
        }

        if (pay.getOrderSource().equals("PRODUCT") && pay.getProductId() != null) {
            Product product = productMapper.selectById(pay.getProductId());
            if (product.getType().equals("GAME")) {

            } else if (product.getType().equals("CODE")) {
                // 安全发卡流程：分配兑换码 -> 生成提取码 -> 可选发送邮件 -> 确认使用
                RedemptionCode code = redemptionCodeService.allocateCodeForOrder(
                        id, product.getId(), pay.getEmail());

                if (code == null) {
                    log.error("兑换码库存不足，订单处理失败: orderId={}, productId={}", id, product.getId());
                    throw new RuntimeException("兑换码库存不足，无法完成订单");
                }

                // 生成提取码（6位数字，24小时有效）
                String extractCode = extractCodeService.generateExtractCode(
                        id, code.getId(), code.getCode());

                // 将提取码存储到订单中，用于支付成功页面显示
                pay.setDownloadUrl(extractCode);

                try {
                    // 如果用户提供了邮箱，发送包含提取码的邮件（可选）
                    if (pay.getEmail() != null && !pay.getEmail().trim().isEmpty()) {
                        emailUtils.sendTemplateMail(
                                payProConfig.getEmail().getSender(),
                                pay.getEmail(),
                                "【Pay个人收款支付系统】支付成功通知（附提取码）",
                                "order-success",
                                pay);
                        log.info("已发送提取码邮件: orderId={}, email={}", id, pay.getEmail());
                    }

                    // 确认使用兑换码（无论是否发送邮件都要确认）
                    boolean confirmed = ((RedemptionCodeServiceImpl) redemptionCodeService)
                            .confirmUseCode(code.getLockToken(), id, pay.getEmail(),
                                    code.getCode(), code.getId());

                    if (!confirmed) {
                        log.error("确认使用兑换码失败: orderId={}, codeId={}", id, code.getId());
                        throw new RuntimeException("确认使用兑换码失败");
                    }

                    // 更新订单状态为成功
                    pay.setState(OrderStatesEnum.SUCCESS_PAY.getState());
                    orderMapper.updateById(pay);
                    log.info("订单支付成功，已生成提取码: orderId={}, extractCode={}", id, extractCode);

                } catch (Exception e) {
                    log.error("处理订单失败: orderId={}, error={}", id, e.getMessage());
                    // 处理失败，兑换码保持锁定状态，等待定时任务释放
                    throw new RuntimeException("处理订单失败: " + e.getMessage());
                }
            } else if (product.getType().equals("CODE_PAYPRO")) {
                pay.setDownloadUrl(payProConfig.getDownloadUrl());
                emailUtils.sendTemplateMail(payProConfig.getEmail().getSender(), pay.getEmail(), "【Pay个人收款支付系统】支付成功通知（附下载链接）",
                        "order-success", pay);
                pay.setState(OrderStatesEnum.SUCCESS_PAY.getState());
                orderMapper.updateById(pay);
                log.info("订单支付成功，已发送邮件: {}", id);
            }
        } else if (pay.getOrderSource().equals("OPENAPI")) {
            callbackFaka(pay.getNotifyUrl(),pay.getId(),pay.getMoney(),pay.getPayNum());
            log.info("订单支付成功，已回调通知: {}", id);
        }
        return 1;
    }

    @Override
    public IPage<Order> list(GetOrderListReq req) {

        LambdaQueryWrapper<Order> wrapper = Wrappers.lambdaQuery();
        wrapper.in(Order::getState, req.getStates());
        if (StringUtils.isNotBlank(req.getOrderBy())) {

            if (req.getOrderBy().equals("createTime")) {
                if (req.getOrder().toLowerCase().equals("desc")) {
                    wrapper.orderByDesc(Order::getCreateTime);
                } else {
                    wrapper.orderByAsc(Order::getCreateTime);
                }
            }

            if (req.getOrderBy().equals("money")) {
                if (req.getOrder().toLowerCase().equals("desc")) {
                    wrapper.orderByDesc(Order::getMoney);
                } else {
                    wrapper.orderByAsc(Order::getMoney);
                }
            }
        }

        if (StringUtils.isNotBlank(req.getKeyword())) {
            //字符串类型的处理，统一全部like查询
            wrapper.like(Order::getEmail, req.getKeyword());
            wrapper.or().like(Order::getNickName, req.getKeyword());
        }
        Page<Order> page = new Page<>(req.getPageIndex(), req.getPageSize());
        Page<Order> payPage = orderMapper.selectPage(page, wrapper);

        for (Order record : payPage.getRecords()) {
            // 屏蔽隐私数据
            record.setId("");
            record.setEmail("");
            record.setPayNum(null);
            record.setMobile(null);
            record.setCustom(null);
            record.setDevice(null);
        }
        return payPage;
    }

    @Override
    public Order getByPayNum(String desc, Date time) {
        // 查询最近7天的订单（包括已过期的）
        // 原因：收款码永久有效，用户可能延迟支付
        Date sevenDaysAgo = DateUtil.offsetDay(time, -7);

        QueryWrapper<Order> payQueryWrapper = new QueryWrapper<>();
        payQueryWrapper.lambda()
                .eq(Order::getPayNum, desc)
                // 查询最近7天的订单
                .between(Order::getCreateTime, sevenDaysAgo, time)
                // 包括待支付和已过期的订单
                .in(Order::getState, Arrays.asList(
                    OrderStatesEnum.WAIT_PAY.getState(),
                    OrderStatesEnum.EXPIRED.getState()
                ))
                // 最新的订单优先
                .orderByDesc(Order::getCreateTime)
                .last("LIMIT 1");

        return orderMapper.selectOne(payQueryWrapper);
    }

    @Override
    @Transactional
    public void autoPass(PayChatMessage dto) {

        String desc = null;
        if (dto.getPlatformType().equals("weixin")) {
            MsgContentsDTO bean = JSONUtil.toBean(dto.getContents(), MsgContentsDTO.class);
            desc = bean.extractRemark();
            if (StringUtils.isBlank(desc)) {
                dto.setProcessStatus(2);
                payChatMessageMapper.updateById(dto);
                log.info("处理:{}.没有提取到备注" + JSONUtil.toJsonStr(dto));
                return;
            }
        }

        Order byPayNum = null;
        if (StringUtils.isNotBlank(desc)) {
            /** 找到匹配的订单*/
            byPayNum = thisService.getByPayNum(desc, dto.getTime());
            if (byPayNum != null) {
                String s = dto.extractAmount();
                BigDecimal money = byPayNum.getMoney();
                // 判断提取的金额是否小于订单金额
                try {
                    if (s != null && !s.trim().isEmpty()) {
                        BigDecimal extractedAmount = new BigDecimal(s);
                        if (extractedAmount.compareTo(money) < 0) {
                            // 金额不足，直接返回，不执行后续逻辑
                            log.info("处理:{}.提取到的金额小于订单金额{} , {}", extractedAmount, JSONUtil.toJsonStr(dto));
                            return;
                        }
                    }
                } catch (Exception e) {
                    // 转换失败，按业务需求处理
                    log.info("处理:{}.没有提取到金额" + JSONUtil.toJsonStr(dto));
                    return;
                }
                thisService.pass(byPayNum.getId());
                dto.setOrderId(byPayNum.getId());
                dto.setProcessStatus(1);
                payChatMessageMapper.updateById(dto);
            }

        }
    }

    /**
     * 拼接管理员链接
     */
    public Order getAdminUrl(Order pay, String id, String token, String myToken) {

        String pass = payProConfig.getSite() + "/order/pass?id=" + id + "&token=" + token + "&myToken=" + myToken;
        pay.setPassUrl(pass);

        String back = payProConfig.getSite() + "/order/back?id=" + id + "&token=" + token + "&myToken=" + myToken;
        pay.setBackUrl(back);

        String edit = payProConfig.getSite() + "/order-edit?id=" + id + "&token=" + token;
        pay.setEditUrl(edit);

        String del = payProConfig.getSite() + "/order-del?id=" + id + "&token=" + token;
        pay.setDelUrl(del);

        String close = payProConfig.getSite() + "/order-close?id=" + id + "&token=" + token;
        pay.setCloseUrl(close);

        String statistic = payProConfig.getSite() + "/statistic?myToken=" + myToken;
        pay.setStatistic(statistic);
        return pay;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OpenApiOrderResp createOpenApiOrder(OpenApiOrderReq req) {
        if (!openApiSignUtil.verifyTimestamp(req.getTimestamp())) {
            throw new ApiException(ApiException.ErrorCode.TIMESTAMP_ERROR, "请求时间戳无效或已过期");
        }

        if (!openApiSignUtil.verifySign(req)) {
            throw new ApiException(ApiException.ErrorCode.SIGN_ERROR, "签名验证失败");
        }

        if (req.getAmount() == null || req.getAmount().compareTo(new BigDecimal("0")) <= 0) {
            throw new ApiException(ApiException.ErrorCode.AMOUNT_ERROR, "金额必须大于0");
        }

        if (req.getAmount().compareTo(new BigDecimal("100000")) > 0) {
            throw new ApiException(ApiException.ErrorCode.AMOUNT_ERROR, "金额超出限制，单笔订单不能超过100000元");
        }

        Order existingOrder = orderMapper.selectById(req.getOrderNo());
        if (existingOrder != null) {
            throw new ApiException(ApiException.ErrorCode.DUPLICATE_ORDER, "订单号已存在");
        }

        Order order = new Order();
        order.setId(req.getOrderNo());
        order.setMoney(req.getAmount());
        order.setPayType(req.getPayType());
        order.setNickName(req.getNickName());
        order.setEmail(req.getEmail());
        order.setNotifyUrl(req.getNotifyUrl());
        order.setUserId(req.getUserId());
        order.setProductId(req.getProductId());
        order.setOrderSource("OPENAPI");
        order.setState(OrderStatesEnum.WAIT_PAY.getState());
        order.setCreateTime(new Date());
        // 计算过期时间
        if (req.getExpireSeconds() != null && req.getExpireSeconds() > 0) {
            Date expireTime = new Date(req.getTimestamp() + req.getExpireSeconds() * 1000L);
            order.setExpireTime(expireTime);
        }
        order.setPayNum(StringUtils.getRandomNum());

        String qrUrl = "";
        int qrNum = 1; // 默认二维码编号
        BigDecimal actualAmount = req.getAmount(); // 实际使用的收款金额

        // 获取支付类型配置 - 先检查是否使用本地二维码
        Boolean useLocalQrCodeConfig = payProConfig.getUseLocalQrCode(req.getPayType());

        if (useLocalQrCodeConfig != null && useLocalQrCodeConfig) {
            // 使用本地二维码（数据库配置的自定义二维码）
            String configKey = getQrCodeConfigKey(req.getPayType());
            String customQrUrl = systemConfigService.getConfigValue(configKey);

            if (customQrUrl != null && !customQrUrl.isEmpty()) {
                // 使用数据库配置的自定义二维码
                qrUrl = payProConfig.getSite() + customQrUrl;
                req.setCustom(true);
                order.setPayQrNum(qrNum);
                log.info("使用本地自定义二维码: payType={}, qrUrl={}", req.getPayType(), qrUrl);
            } else {
                log.warn("配置了使用本地二维码但未上传二维码: payType={}", req.getPayType());
                // 没有上传自定义二维码，使用占位符
                qrUrl = payProConfig.getSite() + "/assets/qr/" + req.getPayType() + "/custom.png";
                req.setCustom(true);
                order.setPayQrNum(qrNum);
            }
        } else {
            // 不使用本地二维码，使用固定金额二维码
            // 查询金额映射，获取可用的实际收款金额列表
            List<BigDecimal> availableAmounts = getAvailableAmounts(req.getPayType(), req.getAmount());

            if (!availableAmounts.isEmpty()) {
                // 随机选择一个金额
                actualAmount = availableAmounts.get(new Random().nextInt(availableAmounts.size()));
            }

            // 获取该金额可用的二维码序号列表
            List<Integer> availableQrNumbers = getAvailableQrNumbers(req.getPayType(), actualAmount);

            if (!availableQrNumbers.isEmpty()) {
                // 从可用序号中随机选择一个
                qrNum = availableQrNumbers.get(new Random().nextInt(availableQrNumbers.size()));
            } else {
                // 如果没有找到可用的二维码，使用默认值
                qrNum = 1;
            }
            order.setPayQrNum(qrNum);

            String formattedAmount = String.format("%.2f", actualAmount);
            boolean fixedAmountExists = checkQrFileExists(req.getPayType(), actualAmount, qrNum);

            if(fixedAmountExists) {
                // 有固定金额的二维码，使用固定金额路径
                qrUrl = payProConfig.getSite() + "/assets/qr/" + req.getPayType() + "/" +
                        formattedAmount + "/" + qrNum + ".png";
                req.setCustom(false);
                log.info("使用固定金额二维码: payType={}, amount={}, qrNum={}", req.getPayType(), formattedAmount, qrNum);
            } else {
                // 没有固定金额的码，使用 custom.png
                qrUrl = payProConfig.getSite() + "/assets/qr/" + req.getPayType() + "/custom.png";
                req.setCustom(true);
                log.info("使用通用自定义二维码: payType={}", req.getPayType());
            }
        }

        String formattedAmount = String.format("%.2f", actualAmount);
        String returnUrl = payProConfig.getSite() + "/payment.html?" +
                "orderId=" + req.getOrderNo() +
                "&money=" + req.getAmount() +
                "&payType=" + req.getPayType() +
                "&payNum=" + order.getPayNum() +
                "&customerQr=" + req.getCustom() +
                "&picName=" + formattedAmount +
                "&qrCode=" + "undefined" +
                "&payQrNum=" + qrNum +
                "&useLocalQrCode=" + useLocalQrCodeConfig;
        try {
            orderMapper.insert(order);
        } catch (Exception e) {
            log.error("创建OpenApi订单失败: {}", e.getMessage(), e);
            throw new ApiException(ApiException.ErrorCode.SYSTEM_ERROR, "创建订单失败");
        }

        String tokenAdmin = UUID.randomUUID().toString();
        String orderTokenKey = RedisKeyConstant.getOrderTokenKey(order.getId());
        redisTemplate.opsForValue().set(orderTokenKey, tokenAdmin, payProConfig.getToken().getExpire(), TimeUnit.DAYS);
        order = getAdminUrl(order, order.getId(), tokenAdmin, payProConfig.getToken().getValue());

        // 发送多渠道通知（PushPlus > Server酱 > 邮件）
        try {
            multiChannelNotificationService.sendOrderNotification(order, tokenAdmin, payProConfig.getToken().getValue());
        } catch (Exception e) {
            log.error("通知发送失败: {}", e.getMessage());
        }

        return OpenApiOrderResp.builder()
                .orderNo(req.getOrderNo())
                .amount(req.getAmount())
                .payType(req.getPayType())
                .payNum(order.getPayNum())
                .state(order.getState())
                .message("订单创建成功")
                .qrCodeUrl(qrUrl)
                .returnUrl(returnUrl)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * 获取可用的实际收款金额列表
     * 根据充值金额和支付类型，从金额映射表中查询可用的实际收款金额
     */
    private List<BigDecimal> getAvailableAmounts(String payType, BigDecimal chargeAmount) {
        LambdaQueryWrapper<AmountMapping> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AmountMapping::getPayType, payType)
               .eq(AmountMapping::getChargeAmount, chargeAmount)
               .eq(AmountMapping::getEnabled, true)
               .orderByAsc(AmountMapping::getPriority);

        List<AmountMapping> mappings = amountMappingMapper.selectList(wrapper);

        if (mappings.isEmpty()) {
            // 如果没有配置映射，返回原金额
            return Collections.singletonList(chargeAmount);
        }

        return mappings.stream()
                .map(AmountMapping::getActualAmount)
                .collect(Collectors.toList());
    }

    /**
     * 根据支付类型获取数据库配置的 key
     */
    private String getQrCodeConfigKey(String payType) {
        switch (payType) {
            case "alipay":
                return "alipay.custom.qr.url";
            case "wechat":
                return "wechat.qr.url";
            case "wechat_zs":
                return "wechat.zs.qr.url";
            case "alipay_dmf":
                return "alipay.dmf.qr.url";
            default:
                return null;
        }
    }

    /**
     * 获取指定支付类型和金额的可用二维码序号列表
     * 扫描目录中实际存在的二维码文件，返回序号列表
     *
     * @param payType 支付类型，如 "wechat"、"alipay"
     * @param amount  金额，将格式化为两位小数
     * @return 可用的二维码序号列表，如果没有则返回空列表
     */
    private List<Integer> getAvailableQrNumbers(String payType, BigDecimal amount) {
        List<Integer> availableNumbers = new ArrayList<>();
        String amountStr = String.format("%.2f", amount);

        try {
            // 尝试加载目录资源
            Resource dirResource = resourceLoader.getResource("classpath:static/qr/" + payType.toLowerCase() + "/");
            if (dirResource.exists() && dirResource.getFile().isDirectory()) {
                File dir = dirResource.getFile();
                File[] files = dir.listFiles();

                if (files != null) {
                    // 匹配文件名格式：{payType}_fixed_{amount}_{序号}.{jpg|png}
                    String prefix = payType.toLowerCase() + "_fixed_" + amountStr + "_";

                    for (File file : files) {
                        String fileName = file.getName();
                        if (fileName.startsWith(prefix) && (fileName.endsWith(".jpg") || fileName.endsWith(".png"))) {
                            // 提取序号部分
                            String numberPart = fileName.substring(prefix.length());
                            numberPart = numberPart.substring(0, numberPart.lastIndexOf('.'));

                            try {
                                int seqNum = Integer.parseInt(numberPart);
                                availableNumbers.add(seqNum);
                            } catch (NumberFormatException e) {
                                log.warn("无法解析二维码序号: {}", fileName);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("扫描二维码目录失败: payType={}, amount={}, error={}", payType, amountStr, e.getMessage());
        }

        return availableNumbers;
    }

    /**
     * 检查指定支付类型和金额的二维码文件是否存在
     *
     * @param payType 支付类型，如 "wechat"、"alipay"，需与文件夹名称匹配
     * @param amount  金额，将格式化为两位小数作为文件夹名
     * @param qrNum   二维码编号，用于构建文件名
     * @return 文件存在返回 true，否则返回 false
     */
    private boolean checkQrFileExists(String payType, BigDecimal amount, int qrNum) {
        // 格式化金额为两位小数，与文件夹名称一致
        String amountStr = String.format("%.2f", amount);
        String seqStr = String.format("%03d", qrNum);

        // 尝试 jpg 和 png 两种格式
        String[] extensions = {".jpg", ".png"};
        for (String ext : extensions) {
            String filePath = "classpath:static/qr/" + payType.toLowerCase() + "/" +
                            payType.toLowerCase() + "_fixed_" + amountStr + "_" + seqStr + ext;
            try {
                Resource resource = resourceLoader.getResource(filePath);
                if (resource.exists()) {
                    return true;
                }
            } catch (Exception e) {
                // 继续尝试下一个扩展名
            }
        }
        return false;
    }

    public void callbackFaka(String notifyUrl, String orderNo, BigDecimal amount, String payNum) {
        Map<String, Object> params = new TreeMap<>();
        params.put("orderNo", orderNo);
        params.put("amount", amount);
        params.put("payNum", payNum);

        // 生成签名
        String sign = openApiSignUtil.generateSign(params);
        params.put("sign", sign);
        HttpUtil.post(notifyUrl, com.alibaba.fastjson2.JSONObject.toJSONString(params));
    }
}
