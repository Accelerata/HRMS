package com.hrms.controller;

import com.hrms.annotation.RequirePermission;
import com.hrms.result.Result;
import com.hrms.service.AttendanceStatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 考勤统计控制器
 *
 * 权限说明：
 * - emp:attendance:view — 查看考勤统计（HR/主管及以上）
 * - 普通员工仅能查看个人统计（通过 PersonalCenterController）
 */
@RestController
@RequestMapping("/api/v1/attendance-statistics")
@RequiredArgsConstructor
public class AttendanceStatisticsController {

    private final AttendanceStatisticsService statisticsService;

    /**
     * 个人月度考勤统计
     * GET /api/v1/attendance-statistics/personal?employeeId=1&year=2026&month=7
     */
    @GetMapping("/personal")
    @RequirePermission("emp:attendance:view")
    public Result<Map<String, Object>> personalStats(@RequestParam Long employeeId,
                                                     @RequestParam int year,
                                                     @RequestParam int month) {
        AttendanceStatisticsService.PersonalStats stats = statisticsService.personalMonthlyStats(employeeId, year, month);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("employeeId", stats.employeeId);
        result.put("year", stats.year);
        result.put("month", stats.month);
        result.put("totalWorkDays", stats.totalWorkDays);
        result.put("normalDays", stats.normalDays);
        result.put("lateDays", stats.lateDays);
        result.put("earlyDays", stats.earlyDays);
        result.put("absentHalfDays", stats.absentHalfDays);
        result.put("missingPunchDays", stats.missingPunchDays);
        result.put("attendanceRate", stats.attendanceRate);
        return Result.success(result);
    }

    /**
     * 部门月度考勤统计
     * GET /api/v1/attendance-statistics/dept?deptId=1&year=2026&month=7
     */
    @GetMapping("/dept")
    @RequirePermission("emp:attendance:view")
    public Result<Map<String, Object>> deptStats(@RequestParam Long deptId,
                                                  @RequestParam int year,
                                                  @RequestParam int month) {
        AttendanceStatisticsService.DeptStats stats = statisticsService.deptMonthlyStats(deptId, year, month);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("deptId", stats.deptId);
        result.put("year", stats.year);
        result.put("month", stats.month);
        result.put("totalEmployees", stats.totalEmployees);
        result.put("recordedEmployeeCount", stats.recordedEmployeeCount);
        result.put("normalDays", stats.normalDays);
        result.put("lateDays", stats.lateDays);
        result.put("earlyDays", stats.earlyDays);
        result.put("absentHalfDays", stats.absentHalfDays);
        result.put("missingPunchDays", stats.missingPunchDays);
        result.put("deptAttendanceRate", stats.deptAttendanceRate);
        return Result.success(result);
    }
}
