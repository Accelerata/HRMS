package com.hrms.service;

import com.hrms.entity.Employee;
import com.hrms.mapper.EmployeeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 调休月度折算定时任务
 *
 * 每月 1 日 01:00 批量对全体员工执行加班→调休折算入账
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CompLeaveOvertimeTask {

    private final EmployeeMapper employeeMapper;
    private final CompLeaveService compLeaveService;

    /**
     * 每月 1 日 01:00 执行月度折算
     */
    @Scheduled(cron = "0 0 1 1 * ?")
    public void monthlyConvert() {
        log.info("定时任务: 月度加班折算调休开始");
        java.util.List<Employee> allEmployees = employeeMapper.selectAll();
        int count = compLeaveService.convertAllEmployees(allEmployees);
        log.info("定时任务: 月度加班折算调休完成, 本次入账{}人", count);
    }
}
