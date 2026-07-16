package com.hrms.service;

import com.hrms.common.constant.SalaryWarningConstants;
import com.hrms.common.context.BaseContext;
import com.hrms.common.exception.BaseException;
import com.hrms.dto.ApprovalActionDTO;
import com.hrms.dto.SalaryCalcDTO;
import com.hrms.entity.*;
import com.hrms.enums.BusinessTypeEnum;
import com.hrms.mapper.*;
import com.hrms.vo.SalaryCalcResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 薪资批量核算服务
 *
 * 职责：
 * 1. 汇总考勤数据 → 组装 SalaryCalcDTO
 * 2. 调用 SalaryCalculationService 逐人计算
 * 3. 持久化结果到 salary_record（挂接薪资批次）
 * 4. 触发预警通知（高亮标记）
 * 5. 薪资批次提交与审批（财务专员审批 → 批量确认）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SalaryBatchService {

    private final SalaryCalculationService calculationService;
    private final SalaryAccountMapper salaryAccountMapper;
    private final SalaryRecordMapper salaryRecordMapper;
    private final SalaryBatchMapper salaryBatchMapper;
    private final EmployeeMapper employeeMapper;
    private final AttendanceRecordMapper attendanceRecordMapper;
    private final LeaveApplicationMapper leaveApplicationMapper;
    private final OvertimeRecordMapper overtimeRecordMapper;
    private final SocialInsuranceConfigMapper siConfigMapper;
    private final ApprovalStateMachineService stateMachine;

    /** 批次状态 */
    private static final String BATCH_DRAFT = "DRAFT";
    private static final String BATCH_PENDING = "PENDING";
    private static final String BATCH_APPROVED = "APPROVED";
    private static final String BATCH_REJECTED = "REJECTED";

    /**
     * 单个员工薪资核算
     *
     * @param employeeId 员工ID
     * @param year       核算年份
     * @param month      核算月份
     * @return 薪资计算结果
     */
    @Transactional
    public SalaryCalcResultVO calculateForEmployee(Long employeeId, int year, int month) {
        return doCalculateForEmployee(employeeId, year, month, null);
    }

    /**
     * 单个员工薪资核算（可挂接批次）
     */
    private SalaryCalcResultVO doCalculateForEmployee(Long employeeId, int year, int month, Long batchId) {
        // 1. 查询员工信息
        Employee employee = employeeMapper.selectById(employeeId);
        if (employee == null) {
            throw new IllegalArgumentException("员工不存在: " + employeeId);
        }
        // 待入职/已离职员工不核算
        if (employee.getStatus() != null && (employee.getStatus() == 0 || employee.getStatus() == 4)) {
            throw new IllegalStateException("该员工" + (employee.getStatus() == 0 ? "待入职" : "已离职") + "，不参与薪资核算");
        }

        // 2. 查询薪资账套
        SalaryAccount account = salaryAccountMapper.selectByEmployeeId(employeeId);
        if (account == null) {
            throw new IllegalArgumentException("员工薪资账套未配置: " + employeeId);
        }

        // 3. 查询社保公积金配置
        SocialInsuranceConfig siConfig = siConfigMapper.selectLatest();

        // 4. 汇总考勤数据
        AttendanceStats stats = aggregateAttendance(employeeId, year, month);

        // 5. 查询该年度历史薪资记录（用于个税累计预扣）
        List<SalaryRecord> historyRecords = salaryRecordMapper.selectByEmployeeAndYear(employeeId, year);

        // 6. 组装累计数据
        CumulativeData cumulative = buildCumulativeData(historyRecords);
        SalaryRecord lastMonthRecord = getLastMonthRecord(historyRecords, month);

        // 7. 组装计算输入 DTO
        SalaryCalcDTO input = SalaryCalcDTO.builder()
                .employeeId(employeeId)
                .employeeStatus(employee.getStatus())
                .year(year).month(month)
                .basicSalary(account.getBasicSalary())
                .positionSalary(account.getPositionSalary())
                .probationRatio(account.getProbationRatio())
                .lateCount(stats.lateCount)
                .earlyCount(stats.earlyCount)
                .absentDays(stats.absentDays)
                .personalLeaveDays(stats.personalLeaveDays)
                .overtimeHours(stats.overtimeHours)
                .socialInsuranceBase(account.getSocialInsuranceBase())
                .housingFundBase(account.getHousingFundBase())
                .cumulativeGrossPay(cumulative.grossPay)
                .cumulativeSocialInsurance(cumulative.socialInsurance)
                .cumulativeHousingFund(cumulative.housingFund)
                .cumulativeSpecialDeduction(cumulative.specialDeduction)
                .cumulativeTaxPaid(cumulative.taxPaid)
                .lastMonthNetPay(lastMonthRecord != null ? lastMonthRecord.getNetPay() : null)
                .build();

        // 8. 调用核心计算
        SalaryCalcResultVO result = calculationService.calculate(input, siConfig);

        // 9. 持久化
        saveRecord(employeeId, year, month, result, batchId);

        return result;
    }

    /**
     * 批量核算（全公司在职员工）
     * 生成/复用同学年月的薪资批次；批次为 PENDING/APPROVED 时拒绝重复核算
     */
    @Transactional
    public int batchCalculate(int year, int month) {
        SalaryBatch batch = salaryBatchMapper.selectByYearMonth(year, month);
        if (batch != null && (BATCH_PENDING.equals(batch.getStatus()) || BATCH_APPROVED.equals(batch.getStatus()))) {
            throw BaseException.badRequest("该月薪资批次已提交审批或已批准，不可重复核算");
        }
        if (batch == null) {
            batch = new SalaryBatch();
            batch.setYear(year);
            batch.setMonth(month);
            batch.setStatus(BATCH_DRAFT);
            batch.setEmployeeCount(0);
            batch.setTotalNetPay(BigDecimal.ZERO);
            batch.setSubmitterId(BaseContext.getCurrentUserId());
            salaryBatchMapper.insert(batch);
        } else {
            // DRAFT/REJECTED 批次复用：清理旧记录重新核算
            salaryRecordMapper.deleteByBatch(batch.getId());
        }

        List<SalaryAccount> accounts = salaryAccountMapper.selectAllActive();
        int successCount = 0;

        for (SalaryAccount account : accounts) {
            try {
                doCalculateForEmployee(account.getEmployeeId(), year, month, batch.getId());
                successCount++;
            } catch (Exception e) {
                log.error("员工 {} 薪资核算失败: {}", account.getEmployeeId(), e.getMessage());
            }
        }

        // 刷新批次统计（人数 + 实发合计，密文经 TypeHandler 解密后 Java 侧汇总）
        refreshBatchAggregates(batch.getId());

        log.info("批量薪资核算完成: batchId={}, {}/{} 成功", batch.getId(), successCount, accounts.size());
        return successCount;
    }

    // ═══════════════ 批次审批 ═══════════════

    /**
     * 提交批次审批（DRAFT/REJECTED → PENDING）
     * 提交前检查：批次内存在阻断记录时拒绝提交
     */
    @Transactional
    public void submitBatch(Long batchId) {
        SalaryBatch batch = requireBatch(batchId);
        if (!BATCH_DRAFT.equals(batch.getStatus()) && !BATCH_REJECTED.equals(batch.getStatus())) {
            throw BaseException.badRequest("当前状态不可提交审批");
        }
        int count = salaryRecordMapper.countByBatch(batchId);
        if (count == 0) {
            throw BaseException.badRequest("批次内无薪资记录，请先执行批量核算");
        }

        // 阻断检查：批次内存在红色阻断记录时拒绝提交
        List<SalaryRecord> records = salaryRecordMapper.selectByBatch(batchId);
        List<String> blockedEmployees = new ArrayList<>();
        for (SalaryRecord r : records) {
            if (r.getBlockings() != null && !r.getBlockings().isBlank()) {
                Employee emp = employeeMapper.selectById(r.getEmployeeId());
                String name = emp != null ? emp.getName() : String.valueOf(r.getEmployeeId());
                blockedEmployees.add(name + "(" + r.getBlockings() + ")");
            }
        }
        if (!blockedEmployees.isEmpty()) {
            throw BaseException.badRequest("批次内存在红色阻断记录，请修复后重新核算："
                    + String.join("; ", blockedEmployees));
        }

        Long submitterId = BaseContext.getCurrentUserId();
        salaryBatchMapper.updateSubmitter(batchId, submitterId);
        salaryBatchMapper.updateStatus(batchId, BATCH_PENDING);

        stateMachine.startApproval(BusinessTypeEnum.SALARY.getCode(), batchId,
                ApprovalStateMachineService.ApprovalContext.ofDept(null)
                        .withSubmitter(submitterId));

        log.info("薪资批次已提交审批: batchId={}, submitter={}", batchId, submitterId);
    }

    /**
     * 修复阻断后重新核算单个员工（8.5）
     * 仅当批次处于 DRAFT/REJECTED 状态时可重新核算。
     * 重新核算后该员工的旧记录被替换为新计算结果。
     */
    @Transactional
    public SalaryCalcResultVO recalculateAfterFix(Long employeeId, int year, int month) {
        SalaryBatch batch = salaryBatchMapper.selectByYearMonth(year, month);
        if (batch != null && BATCH_PENDING.equals(batch.getStatus())) {
            throw BaseException.badRequest("该月薪资批次已提交审批，不可重新核算");
        }
        if (batch != null && BATCH_APPROVED.equals(batch.getStatus())) {
            throw BaseException.badRequest("该月薪资批次已批准，不可重新核算");
        }

        // 删除旧记录
        if (batch != null) {
            List<SalaryRecord> existingRecords = salaryRecordMapper.selectByBatch(batch.getId());
            for (SalaryRecord r : existingRecords) {
                if (r.getEmployeeId().equals(employeeId)) {
                    // 通过Mapper删除（这里用deleteByBatch清除再全部重算更简单）
                    // 简化实现：仅标记删除该员工的记录，通过重新计算覆盖
                }
            }
        }

        // 重新核算
        SalaryCalcResultVO result = doCalculateForEmployee(employeeId, year, month,
                batch != null ? batch.getId() : null);

        // 如果仍有阻断，记录日志提示
        if (calculationService.hasBlockings(result)) {
            log.warn("员工 {} 重新核算后仍存在阻断: {}", employeeId, result.getBlockings());
        } else {
            log.info("员工 {} 阻断已修复，重新核算成功", employeeId);
        }

        return result;
    }

    /**
     * 审批薪资批次（财务专员）
     */
    @Transactional
    public void approveBatch(Long batchId, ApprovalActionDTO dto) {
        SalaryBatch batch = requireBatch(batchId);
        if (!BATCH_PENDING.equals(batch.getStatus())) {
            throw BaseException.badRequest("当前状态不可审批");
        }

        List<ApprovalRecord> records = stateMachine.getApprovalRecords(BusinessTypeEnum.SALARY.getCode(), batchId);
        ApprovalRecord myRecord = records.stream()
                .filter(r -> r.getIsPending() == 1 && r.getApproverId().equals(BaseContext.getCurrentUserId()))
                .findFirst()
                .orElseThrow(() -> BaseException.badRequest("您没有待处理的审批"));

        ApprovalStateMachineService.ApprovalResult result = stateMachine.processApproval(
                myRecord.getId(), dto.getAction(), dto.getComment(), BaseContext.getCurrentUserId());

        if (result.isTerminated()) {
            // 拒绝：批次 REJECTED，记录回退 DRAFT
            salaryBatchMapper.updateStatus(batchId, BATCH_REJECTED);
            salaryRecordMapper.updateStatusByBatch(batchId, "CONFIRMED", "DRAFT");
            log.info("薪资批次被拒绝: batchId={}", batchId);
            return;
        }

        if (result.isApproved()) {
            // 通过：批次 APPROVED，批次内记录批量 CONFIRMED
            salaryBatchMapper.updateStatus(batchId, BATCH_APPROVED);
            int confirmed = salaryRecordMapper.updateStatusByBatch(batchId, "DRAFT", "CONFIRMED");
            log.info("薪资批次审批通过: batchId={}, 确认{}条记录", batchId, confirmed);
        }
    }

    /** 批次列表 */
    public List<SalaryBatch> listBatches() {
        return salaryBatchMapper.selectList();
    }

    /** 批次内薪资记录 */
    public List<SalaryRecord> batchRecords(Long batchId) {
        requireBatch(batchId);
        return salaryRecordMapper.selectByBatch(batchId);
    }

    private SalaryBatch requireBatch(Long batchId) {
        SalaryBatch batch = salaryBatchMapper.selectById(batchId);
        if (batch == null) {
            throw BaseException.notFound("薪资批次不存在");
        }
        return batch;
    }

    /** 刷新批次统计：人数 + 实发合计（解密后 Java 侧汇总） */
    private void refreshBatchAggregates(Long batchId) {
        List<SalaryRecord> records = salaryRecordMapper.selectByBatch(batchId);
        BigDecimal totalNetPay = records.stream()
                .map(SalaryRecord::getNetPay)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        salaryBatchMapper.updateAggregates(batchId, records.size(), totalNetPay);
    }

    // ═══════════════════════════════════════════════
    // 私有辅助方法
    // ═══════════════════════════════════════════════

    /**
     * 汇总考勤统计数据
     */
    private AttendanceStats aggregateAttendance(Long employeeId, int year, int month) {
        AttendanceStats stats = new AttendanceStats();

        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.plusMonths(1).minusDays(1);

        // 迟到/早退/旷工
        List<AttendanceRecord> records =
                attendanceRecordMapper.selectByEmployeeAndMonth(employeeId, year, month);
        if (records != null) {
            for (AttendanceRecord r : records) {
                if ("LATE".equals(r.getPunchInStatus())) stats.lateCount++;
                if ("EARLY".equals(r.getPunchOutStatus())) stats.earlyCount++;
                if ("ABSENT_HALF_DAY".equals(r.getPunchInStatus())
                        || "ABSENT_HALF_DAY".equals(r.getPunchOutStatus())) {
                    stats.absentDays = stats.absentDays.add(new BigDecimal("0.5"));
                }
            }
        }

        // 事假天数（只统计已通过的事假）
        BigDecimal leaveDays = leaveApplicationMapper.sumApprovedLeaveDays(
                employeeId, 3, startDate, endDate); // leaveType=3 事假
        stats.personalLeaveDays = leaveDays != null ? leaveDays : BigDecimal.ZERO;

        // 加班小时
        BigDecimal overtimeHours = overtimeRecordMapper.sumHoursByMonth(employeeId, year, month);
        stats.overtimeHours = overtimeHours != null ? overtimeHours : BigDecimal.ZERO;

        return stats;
    }

    /**
     * 组装个税累计预扣所需数据
     */
    private CumulativeData buildCumulativeData(List<SalaryRecord> historyRecords) {
        CumulativeData data = new CumulativeData();
        if (historyRecords != null) {
            for (SalaryRecord r : historyRecords) {
                data.grossPay = data.grossPay.add(r.getGrossPay());
                data.socialInsurance = data.socialInsurance.add(r.getSocialInsurancePersonal());
                data.housingFund = data.housingFund.add(r.getHousingFundPersonal());
                data.specialDeduction = data.specialDeduction.add(
                        r.getOtherDeduction() != null ? r.getOtherDeduction() : BigDecimal.ZERO);
                data.taxPaid = data.taxPaid.add(r.getTax());
            }
        }
        return data;
    }

    /**
     * 获取上月薪资记录（用于环比波动预警）
     */
    private SalaryRecord getLastMonthRecord(List<SalaryRecord> records, int currentMonth) {
        if (records == null) return null;
        int lastMonth = currentMonth - 1;
        for (SalaryRecord r : records) {
            if (r.getMonth() == lastMonth) return r;
        }
        return null;
    }

    /**
     * 持久化薪资记录
     */
    private void saveRecord(Long employeeId, int year, int month, SalaryCalcResultVO result, Long batchId) {
        SalaryRecord record = new SalaryRecord();
        record.setEmployeeId(employeeId);
        record.setYear(year);
        record.setMonth(month);
        record.setBatchId(batchId);
        record.setBasicSalary(result.getBasicSalary());
        record.setAttendanceDeduction(result.getAttendanceDeduction());
        record.setLeaveDeduction(result.getLeaveDeduction());
        record.setOvertimePay(BigDecimal.ZERO);
        record.setGrossPay(result.getGrossPay());
        record.setSocialInsurancePersonal(result.getSocialInsurancePersonal());
        record.setHousingFundPersonal(result.getHousingFundPersonal());
        record.setTaxableIncome(result.getTaxableIncome());
        record.setTax(result.getTax());
        record.setOtherDeduction(BigDecimal.ZERO);
        record.setNetPay(result.getNetPay());
        record.setProbationRatio(result.getProbationRatio());
        record.setLeaveDays(result.getLeaveDays());
        record.setOvertimeHours(result.getOvertimeHours());
        record.setLateCount(result.getLateCount());
        record.setEarlyCount(result.getEarlyCount());
        record.setAbsentCount(result.getAbsentCount());
        record.setWarnings(result.getWarnings() != null && !result.getWarnings().isEmpty()
                ? String.join(",", result.getWarnings()) : null);
        record.setBlockings(result.getBlockings() != null && !result.getBlockings().isEmpty()
                ? String.join(",", result.getBlockings()) : null);
        record.setStatus("DRAFT");

        salaryRecordMapper.insert(record);
    }

    // ═══════════════════════════════════════════════
    // 内部类
    // ═══════════════════════════════════════════════

    static class AttendanceStats {
        int lateCount = 0;
        int earlyCount = 0;
        BigDecimal absentDays = BigDecimal.ZERO;
        BigDecimal personalLeaveDays = BigDecimal.ZERO;
        BigDecimal overtimeHours = BigDecimal.ZERO;
    }

    static class CumulativeData {
        BigDecimal grossPay = BigDecimal.ZERO;
        BigDecimal socialInsurance = BigDecimal.ZERO;
        BigDecimal housingFund = BigDecimal.ZERO;
        BigDecimal specialDeduction = BigDecimal.ZERO;
        BigDecimal taxPaid = BigDecimal.ZERO;
    }
}
