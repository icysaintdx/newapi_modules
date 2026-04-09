package com.wendao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wendao.entity.RedemptionCode;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 兑换码Mapper
 */
@Mapper
public interface RedemptionCodeMapper extends BaseMapper<RedemptionCode> {

    /**
     * 原子性锁定一个可用的兑换码（使用数据库行锁防止并发）
     *
     * @param productId 商品ID
     * @param lockToken 锁定令牌（UUID）
     * @param lockTime 锁定时间
     * @return 影响的行数
     */
    @Update("UPDATE t_redemption_code " +
            "SET status = 2, lock_token = #{lockToken}, lock_time = #{lockTime} " +
            "WHERE id = (" +
            "  SELECT id FROM (" +
            "    SELECT id FROM t_redemption_code " +
            "    WHERE product_id = #{productId} AND status = 0 " +
            "    ORDER BY id ASC LIMIT 1 FOR UPDATE" +
            "  ) tmp" +
            ") AND status = 0")
    int lockAvailableCode(@Param("productId") Integer productId,
                          @Param("lockToken") String lockToken,
                          @Param("lockTime") java.util.Date lockTime);

    /**
     * 确认使用兑换码（将锁定状态改为已使用）
     *
     * @param lockToken 锁定令牌
     * @param orderId 订单ID
     * @param usedTime 使用时间
     * @return 影响的行数
     */
    @Update("UPDATE t_redemption_code " +
            "SET status = 1, order_id = #{orderId}, used_time = #{usedTime} " +
            "WHERE lock_token = #{lockToken} AND status = 2")
    int confirmUseCode(@Param("lockToken") String lockToken,
                       @Param("orderId") String orderId,
                       @Param("usedTime") java.util.Date usedTime);

    /**
     * 释放超时的锁定（定时任务清理）
     *
     * @param timeoutMinutes 超时分钟数
     * @return 影响的行数
     */
    @Update("UPDATE t_redemption_code " +
            "SET status = 0, lock_token = NULL, lock_time = NULL " +
            "WHERE status = 2 AND lock_time < DATE_SUB(NOW(), INTERVAL #{timeoutMinutes} MINUTE)")
    int releaseTimeoutLocks(@Param("timeoutMinutes") int timeoutMinutes);
}
