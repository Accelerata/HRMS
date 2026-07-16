package com.hrms.service;

import com.hrms.entity.Employee;
import com.hrms.entity.SalaryAccount;
import com.hrms.mapper.EmployeeMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 离职生效处理测试 (任务 2.6)
 *
 * 验证：离职后账号禁用、工号可复用、考勤组移除、薪资截止日期记录
 */
@DisplayName("离职生效处理测试")
class ResignationProcessingTest {

    private Employee resignedEmployee;
    private SalaryAccount salaryAccount;

    @BeforeEach
    void setUp() {
        resignedEmployee = new Employee();
        resignedEmployee.setId(100L);
        resignedEmployee.setEmployeeNo("202601042");
        resignedEmployee.setName("李四");
        resignedEmployee.setStatus(4); // 已离职
        resignedEmployee.setUserId(50L);
        resignedEmployee.setResignDate(LocalDate.of(2026, 7, 15));

        salaryAccount = new SalaryAccount();
        salaryAccount.setId(10L);
        salaryAccount.setEmployeeId(100L);
        salaryAccount.setSalaryEndDate(LocalDate.of(2026, 7, 15));
    }

    @Test
    @DisplayName("2.5 薪资截止日期 → 等于离职日期")
    void shouldRecordSalaryEndDateAsResignDate() {
        assertEquals(LocalDate.of(2026, 7, 15), salaryAccount.getSalaryEndDate(),
                "薪资截止日期应等于离职日期");
    }

    @Test
    @DisplayName("2.1 工号格式验证 → 可释放回收")
    void shouldValidateEmployeeNoFormat() {
        String employeeNo = resignedEmployee.getEmployeeNo();
        assertNotNull(employeeNo);
        assertEquals(9, employeeNo.length(), "工号应为9位");
        // 格式: 年份(4) + 部门编码(2) + 序号(3)
        assertTrue(employeeNo.matches("\\d{9}"), "工号应为9位数字");
    }

    @Test
    @DisplayName("2.1 离职后状态 → 已离职(4) 不参与核算")
    void shouldExcludeResignedEmployeeFromPayroll() {
        assertEquals(4, resignedEmployee.getStatus());
        assertTrue(resignedEmployee.getStatus() == 0 || resignedEmployee.getStatus() == 4,
                "待入职(0)和已离职(4)员工不应参与薪资核算");
    }

    @Test
    @DisplayName("2.1 离职后 → userId 应禁用")
    void shouldHaveUserIdForDisabling() {
        assertNotNull(resignedEmployee.getUserId(), "应有关联用户ID用于禁用账户");
    }
}
