package com.hrms.service;

import com.hrms.entity.CompLeaveGrant;
import com.hrms.entity.LeaveBalance;
import com.hrms.enums.LeaveType;
import com.hrms.mapper.CompLeaveGrantMapper;
import com.hrms.mapper.LeaveBalanceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 调休过期清零任务
 *
 * 每日 03:00 扫描已过期但仍有效的调休入账，执行清零。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CompLeaveExpiryTask {

    private final CompLeaveGrantMapper compLeaveGrantMapper;
    private final LeaveBalanceMapper leaveBalanceMapper;

    /**
     * 每日 03:00 执行过期清零
     */
    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void expireGrants() {
        LocalDate today = LocalDate.now();
        log.info("定时任务: 调休过期清零开始, today={}", today);

        List<CompLeaveGrant> expiredGrants = compLeaveGrantMapper.selectExpiredButActive(today);
        int count = 0;

        for (CompLeaveGrant grant : expiredGrants) {
            try {
                // 未用完差额
                BigDecimal remaining = grant.getDays().subtract(grant.getUsedDays());
                if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                    // 已用完，直接标记过期
                    compLeaveGrantMapper.markExpired(grant.getId());
                    count++;
                    continue;
                }

                // 从 leave_balance 扣减剩余（下限 0）
                int year = grant.getExpireDate().getYear();
                LeaveBalance balance = leaveBalanceMapper.selectByEmployeeTypeAndYear(
                        grant.getEmployeeId(), LeaveType.COMPENSATORY.getCode(), year);
                if (balance != null) {
                    BigDecimal newRemaining = balance.getRemainingDays().subtract(remaining);
                    if (newRemaining.compareTo(BigDecimal.ZERO) < 0) {
                        newRemaining = BigDecimal.ZERO;
                    }
                    balance.setRemainingDays(newRemaining);
                    balance.setTotalDays(balance.getTotalDays().subtract(remaining).max(BigDecimal.ZERO));
                    leaveBalanceMapper.update(balance);
                }

                // 置为已过期
                compLeaveGrantMapper.markExpired(grant.getId());
                count++;

                log.info("调休过期清零: grantId={}, employeeId={}, expireDate={}, days={}, used={}, remaining={}",
                        grant.getId(), grant.getEmployeeId(), grant.getExpireDate(),
                        grant.getDays(), grant.getUsedDays(), remaining);
            } catch (Exception e) {
                log.error("调休过期清零失败: grantId={}, error={}", grant.getId(), e.getMessage());
            }
        }

        log.info("定时任务: 调休过期清零完成, 共{}笔", count);
    }
}
