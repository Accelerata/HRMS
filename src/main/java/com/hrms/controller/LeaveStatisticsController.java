package com.hrms.controller;

import com.hrms.annotation.RequirePermission;
import com.hrms.result.Result;
import com.hrms.service.LeaveStatisticsService;
import com.hrms.vo.DeptLeaveRateVO;
import com.hrms.vo.LeaveTypeDistributionVO;
import com.hrms.vo.PersonalLeaveStatsVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 请假统计控制器
 *
 * 权限说明：
 * - att:leave:view - 查看请假统计（员工及以上，数据范围在 Service 层控制）
 */
@RestController
@RequestMapping("/api/v1/leave/stats")
@RequiredArgsConstructor
public class LeaveStatisticsController {

    private final LeaveStatisticsService leaveStatisticsService;

    /** 个人请假统计 */
    @GetMapping("/personal/{employeeId}")
    @RequirePermission("att:leave:view")
    public Result<PersonalLeaveStatsVO> personal(
            @PathVariable Long employeeId,
            @RequestParam int year,
            @RequestParam int month) {
        return Result.success(leaveStatisticsService.personalStats(employeeId, year, month));
    }

    /** 部门请假率统计 */
    @GetMapping("/dept/{deptId}")
    @RequirePermission("att:leave:view")
    public Result<DeptLeaveRateVO> dept(
            @PathVariable Long deptId,
            @RequestParam int year,
            @RequestParam int month) {
        return Result.success(leaveStatisticsService.deptStats(deptId, year, month));
    }

    /** 请假类型分布 */
    @GetMapping("/type-distribution")
    @RequirePermission("att:leave:view")
    public Result<List<LeaveTypeDistributionVO>> typeDistribution(
            @RequestParam int year,
            @RequestParam int month,
            @RequestParam(required = false) Long deptId) {
        return Result.success(leaveStatisticsService.typeDistribution(deptId, year, month));
    }
}
