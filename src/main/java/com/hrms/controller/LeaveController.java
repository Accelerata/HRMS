package com.hrms.controller;

import com.hrms.annotation.RequirePermission;
import com.hrms.dto.ApprovalActionDTO;
import com.hrms.dto.LeaveApplyDTO;
import com.hrms.entity.LeaveApplication;
import com.hrms.entity.LeaveBalance;
import com.hrms.result.Result;
import com.hrms.service.LeaveDayCalculator;
import com.hrms.service.LeaveService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 假期管理控制器
 *
 * 权限说明（与 RBAC 种子 att:leave:* 对齐）：
 * - att:leave:view    - 查看假期余额/请假记录（普通员工及以上）
 * - att:leave:apply   - 请假申请/提交/取消（普通员工及以上）
 * - att:leave:approve - 审批请假（部门主管/HR专员）
 * - att:record:manage - 初始化年假余额（HR专员）
 */
@RestController
@RequestMapping("/api/v1/leave")
@RequiredArgsConstructor
public class LeaveController {

    private final LeaveService leaveService;
    private final LeaveDayCalculator leaveDayCalculator;

    // ═══════════════ 天数试算预览 ═══════════════

    /** 天数试算预览（供前端实时回显） */
    @GetMapping("/days/calculate")
    @RequirePermission("att:leave:apply")
    public Result<BigDecimal> calculateDays(
            @RequestParam String startDate,
            @RequestParam(defaultValue = "0") int startPeriod,
            @RequestParam String endDate,
            @RequestParam(defaultValue = "1") int endPeriod) {
        BigDecimal days = leaveDayCalculator.calculate(
                LocalDate.parse(startDate), startPeriod,
                LocalDate.parse(endDate), endPeriod);
        return Result.success(days);
    }

    // ═══════════════ 假期余额 ═══════════════

    /** 查询员工假期余额 */
    @GetMapping("/balance/{employeeId}")
    @RequirePermission("att:leave:view")
    public Result<List<LeaveBalance>> balance(
            @PathVariable Long employeeId,
            @RequestParam(defaultValue = "2026") int year) {
        return Result.success(leaveService.getBalances(employeeId, year));
    }

    /** 初始化年假余额（入职时调用） */
    @PostMapping("/balance/annual/init")
    @RequirePermission("att:record:manage")
    public Result<Void> initAnnualLeave(
            @RequestParam Long employeeId,
            @RequestParam String entryDate,
            @RequestParam(defaultValue = "2026") int year) {
        leaveService.initAnnualLeaveBalance(employeeId, java.time.LocalDate.parse(entryDate), year);
        return Result.success();
    }

    // ═══════════════ 请假申请 ═══════════════

    /** 创建请假申请（草稿） */
    @PostMapping("/apply")
    @RequirePermission("att:leave:apply")
    public Result<LeaveApplication> apply(@Valid @RequestBody LeaveApplyDTO dto) {
        return Result.success(leaveService.apply(dto));
    }

    /** 提交请假申请进入审批 */
    @PostMapping("/{id}/submit")
    @RequirePermission("att:leave:apply")
    public Result<Void> submit(@PathVariable Long id) {
        leaveService.submit(id);
        return Result.success();
    }

    /** 审批请假申请 */
    @PostMapping("/{id}/approve")
    @RequirePermission("att:leave:approve")
    public Result<Void> approve(@PathVariable Long id, @RequestBody ApprovalActionDTO dto) {
        leaveService.approve(id, dto);
        return Result.success();
    }

    /** 取消请假申请（仅审批中 + 本人） */
    @PostMapping("/{id}/cancel")
    @RequirePermission("att:leave:apply")
    public Result<Void> cancel(@PathVariable Long id) {
        leaveService.cancel(id);
        return Result.success();
    }

    /** 查询员工请假记录 */
    @GetMapping("/applications/{employeeId}")
    @RequirePermission("att:leave:view")
    public Result<List<LeaveApplication>> applications(
            @PathVariable Long employeeId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.success(leaveService.getApplications(employeeId, page, size));
    }
}
