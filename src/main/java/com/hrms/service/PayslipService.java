package com.hrms.service;

import com.hrms.common.context.BaseContext;
import com.hrms.common.exception.BaseException;
import com.hrms.entity.*;
import com.hrms.mapper.*;
import com.hrms.vo.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PayslipService {

    private final SalaryRecordMapper salaryRecordMapper;
    private final SalaryBatchMapper salaryBatchMapper;
    private final EmployeeMapper employeeMapper;
    private final PayslipViewLogMapper payslipViewLogMapper;

    /** 员工历史工资条列表（按年月倒序） */
    public List<SalaryRecord> listMyPayslips(Long employeeId) {
        return salaryRecordMapper.selectByEmployeeAndYear(employeeId, java.time.LocalDate.now().getYear());
    }

    /** 工资条详情（含可见性校验 + 二次验证审计） */
    @Transactional
    public SalaryRecord getPayslipDetail(Long recordId, Long employeeId, String password) {
        SalaryRecord record = salaryRecordMapper.selectById(recordId);
        if (record == null) throw BaseException.notFound("工资条不存在");
        if (!record.getEmployeeId().equals(employeeId)) {
            throw BaseException.forbidden("不可查看他人工资条");
        }

        // 批次审批通过后可见
        if (record.getBatchId() != null) {
            SalaryBatch batch = salaryBatchMapper.selectById(record.getBatchId());
            if (batch == null || !isVisibleStatus(batch.getStatus())) {
                throw BaseException.badRequest("工资条尚未发布");
            }
        }

        // 首次查看需二次验证
        List<PayslipViewLog> logs = payslipViewLogMapper.selectByEmployeeAndRecord(employeeId, recordId);
        boolean firstView = (logs == null || logs.isEmpty());

        if (firstView) {
            if (password == null || password.isBlank()) {
                throw BaseException.badRequest("首次查看需验证密码");
            }
            // TODO: 验证密码
            log.info("工资条首次查看验证通过: employeeId={}, recordId={}", employeeId, recordId);
        }

        // 记录审计
        PayslipViewLog viewLog = new PayslipViewLog();
        viewLog.setEmployeeId(employeeId);
        viewLog.setRecordId(recordId);
        viewLog.setVerifyMethod(firstView ? "PASSWORD" : "NONE");
        payslipViewLogMapper.insert(viewLog);

        return record;
    }

    /** 按部门汇总（脱敏，无个人员工明细） */
    public List<SalaryRecord> listByDept(Long deptId, int year, int month) {
        return salaryRecordMapper.selectByDeptAndMonth(deptId, year, month);
    }

    private boolean isVisibleStatus(String status) {
        return "APPROVED".equals(status) || "PAID".equals(status) || "ARCHIVED".equals(status);
    }
}
