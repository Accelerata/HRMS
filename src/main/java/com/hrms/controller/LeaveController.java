package com.hrms.controller;

import com.hrms.annotation.RequirePermission;
import com.hrms.common.context.BaseContext;
import com.hrms.dto.LeaveApplyDTO;
import com.hrms.entity.LeaveApplication;
import com.hrms.entity.LeaveBalance;
import com.hrms.result.Result;
import com.hrms.service.LeaveService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 假期管理控制器
 *
 * 权限说明：
 * - leave:balance:view - 查看假期余额（普通员工及以上）
 * - leave:apply - 请假申请（普通员工及以上）
 * - leave:record:view - 查看请假记录（员工本人及管理者）
 * - leave:approve - 审批请假（部门主管/HR专员）
 */
@RestController
@RequestMapping("/api/v1/leave")
@RequiredArgsConstructor
public class LeaveController {

    private final LeaveService leaveService;

    // ═══════════════ 假期余额 ═══════════════

    /** 查询员工假期余额 */
    @GetMapping("/balance/{employeeId}")
    @RequirePermission("leave:balance:view")
    public Result<List<LeaveBalance>> balance(
            @PathVariable Long employeeId,
            @RequestParam(defaultValue = "2026") int year) {
        return Result.success(leaveService.getBalances(employeeId, year));
    }

    /** 初始化年假余额（入职时调用） */
    @PostMapping("/balance/annual/init")
    @RequirePermission("leave:balance:init")
    public Result<Void> initAnnualLeave(
            @RequestParam Long employeeId,
            @RequestParam String entryDate,
            @RequestParam(defaultValue = "2026") int year) {
        leaveService.initAnnualLeaveBalance(employeeId, java.time.LocalDate.parse(entryDate), year);
        return Result.success();
    }

    // ═══════════════ 请假申请 ═══════════════

    /** 提交请假申请 */
    @PostMapping("/apply")
    @RequirePermission("leave:apply")
    public Result<LeaveApplication> apply(@Valid @RequestBody LeaveApplyDTO dto) {
        // 从上下文获取当前登录用户ID
        Long currentUserId = BaseContext.getCurrentUserId();
        return Result.success(leaveService.apply(dto, currentUserId));
    }

    /** 查询员工请假记录 */
    @GetMapping("/applications/{employeeId}")
    @RequirePermission("leave:record:view")
    public Result<List<LeaveApplication>> applications(
            @PathVariable Long employeeId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.success(leaveService.getApplications(employeeId, page, size));
    }
}
