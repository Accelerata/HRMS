package com.hrms.service;

import com.hrms.entity.*;
import com.hrms.mapper.ApprovalRecordMapper;
import com.hrms.mapper.DepartmentMapper;
import com.hrms.mapper.EmployeeMapper;
import com.hrms.mapper.SysUserMapper;
import com.hrms.enums.EmployeeStatusEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * 员工生命周期定时任务调度器
 *
 * 职责：
 * 1. 试用期到期提醒（每天 08:00）
 * 2. 待离职自动过渡（每天 00:30）
 * 3. 审批超时催办（每小时）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LifecycleScheduler {

    private final EmployeeMapper employeeMapper;
    private final ApprovalRecordMapper approvalRecordMapper;
    private final DepartmentMapper departmentMapper;
    private final SysUserMapper sysUserMapper;
    private final NotificationService notificationService;
    private final EmployeeAccountService employeeAccountService;

    /**
     * 试用期到期提醒 — 每天 08:00
     */
    @Scheduled(cron = "0 0 8 * * ?")
    public void scanProbationReminders() {
        log.info("定时任务: 试用期到期提醒扫描开始");
        List<Employee> employees = employeeMapper.selectProbationForRegularizationReminder(7);
        for (Employee emp : employees) {
            if (emp.getRegularDate() == null) continue;
            long daysRemaining = emp.getRegularDate().toEpochDay() - LocalDate.now().toEpochDay();
            String title = "转正提醒: 员工试用期即将到期";
            String content = String.format("员工 %s（工号: %s）试用期将于 %s 到期（剩余 %d 天），请及时发起转正流程。",
                    emp.getName(), emp.getEmployeeNo(), emp.getRegularDate(), daysRemaining);

            // 通知部门主管
            if (emp.getDeptId() != null) {
                Department dept = departmentMapper.selectById(emp.getDeptId());
                if (dept != null && dept.getManagerId() != null) {
                    Employee manager = employeeMapper.selectById(dept.getManagerId());
                    if (manager != null && manager.getUserId() != null) {
                        notificationService.sendReminder(manager.getUserId(), title, content, null, emp.getId());
                    }
                }
            }

            // 通知HR
            SysUser hr = sysUserMapper.findFirstByRoleCode("ROLE_HR");
            if (hr != null) {
                notificationService.sendReminder(hr.getId(), title, content, null, emp.getId());
            }
        }
        log.info("定时任务: 试用期到期提醒扫描完成, 共{}条", employees.size());
    }

    /**
     * 待离职自动过渡 — 每天 00:30
     */
    @Scheduled(cron = "0 30 0 * * ?")
    public void processPendingResignations() {
        log.info("定时任务: 待离职自动过渡扫描开始");
        List<Employee> employees = employeeMapper.selectPendingResignByDate(LocalDate.now());
        for (Employee emp : employees) {
            try {
                if (emp.getStatus() == null || emp.getStatus() != EmployeeStatusEnum.PENDING_RESIGN.getCode()) {
                    continue;
                }

                // 禁用账号
                if (emp.getUserId() != null) {
                    employeeAccountService.disableAccount(emp.getUserId());
                }

                // 更新为已离职
                emp.setStatus(EmployeeStatusEnum.RESIGNED.getCode());
                employeeMapper.update(emp);

                log.info("离职过渡完成: employeeId={}, name={}", emp.getId(), emp.getName());
            } catch (Exception e) {
                log.error("离职过渡失败: employeeId={}", emp.getId(), e);
            }
        }
        log.info("定时任务: 待离职自动过渡扫描完成, 共{}条", employees.size());
    }

    /**
     * 审批超时催办 — 每小时
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void escalateOverdueApprovals() {
        log.info("定时任务: 审批超时催办扫描开始");
        List<ApprovalRecord> overdueRecords = approvalRecordMapper.selectOverduePending();
        for (ApprovalRecord record : overdueRecords) {
            try {
                String title = "审批催办: 审批超时未处理";
                String content = String.format("您的审批任务（业务类型: %d, 业务ID: %d, 步骤: %d）已超时，截止时间: %s，请尽快处理。",
                        record.getBusinessType(), record.getBusinessId(),
                        record.getStepOrder(), record.getDueTime());

                notificationService.sendWarning(record.getApproverId(), title, content,
                        record.getBusinessType(), record.getBusinessId());
            } catch (Exception e) {
                log.error("催办通知发送失败: recordId={}", record.getId(), e);
            }
        }
        log.info("定时任务: 审批超时催办扫描完成, 共{}条", overdueRecords.size());
    }
}
