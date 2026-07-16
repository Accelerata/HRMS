package com.hrms.service;

import com.hrms.entity.Employee;
import com.hrms.enums.EmployeeStatusEnum;
import com.hrms.mapper.EmployeeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * 年假年度自动发放任务
 *
 * 每年 1 月 1 日 02:00 遍历全体在职员工，初始化当年年假余额（幂等 upsert）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AnnualLeaveAccrualTask {

    private final EmployeeMapper employeeMapper;
    private final LeaveService leaveService;

    /**
     * 每年 1 月 1 日 02:00 执行年假发放
     */
    @Scheduled(cron = "0 0 2 1 1 ?")
    public void accrueAnnualLeave() {
        int currentYear = LocalDate.now().getYear();
        log.info("定时任务: 年度年假发放开始, year={}", currentYear);

        List<Employee> allEmployees = employeeMapper.selectAll();
        int successCount = 0;
        int skipCount = 0;

        for (Employee emp : allEmployees) {
            // 仅在职员工（试用期/正式）发放
            if (emp.getStatus() == null) continue;
            EmployeeStatusEnum status = EmployeeStatusEnum.fromCode(emp.getStatus());
            if (status != EmployeeStatusEnum.PROBATION && status != EmployeeStatusEnum.REGULAR) {
                skipCount++;
                continue;
            }
            try {
                leaveService.initAnnualLeaveBalance(emp.getId(), emp.getEntryDate(), currentYear);
                successCount++;
            } catch (Exception e) {
                log.error("年假发放失败: employeeId={}, name={}, error={}",
                        emp.getId(), emp.getName(), e.getMessage());
            }
        }

        log.info("定时任务: 年度年假发放完成, 成功={}, 跳过(非在职)={}, 总计={}",
                successCount, skipCount, allEmployees.size());
    }
}
