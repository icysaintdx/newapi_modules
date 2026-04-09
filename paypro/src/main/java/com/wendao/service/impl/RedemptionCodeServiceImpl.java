package com.wendao.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wendao.entity.CardDeliveryLog;
import com.wendao.entity.RedemptionCode;
import com.wendao.mapper.CardDeliveryLogMapper;
import com.wendao.mapper.RedemptionCodeMapper;
import com.wendao.service.RedemptionCodeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * 兑换码服务实现
 * 核心功能：安全、幂等的兑换码分配
 */
@Service
public class RedemptionCodeServiceImpl implements RedemptionCodeService {

    private static final Logger log = LoggerFactory.getLogger(RedemptionCodeServiceImpl.class);

    @Autowired
    private RedemptionCodeMapper redemptionCodeMapper;

    @Autowired
    private CardDeliveryLogMapper cardDeliveryLogMapper;

    /**
     * 为订单分配兑换码（安全、幂等）
     *
     * 安全机制：
     * 1. 幂等性检查：通过 idempotent_key 防止重复发卡
     * 2. 数据库行锁：使用 FOR UPDATE 防止并发问题
     * 3. 两阶段提交：先锁定，发送成功后再确认使用
     * 4. 审计日志：记录所有发卡操作
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public RedemptionCode allocateCodeForOrder(String orderId, Integer productId, String email) {

        // 1. 幂等性检查：生成幂等键（订单ID的MD5哈希）
        String idempotentKey = DigestUtils.md5DigestAsHex(orderId.getBytes());

        LambdaQueryWrapper<CardDeliveryLog> logQuery = new LambdaQueryWrapper<>();
        logQuery.eq(CardDeliveryLog::getIdempotentKey, idempotentKey);
        CardDeliveryLog existingLog = cardDeliveryLogMapper.selectOne(logQuery);

        if (existingLog != null) {
            log.warn("订单已发卡，拒绝重复处理: orderId={}, idempotentKey={}", orderId, idempotentKey);

            // 如果之前发卡成功，返回已分配的兑换码
            if (existingLog.getStatus() == 1) {
                LambdaQueryWrapper<RedemptionCode> codeQuery = new LambdaQueryWrapper<>();
                codeQuery.eq(RedemptionCode::getId, existingLog.getRedemptionCodeId());
                return redemptionCodeMapper.selectOne(codeQuery);
            }

            // 如果之前发卡失败，返回null（调用方可以重试）
            return null;
        }

        // 2. 原子性锁定一个可用的兑换码
        String lockToken = UUID.randomUUID().toString();
        Date lockTime = new Date();

        int lockedRows = redemptionCodeMapper.lockAvailableCode(productId, lockToken, lockTime);

        if (lockedRows == 0) {
            log.error("没有可用的兑换码: productId={}, orderId={}", productId, orderId);

            // 记录失败日志
            CardDeliveryLog failLog = new CardDeliveryLog();
            failLog.setOrderId(orderId);
            failLog.setRedemptionCodeId(0L);
            failLog.setCode("");
            failLog.setEmail(email);
            failLog.setStatus(0);
            failLog.setErrorMsg("库存不足，没有可用的兑换码");
            failLog.setCreateTime(new Date());
            failLog.setIdempotentKey(idempotentKey);
            cardDeliveryLogMapper.insert(failLog);

            return null;
        }

        // 3. 查询锁定的兑换码
        LambdaQueryWrapper<RedemptionCode> codeQuery = new LambdaQueryWrapper<>();
        codeQuery.eq(RedemptionCode::getLockToken, lockToken);
        codeQuery.eq(RedemptionCode::getStatus, 2); // 已锁定状态
        RedemptionCode lockedCode = redemptionCodeMapper.selectOne(codeQuery);

        if (lockedCode == null) {
            log.error("锁定兑换码后查询失败: lockToken={}", lockToken);
            return null;
        }

        log.info("成功锁定兑换码: codeId={}, code={}, orderId={}",
                lockedCode.getId(), lockedCode.getCode(), orderId);

        // 4. 返回锁定的兑换码（调用方负责发送邮件）
        // 注意：此时兑换码状态为"已锁定"，邮件发送成功后需要调用 confirmUseCode 确认使用
        return lockedCode;
    }

    /**
     * 确认使用兑换码（邮件发送成功后调用）
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean confirmUseCode(String lockToken, String orderId, String email, String code, Long codeId) {

        // 1. 确认使用兑换码（将状态从"已锁定"改为"已使用"）
        Date usedTime = new Date();
        int confirmedRows = redemptionCodeMapper.confirmUseCode(lockToken, orderId, usedTime);

        if (confirmedRows == 0) {
            log.error("确认使用兑换码失败: lockToken={}, orderId={}", lockToken, orderId);
            return false;
        }

        // 2. 记录成功的发卡日志
        String idempotentKey = DigestUtils.md5DigestAsHex(orderId.getBytes());

        CardDeliveryLog successLog = new CardDeliveryLog();
        successLog.setOrderId(orderId);
        successLog.setRedemptionCodeId(codeId);
        successLog.setCode(code);
        successLog.setEmail(email);
        successLog.setStatus(1);
        successLog.setErrorMsg(null);
        successLog.setCreateTime(new Date());
        successLog.setIdempotentKey(idempotentKey);
        cardDeliveryLogMapper.insert(successLog);

        log.info("确认使用兑换码成功: codeId={}, orderId={}", codeId, orderId);
        return true;
    }

    /**
     * 批量导入兑换码
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchImportCodes(Integer productId, List<String> codes, String batchId) {

        int successCount = 0;
        Date now = new Date();

        for (String code : codes) {
            // 去除空白字符
            code = code.trim();
            if (code.isEmpty()) {
                continue;
            }

            // 检查是否已存在
            LambdaQueryWrapper<RedemptionCode> query = new LambdaQueryWrapper<>();
            query.eq(RedemptionCode::getCode, code);
            RedemptionCode existing = redemptionCodeMapper.selectOne(query);

            if (existing != null) {
                log.warn("兑换码已存在，跳过: code={}", code);
                continue;
            }

            // 插入新兑换码
            RedemptionCode newCode = new RedemptionCode();
            newCode.setCode(code);
            newCode.setProductId(productId);
            newCode.setStatus(0); // 未使用
            newCode.setCreateTime(now);
            newCode.setBatchId(batchId);

            int result = redemptionCodeMapper.insert(newCode);
            if (result > 0) {
                successCount++;
            }
        }

        log.info("批量导入兑换码完成: productId={}, batchId={}, total={}, success={}",
                productId, batchId, codes.size(), successCount);

        return successCount;
    }

    /**
     * 获取商品可用库存
     */
    @Override
    public int getAvailableStock(Integer productId) {
        LambdaQueryWrapper<RedemptionCode> query = new LambdaQueryWrapper<>();
        query.eq(RedemptionCode::getProductId, productId);
        query.eq(RedemptionCode::getStatus, 0); // 未使用状态

        return redemptionCodeMapper.selectCount(query).intValue();
    }

    /**
     * 释放超时锁定的兑换码（定时任务调用）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int releaseTimeoutLocks(int timeoutMinutes) {
        int releasedCount = redemptionCodeMapper.releaseTimeoutLocks(timeoutMinutes);

        if (releasedCount > 0) {
            log.info("释放超时锁定的兑换码: count={}, timeoutMinutes={}", releasedCount, timeoutMinutes);
        }

        return releasedCount;
    }
}
