package com.wendao.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wendao.entity.ExtractCode;
import com.wendao.mapper.ExtractCodeMapper;
import com.wendao.service.ExtractCodeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Random;

/**
 * 提取码服务实现
 */
@Service
public class ExtractCodeServiceImpl implements ExtractCodeService {

    private static final Logger log = LoggerFactory.getLogger(ExtractCodeServiceImpl.class);

    @Autowired
    private ExtractCodeMapper extractCodeMapper;

    /**
     * 生成6位数字提取码
     */
    private String generateCode() {
        Random random = new Random();
        return String.format("%06d", random.nextInt(1000000));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String generateExtractCode(String orderId, Long redemptionCodeId, String redemptionCode) {
        // 检查该订单是否已经生成过提取码
        LambdaQueryWrapper<ExtractCode> query = new LambdaQueryWrapper<>();
        query.eq(ExtractCode::getOrderId, orderId);
        ExtractCode existing = extractCodeMapper.selectOne(query);

        if (existing != null) {
            log.info("订单已存在提取码: orderId={}, code={}", orderId, existing.getCode());
            return existing.getCode();
        }

        // 生成唯一的提取码
        String code;
        int maxRetries = 10;
        int retries = 0;

        do {
            code = generateCode();
            LambdaQueryWrapper<ExtractCode> checkQuery = new LambdaQueryWrapper<>();
            checkQuery.eq(ExtractCode::getCode, code);
            ExtractCode duplicate = extractCodeMapper.selectOne(checkQuery);

            if (duplicate == null) {
                break;
            }

            retries++;
            if (retries >= maxRetries) {
                throw new RuntimeException("生成提取码失败：重试次数过多");
            }
        } while (true);

        // 创建提取码记录
        ExtractCode extractCode = new ExtractCode();
        extractCode.setCode(code);
        extractCode.setOrderId(orderId);
        extractCode.setRedemptionCodeId(redemptionCodeId);
        extractCode.setRedemptionCode(redemptionCode);
        extractCode.setStatus(0); // 未使用
        extractCode.setCreateTime(new Date());

        // 设置过期时间（24小时后）
        Date expireTime = new Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000);
        extractCode.setExpireTime(expireTime);

        extractCodeMapper.insert(extractCode);

        log.info("生成提取码成功: orderId={}, code={}", orderId, code);
        return code;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String useExtractCode(String code) {
        LambdaQueryWrapper<ExtractCode> query = new LambdaQueryWrapper<>();
        query.eq(ExtractCode::getCode, code);
        ExtractCode extractCode = extractCodeMapper.selectOne(query);

        if (extractCode == null) {
            log.warn("提取码不存在: code={}", code);
            return null;
        }

        // 检查是否已使用
        if (extractCode.getStatus() == 1) {
            log.warn("提取码已使用: code={}", code);
            return null;
        }

        // 检查是否过期
        if (new Date().after(extractCode.getExpireTime())) {
            log.warn("提取码已过期: code={}, expireTime={}", code, extractCode.getExpireTime());
            return null;
        }

        // 标记为已使用
        extractCode.setStatus(1);
        extractCode.setUsedTime(new Date());
        extractCodeMapper.updateById(extractCode);

        log.info("提取码使用成功: code={}, redemptionCode={}", code, extractCode.getRedemptionCode());
        return extractCode.getRedemptionCode();
    }

    @Override
    public ExtractCode getExtractCodeInfo(String code) {
        LambdaQueryWrapper<ExtractCode> query = new LambdaQueryWrapper<>();
        query.eq(ExtractCode::getCode, code);
        return extractCodeMapper.selectOne(query);
    }
}
