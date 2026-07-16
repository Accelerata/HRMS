package com.hrms.service;

import com.hrms.common.exception.BaseException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 请假天数计算器
 *
 * 规则：
 * 1. 区间内所有工作日逐日累加
 * 2. 开始日：时段为上午(0) → 计 1.0，下午(1) → 计 0.5
 * 3. 结束日：时段为上午(0) → 计 0.5，下午(1) → 计 1.0
 * 4. 同一天：上午→上午=0.5，下午→下午=0.5，上午→下午=1.0，下午→上午 → 非法（结束<开始）
 * 5. 中间工作日各计 1.0
 * 6. 区间内无工作日 → 400
 */
@Component
@RequiredArgsConstructor
public class LeaveDayCalculator {

    /** 上午 */
    public static final int PERIOD_AM = 0;
    /** 下午 */
    public static final int PERIOD_PM = 1;

    private final WorkCalendarService workCalendarService;

    /**
     * 根据起止日期与时段计算请假天数
     *
     * @param startDate   开始日期
     * @param startPeriod 开始时段：0-上午 1-下午
     * @param endDate     结束日期
     * @param endPeriod   结束时段：0-上午 1-下午
     * @return 请假天数（0.5 精度）
     */
    public BigDecimal calculate(LocalDate startDate, int startPeriod, LocalDate endDate, int endPeriod) {
        // 参数校验
        if (startPeriod != PERIOD_AM && startPeriod != PERIOD_PM) {
            throw BaseException.badRequest("时段取值非法，仅允许 0（上午）或 1（下午）");
        }
        if (endPeriod != PERIOD_AM && endPeriod != PERIOD_PM) {
            throw BaseException.badRequest("时段取值非法，仅允许 0（上午）或 1（下午）");
        }
        if (endDate.isBefore(startDate)) {
            throw BaseException.badRequest("结束日期不能早于开始日期");
        }

        // 获取区间内所有工作日
        List<LocalDate> workdays = workCalendarService.workdaysBetween(startDate, endDate);
        if (workdays.isEmpty()) {
            throw BaseException.badRequest("该区间无工作日");
        }

        if (startDate.equals(endDate)) {
            // 同一天
            if (startPeriod == PERIOD_AM && endPeriod == PERIOD_PM) {
                return BigDecimal.ONE;           // 上午 → 下午 = 1.0
            } else {
                return new BigDecimal("0.5");    // 上午→上午 或 下午→下午 = 0.5
            }
        }

        // 多天：中间工作日各计 1.0，边界日按时段折半
        // 开始日：上午=1.0, 下午=0.5
        // 结束日：上午=0.5, 下午=1.0
        // 中间天数 = workdays.size() - 2（含首尾） + 开始日 + 结束日
        BigDecimal startDayPart = (startPeriod == PERIOD_AM) ? BigDecimal.ONE : new BigDecimal("0.5");
        BigDecimal endDayPart = (endPeriod == PERIOD_PM) ? BigDecimal.ONE : new BigDecimal("0.5");

        // 中间工作日数
        int middleCount = workdays.size() - 2;
        BigDecimal middleDays = BigDecimal.valueOf(middleCount);

        return startDayPart.add(middleDays).add(endDayPart);
    }
}
