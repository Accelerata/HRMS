package com.hrms.controller;

import com.hrms.annotation.RequirePermission;
import com.hrms.dto.SalaryBatchCalcDTO;
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
 * 权限说明：
 * - salary:calculate  — 薪资核算（HR专员及以上）
 * - salary:view       — 查看薪资记录（HR专员、部门主管[本部门]、财务专员、员工本人）
 * - salary:confirm    — 确认薪资（HR专员及以上）
 * - salary:detail     — 查看工资条详情（需二次验证）
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
    @RequirePermission("salary:calculate")
    public Result<SalaryCalcResultVO> calculate(
            @PathVariable Long employeeId,
            @RequestParam int year,
            @RequestParam int month) {
        SalaryCalcResultVO result = salaryBatchService.calculateForEmployee(employeeId, year, month);
        return Result.success(result);
    }

    /**
     * 批量薪资核算
     * 传入 year + month，自动核算全公司（或指定部门）在职员工
     */
    @PostMapping("/batch-calculate")
    @RequirePermission("salary:calculate")
    public Result<Integer> batchCalculate(@Valid @RequestBody SalaryBatchCalcDTO dto) {
        int count = salaryBatchService.batchCalculate(dto.getYear(), dto.getMonth());
        return Result.success(count);
    }

    // ═══════════════ 薪资记录查询 ═══════════════

    /**
     * 查询薪资记录列表
     * - HR专员/财务专员：全公司
     * - 部门主管：本部门
     * - 普通员工：仅本人
     */
    @GetMapping("/records")
    @RequirePermission({"salary:view", "salary:calculate"})
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
    @RequirePermission("salary:view")
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
    @RequirePermission("salary:view")
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
    @RequirePermission("salary:confirm")
    public Result<Void> confirm(@PathVariable Long id) {
        salaryRecordMapper.updateStatus(id, "CONFIRMED");
        return Result.success();
    }

    /**
     * 标记薪资已发放（CONFIRMED → PAID）
     */
    @PutMapping("/records/{id}/paid")
    @RequirePermission("salary:confirm")
    public Result<Void> markPaid(@PathVariable Long id) {
        salaryRecordMapper.updateStatus(id, "PAID");
        return Result.success();
    }
}
