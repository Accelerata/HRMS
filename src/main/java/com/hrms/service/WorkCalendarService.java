package com.hrms.service;

import com.hrms.common.exception.BaseException;
import com.hrms.entity.WorkCalendar;
import com.hrms.mapper.WorkCalendarMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 工作日历服务
 *
 * 工作日判定规则：
 * 1. 命中 work_calendar 记录 → 按 day_type（1=休息，2=工作日）返回；
 * 2. 否则周六/周日 → 非工作日；
 * 3. 其余 → 工作日。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkCalendarService {

    /** 法定节假日/休息 */
    public static final int DAY_TYPE_REST = 1;
    /** 调班工作日 */
    public static final int DAY_TYPE_WORK = 2;

    private final WorkCalendarMapper workCalendarMapper;

    /**
     * 判断指定日期是否为工作日
     */
    public boolean isWorkday(LocalDate date) {
        WorkCalendar cal = workCalendarMapper.selectByDate(date);
        if (cal != null) {
            return cal.getDayType() == DAY_TYPE_WORK;
        }
        // 默认：周六日休息，周一至周五工作日
        DayOfWeek dow = date.getDayOfWeek();
        return dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY;
    }

    /**
     * 返回闭区间 [start, end] 内所有工作日列表
     */
    public List<LocalDate> workdaysBetween(LocalDate start, LocalDate end) {
        if (start == null || end == null || end.isBefore(start)) {
            throw BaseException.badRequest("日期区间非法");
        }
        List<LocalDate> workdays = new ArrayList<>();
        LocalDate cursor = start;
        while (!cursor.isAfter(end)) {
            if (isWorkday(cursor)) {
                workdays.add(cursor);
            }
            cursor = cursor.plusDays(1);
        }
        return workdays;
    }

    // ═══════════════ HR 配置接口 ═══════════════

    /** 按年查询节假日/调班列表 */
    public List<WorkCalendar> listByYear(int year) {
        return workCalendarMapper.selectByYear(year);
    }

    /** 批量保存（upsert）节假日/调班 */
    public void batchSave(List<WorkCalendar> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        for (WorkCalendar cal : list) {
            cal.setYear(cal.getCalendarDate().getYear());
        }
        workCalendarMapper.batchUpsert(list);
        log.info("批量写入工作日历 {} 条", list.size());
    }

    /** 删除某日配置 */
    public void deleteByDate(LocalDate date) {
        workCalendarMapper.deleteByDate(date);
        log.info("删除工作日历: {}", date);
    }
}
