package com.hrms.controller;

import com.hrms.annotation.RequirePermission;
import com.hrms.entity.WorkCalendar;
import com.hrms.result.Result;
import com.hrms.service.WorkCalendarService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 工作日历管理控制器
 *
 * 权限说明：
 * - att:calendar        - 查看工作日历（菜单）
 * - att:calendar:manage - 配置节假日/调班（HR 专员、管理员）
 */
@RestController
@RequestMapping("/api/v1/calendar")
@RequiredArgsConstructor
public class WorkCalendarController {

    private final WorkCalendarService workCalendarService;

    /** 查询指定年份的节假日/调班列表 */
    @GetMapping
    @RequirePermission("att:calendar:manage")
    public Result<List<WorkCalendar>> list(@RequestParam(defaultValue = "2026") int year) {
        return Result.success(workCalendarService.listByYear(year));
    }

    /** 批量保存（upsert）节假日/调班 */
    @PostMapping("/batch")
    @RequirePermission("att:calendar:manage")
    public Result<Void> batchSave(@RequestBody List<WorkCalendar> list) {
        workCalendarService.batchSave(list);
        return Result.success();
    }

    /** 删除某日配置 */
    @DeleteMapping
    @RequirePermission("att:calendar:manage")
    public Result<Void> delete(@RequestParam String date) {
        workCalendarService.deleteByDate(LocalDate.parse(date));
        return Result.success();
    }
}
