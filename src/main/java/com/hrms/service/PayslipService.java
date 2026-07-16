package com.hrms.service;

import com.hrms.common.context.BaseContext;
import com.hrms.common.exception.BaseException;
import com.hrms.entity.*;
import com.hrms.mapper.*;
import com.hrms.utils.PasswordUtil;
import com.hrms.vo.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PayslipService {

    private final SalaryRecordMapper salaryRecordMapper;
    private final SalaryBatchMapper salaryBatchMapper;
    private final EmployeeMapper employeeMapper;
    private final PayslipViewLogMapper payslipViewLogMapper;
    private final SysUserMapper sysUserMapper;

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
            // 验证密码：通过 SysUser 查询当前登录用户的 BCrypt 密码比对
            Long userId = BaseContext.getCurrentUserId();
            if (userId == null) {
                throw new BaseException(401, "未登录");
            }
            SysUser user = sysUserMapper.findById(userId);
            if (user == null || !PasswordUtil.matches(password, user.getPassword())) {
                throw BaseException.badRequest("密码验证失败");
            }
            log.info("工资条首次查看密码验证通过: employeeId={}, recordId={}", employeeId, recordId);
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

    /** 获取员工某个月份的工资条（供个人中心薪资趋势调用） */
    public Map<String, Object> getPayslipForMonth(Long employeeId, java.time.YearMonth yearMonth) {
        int year = yearMonth.getYear();
        int month = yearMonth.getMonthValue();
        SalaryRecord record = salaryRecordMapper.selectByEmployeeAndMonth(employeeId, year, month);
        if (record == null) return null;
        Map<String, Object> result = new HashMap<>();
        result.put("netPay", record.getNetPay());
        result.put("grossPay", record.getGrossPay());
        result.put("yearMonth", yearMonth.toString());
        return result;
    }
}
