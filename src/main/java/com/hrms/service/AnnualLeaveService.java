package com.hrms.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * 年假计算服务
 *
 * 负责根据入职年限、社会工龄等计算员工年假天数。
 * 试用期员工按入职月数比例折算当年年假。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnnualLeaveService {

    private static final int SCALE = 1; // 保留1位小数（支持半天）

    /**
     * 计算试用期员工当年年假天数（按入职月份比例折算）
     *
     * 规则：
     * - 入职不满1年：按已工作月份 / 12 比例折算
     * - 折算后不足0.5天的部分舍去
     *
     * @param joinDate   入职日期
     * @param totalYears 总社会工龄（用于确定年假基数）
     * @return 当年应享年假天数
     */
    public BigDecimal calculateProbationYearLeave(LocalDate joinDate, int totalYears) {
        // 确定年假基数
        BigDecimal baseDays = getBaseDaysByTotalYears(totalYears);

        // 计算入职当年剩余月份
        LocalDate now = LocalDate.now();
        long monthsWorked = ChronoUnit.MONTHS.between(
                joinDate.withDayOfMonth(1),
                now.withDayOfMonth(1));
        // 至少1个月
        monthsWorked = Math.max(1, Math.min(12, monthsWorked + 1));

        // 按比例折算
        BigDecimal ratio = BigDecimal.valueOf(monthsWorked)
                .divide(BigDecimal.valueOf(12), 4, RoundingMode.HALF_UP);
        BigDecimal proratedDays = baseDays.multiply(ratio)
                .setScale(SCALE, RoundingMode.DOWN);

        log.info("试用期年假计算: joinDate={}, totalYears={}, baseDays={}, months={}, proratedDays={}",
                joinDate, totalYears, baseDays, monthsWorked, proratedDays);

        return proratedDays;
    }

    /**
     * 根据总社会工龄确定年假基数
     *
     * @param totalYears 总工作年限
     * @return 年假基数天数
     */
    public BigDecimal getBaseDaysByTotalYears(int totalYears) {
        if (totalYears < 1) {
            return new BigDecimal("5");  // 不满1年按5天基数
        } else if (totalYears < 10) {
            return new BigDecimal("5");
        } else if (totalYears < 20) {
            return new BigDecimal("10");
        } else {
            return new BigDecimal("15");
        }
    }

    /**
     * 根据入职日期计算当年年假（含试用期折算）
     *
     * @param entryDate  入职日期
     * @param totalYears 总社会工龄
     * @param year       计算年份
     * @return 当年年假天数
     */
    public BigDecimal calculateAnnualLeave(LocalDate entryDate, int totalYears, int year) {
        int serviceYears = year - entryDate.getYear();

        if (serviceYears < 1) {
            // 入职当年，按比例折算
            return calculateProbationYearLeave(entryDate, totalYears);
        }

        // 入职满1年后，按标准规则
        return getBaseDaysByTotalYears(totalYears + serviceYears);
    }
}
