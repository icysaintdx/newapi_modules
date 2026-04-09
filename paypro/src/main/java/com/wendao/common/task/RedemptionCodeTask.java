package com.wendao.common.task;

import com.wendao.service.RedemptionCodeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 兑换码定时任务
 */
@Component
public class RedemptionCodeTask {

    private static final Logger log = LoggerFactory.getLogger(RedemptionCodeTask.class);

    @Autowired
    private RedemptionCodeService redemptionCodeService;

    /**
     * 每5分钟释放超时锁定的兑换码
     * 超时时间：10分钟
     */
    @Scheduled(cron = "0 */5 * * * ?")
    public void releaseTimeoutLocks() {
        try {
            int releasedCount = redemptionCodeService.releaseTimeoutLocks(10);
            if (releasedCount > 0) {
                log.info("定时任务：释放超时锁定的兑换码，数量: {}", releasedCount);
            }
        } catch (Exception e) {
            log.error("定时任务执行失败：释放超时锁定的兑换码", e);
        }
    }
}
