package com.hrms.controller;

import com.hrms.annotation.RequirePermission;
import com.hrms.common.context.BaseContext;
import com.hrms.dto.ApprovalActionDTO;
import com.hrms.dto.SalaryBatchCalcDTO;
import com.hrms.entity.SalaryBatch;
import com.hrms.entity.SalaryRecord;
import com.hrms.mapper.SalaryBatchMapper;
import com.hrms.mapper.SalaryRecordMapper;
import com.hrms.result.Result;
import com.hrms.service.*;
import com.hrms.vo.SalaryCalcResultVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/salary")
@RequiredArgsConstructor
public class SalaryController {

    private final SalaryBatchService salaryBatchService;
    private final SalaryRecordMapper salaryRecordMapper;
    private final SalaryBatchMapper salaryBatchMapper;
    private final PayslipService payslipService;
    private final SalaryAccessControlService accessControlService;
    private final SalaryReportService salaryReportService;

    // ═══════════════ 薪资核算 ═══════════════

    @PostMapping("/calculate/{employeeId}")
    @RequirePermission("salary:batch:calc")
    public Result<SalaryCalcResultVO> calculate(@PathVariable Long employeeId,
                                                 @RequestParam int year,
                                                 @RequestParam int month) {
        return Result.success(salaryBatchService.calculateForEmployee(employeeId, year, month));
    }

    @PostMapping("/batch-calculate")
    @RequirePermission("salary:batch:calc")
    public Result<Integer> batchCalculate(@Valid @RequestBody SalaryBatchCalcDTO dto) {
        return Result.success(salaryBatchService.batchCalculate(dto.getYear(), dto.getMonth()));
    }

    // ═══════════════ 薪资批次 ═══════════════

    @GetMapping("/batches")
    @RequirePermission("salary:batch:view")
    public Result<List<SalaryBatch>> batches() {
        return Result.success(salaryBatchService.listBatches());
    }

    @GetMapping("/batches/{id}/records")
    @RequirePermission("salary:batch:view")
    public Result<List<SalaryRecord>> batchRecords(@PathVariable Long id) {
        return Result.success(salaryBatchService.batchRecords(id));
    }

    @PostMapping("/batches/{id}/submit")
    @RequirePermission("salary:batch:submit")
    public Result<Void> submitBatch(@PathVariable Long id) {
        salaryBatchService.submitBatch(id);
        return Result.success();
    }

    @PostMapping("/batches/{id}/approve")
    @RequirePermission("salary:calc:approve")
    public Result<Void> approveBatch(@PathVariable Long id, @RequestBody ApprovalActionDTO dto) {
        salaryBatchService.approveBatch(id, dto);
        return Result.success();
    }

    /** 批次发放确认（仅 APPROVED → PAID） */
    @PostMapping("/batches/{id}/pay")
    @RequirePermission("salary:batch:pay")
    public Result<Void> payBatch(@PathVariable Long id) {
        SalaryBatch batch = salaryBatchMapper.selectById(id);
        if (batch == null || !"APPROVED".equals(batch.getStatus())) {
            return Result.error("仅已通过批次可发放");
        }
        salaryBatchMapper.updateStatus(id, "PAID");
        salaryRecordMapper.updateStatusByBatch(id, "CONFIRMED", "PAID");
        return Result.success();
    }

    /** 批次归档（仅 PAID → ARCHIVED） */
    @PostMapping("/batches/{id}/archive")
    @RequirePermission("salary:batch:archive")
    public Result<Void> archiveBatch(@PathVariable Long id) {
        SalaryBatch batch = salaryBatchMapper.selectById(id);
        if (batch == null || !"PAID".equals(batch.getStatus())) {
            return Result.error("仅已发放批次可归档");
        }
        salaryBatchMapper.updateStatus(id, "ARCHIVED");
        return Result.success();
    }

    // ═══════════════ 薪资记录查询（含数据权限） ═══════════════

    @GetMapping("/records")
    @RequirePermission("salary:calc:view")
    public Result<List<SalaryRecord>> listRecords(@RequestParam(required = false) Long employeeId,
                                                   @RequestParam int year,
                                                   @RequestParam int month) {
        if (employeeId != null) {
            accessControlService.checkEmployeeAccess(employeeId);
            SalaryRecord record = salaryRecordMapper.selectByEmployeeAndMonth(employeeId, year, month);
            return Result.success(record != null ? List.of(record) : List.of());
        }
        // 全量查询仅 HR/财务
        accessControlService.checkDeptAccess(null);
        return Result.success(salaryRecordMapper.selectByDeptAndMonth(null, year, month));
    }

    @GetMapping("/records/yearly")
    @RequirePermission("salary:payslip:self")
    public Result<List<SalaryRecord>> yearlyRecords(@RequestParam Long employeeId,
                                                     @RequestParam int year) {
        accessControlService.checkEmployeeAccess(employeeId);
        return Result.success(salaryRecordMapper.selectByEmployeeAndYear(employeeId, year));
    }

    // ═══════════════ 工资条 ═══════════════

    /** 员工本人工资条列表 */
    @GetMapping("/payslips")
    @RequirePermission("salary:payslip:self")
    public Result<List<SalaryRecord>> myPayslips() {
        Long empId = BaseContext.getCurrentEmployeeId();
        return Result.success(payslipService.listMyPayslips(empId));
    }

    /** 工资条详情（员工本人 + 首次二次验证） */
    @GetMapping("/payslips/{recordId}")
    @RequirePermission("salary:payslip:self")
    public Result<SalaryRecord> payslipDetail(@PathVariable Long recordId,
                                                @RequestParam(required = false) String password) {
        Long empId = BaseContext.getCurrentEmployeeId();
        return Result.success(payslipService.getPayslipDetail(recordId, empId, password));
    }

    // ═══════════════ 薪资报表 ═══════════════

    @GetMapping("/reports/trend")
    @RequirePermission("salary:report:view")
    public Result<List<Map<String, Object>>> monthlyTrend() {
        return Result.success(salaryReportService.monthlyTrend());
    }

    @GetMapping("/reports/dept-cost")
    @RequirePermission("salary:report:view")
    public Result<List<Map<String, Object>>> deptCost(@RequestParam int year, @RequestParam int month) {
        return Result.success(salaryReportService.deptCostDistribution(year, month));
    }

    @GetMapping("/reports/composition")
    @RequirePermission("salary:report:view")
    public Result<Map<String, java.math.BigDecimal>> composition(@RequestParam int year, @RequestParam int month) {
        return Result.success(salaryReportService.compositionPct(year, month));
    }
}
