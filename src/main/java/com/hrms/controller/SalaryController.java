package com.hrms.controller;

import com.hrms.annotation.RequirePermission;
import com.hrms.dto.ApprovalActionDTO;
import com.hrms.dto.SalaryBatchCalcDTO;
import com.hrms.entity.SalaryBatch;
import com.hrms.entity.SalaryRecord;
import com.hrms.mapper.SalaryRecordMapper;
import com.hrms.result.Result;
import com.hrms.service.SalaryBatchService;
import com.hrms.vo.SalaryCalcResultVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 薪资管理控制器
 *
 * 权限说明（与 RBAC 种子 salary:calc:* 对齐）：
 * - salary:calc:calc    — 薪资核算、批次提交（HR专员及以上）
 * - salary:calc:view    — 查看薪资记录/批次（HR专员、部门主管[本部门]、财务专员、员工本人）
 * - salary:calc:approve — 批次审批（财务专员）、单条确认/发放（HR专员及以上）
 * - salary:detail       — 查看工资条详情（需二次验证）
 */
@RestController
@RequestMapping("/api/v1/salary")
@RequiredArgsConstructor
public class SalaryController {

    private final SalaryBatchService salaryBatchService;
    private final SalaryRecordMapper salaryRecordMapper;

    // ═══════════════ 薪资核算 ═══════════════

    /**
     * 单个员工薪资核算
     * 重新核算后，覆盖该员工该月的原有薪资记录
     */
    @PostMapping("/calculate/{employeeId}")
    @RequirePermission("salary:calc:calc")
    public Result<SalaryCalcResultVO> calculate(
            @PathVariable Long employeeId,
            @RequestParam int year,
            @RequestParam int month) {
        SalaryCalcResultVO result = salaryBatchService.calculateForEmployee(employeeId, year, month);
        return Result.success(result);
    }

    /**
     * 批量薪资核算
     * 传入 year + month，自动核算全公司（或指定部门）在职员工，生成薪资批次
     */
    @PostMapping("/batch-calculate")
    @RequirePermission("salary:calc:calc")
    public Result<Integer> batchCalculate(@Valid @RequestBody SalaryBatchCalcDTO dto) {
        int count = salaryBatchService.batchCalculate(dto.getYear(), dto.getMonth());
        return Result.success(count);
    }

    // ═══════════════ 薪资批次审批 ═══════════════

    /** 批次列表（含全公司实发合计，仅 HR/财务可见） */
    @GetMapping("/batches")
    @RequirePermission("salary:batch:view")
    public Result<List<SalaryBatch>> batches() {
        return Result.success(salaryBatchService.listBatches());
    }

    /** 批次内薪资记录（仅 HR/财务可见） */
    @GetMapping("/batches/{id}/records")
    @RequirePermission("salary:batch:view")
    public Result<List<SalaryRecord>> batchRecords(@PathVariable Long id) {
        return Result.success(salaryBatchService.batchRecords(id));
    }

    /** 提交批次审批（HR） */
    @PostMapping("/batches/{id}/submit")
    @RequirePermission("salary:calc:calc")
    public Result<Void> submitBatch(@PathVariable Long id) {
        salaryBatchService.submitBatch(id);
        return Result.success();
    }

    /** 审批薪资批次（财务专员） */
    @PostMapping("/batches/{id}/approve")
    @RequirePermission("salary:calc:approve")
    public Result<Void> approveBatch(@PathVariable Long id, @RequestBody ApprovalActionDTO dto) {
        salaryBatchService.approveBatch(id, dto);
        return Result.success();
    }

    // ═══════════════ 薪资记录查询 ═══════════════

    /**
     * 查询薪资记录列表
     * - HR专员/财务专员：全公司
     * - 部门主管：本部门
     * - 普通员工：仅本人
     */
    @GetMapping("/records")
    @RequirePermission({"salary:calc:view", "salary:calc:calc"})
    public Result<List<SalaryRecord>> listRecords(
            @RequestParam(required = false) Long employeeId,
            @RequestParam int year,
            @RequestParam int month) {
        List<SalaryRecord> records;
        if (employeeId != null) {
            SalaryRecord record = salaryRecordMapper.selectByEmployeeAndMonth(employeeId, year, month);
            records = record != null ? List.of(record) : List.of();
        } else {
            // TODO: 根据当前登录用户权限范围，查询对应部门或全公司的记录
            records = List.of();
        }
        return Result.success(records);
    }

    /**
     * 查询员工年度薪资记录（用于个税累计预扣、年度汇总）
     */
    @GetMapping("/records/yearly")
    @RequirePermission("salary:calc:view")
    public Result<List<SalaryRecord>> yearlyRecords(
            @RequestParam Long employeeId,
            @RequestParam int year) {
        List<SalaryRecord> records = salaryRecordMapper.selectByEmployeeAndYear(employeeId, year);
        return Result.success(records);
    }

    /**
     * 查看工资条详情（需二次验证：密码/验证码）
     * 员工只能查看本人，HR/财务可查看他人
     */
    @GetMapping("/records/{id}")
    @RequirePermission("salary:calc:view")
    public Result<SalaryRecord> detail(@PathVariable Long id) {
        // TODO: 根据权限范围判断是否允许查看
        // TODO: 员工本人查看需二次验证（密码/验证码），由前端触发
        // 此处暂时返回记录
        return Result.success(null);
    }

    // ═══════════════ 薪资确认 ═══════════════

    /**
     * 确认薪资记录（DRAFT → CONFIRMED）
     */
    @PutMapping("/records/{id}/confirm")
    @RequirePermission("salary:calc:approve")
    public Result<Void> confirm(@PathVariable Long id) {
        salaryRecordMapper.updateStatus(id, "CONFIRMED");
        return Result.success();
    }

    /**
     * 标记薪资已发放（CONFIRMED → PAID）
     */
    @PutMapping("/records/{id}/paid")
    @RequirePermission("salary:calc:approve")
    public Result<Void> markPaid(@PathVariable Long id) {
        salaryRecordMapper.updateStatus(id, "PAID");
        return Result.success();
    }
}
