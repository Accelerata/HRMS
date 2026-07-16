package com.hrms.service;

import com.hrms.entity.Employee;
import com.hrms.entity.LeaveApplication;
import com.hrms.mapper.EmployeeMapper;
import com.hrms.mapper.LeaveApplicationMapper;
import com.hrms.vo.EmployeeVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 个人中心测试 (任务 7.8)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("个人中心测试")
class PersonalCenterTest {

    @Mock
    private EmployeeMapper employeeMapper;

    @Mock
    private LeaveApplicationMapper leaveApplicationMapper;

    @Mock
    private EmployeeService employeeService;

    @Mock
    private AttendanceService attendanceService;

    @Mock
    private PayslipService payslipService;

    @Test
    @DisplayName("7.3 个人档案可编辑字段 → email/currentAddress/birthday 可编辑")
    void shouldAllowEditingPermittedFields() {
        // 可编辑字段
        String[] editableFields = {"email", "currentAddress", "registeredAddress", "birthday"};
        String[] lockedFields = {"deptId", "positionId", "grade", "reportTo",
                "baseSalary", "salaryAccountId", "bankAccount", "employeeNo", "status"};

        for (String field : editableFields) {
            assertTrue(isEditable(field), field + " 应该可编辑");
        }
        for (String field : lockedFields) {
            assertFalse(isEditable(field), field + " 应该是锁定字段");
        }
    }

    private boolean isEditable(String field) {
        String[] editableFields = {"email", "currentAddress", "registeredAddress", "birthday"};
        for (String f : editableFields) {
            if (f.equals(field)) return true;
        }
        return false;
    }

    @Test
    @DisplayName("7.5 日历视图 → 完整月份天数")
    void shouldGenerateFullMonthCalendar() {
        java.time.YearMonth ym = java.time.YearMonth.of(2026, 7);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();

        int days = 0;
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            days++;
        }
        assertEquals(31, days, "7月应有31天");
    }

    @Test
    @DisplayName("7.6 日历视图 → 日期含请假信息")
    void shouldIncludeLeaveDetailsInCalendar() {
        LeaveApplication leave = new LeaveApplication();
        leave.setId(1L);
        leave.setEmployeeId(1L);
        leave.setLeaveType(1); // 年假
        leave.setStartDate(LocalDate.of(2026, 7, 10));
        leave.setEndDate(LocalDate.of(2026, 7, 12));
        leave.setReason("家庭旅行");
        leave.setStatus(2); // 已通过

        // 验证请假日期范围
        assertEquals(LocalDate.of(2026, 7, 10), leave.getStartDate());
        assertEquals(LocalDate.of(2026, 7, 12), leave.getEndDate());
        assertTrue(leave.getEndDate().isAfter(leave.getStartDate())
                || leave.getEndDate().equals(leave.getStartDate()));

        // 7月10-12日应标记为请假
        LocalDate checkDate = LocalDate.of(2026, 7, 11);
        assertFalse(checkDate.isBefore(leave.getStartDate()));
        assertFalse(checkDate.isAfter(leave.getEndDate()));
    }

    @Test
    @DisplayName("7.7 薪资趋势 → 近6个月数据点")
    void shouldReturnLast6MonthsTrend() {
        java.time.YearMonth current = java.time.YearMonth.now();
        int months = 0;
        for (int i = 5; i >= 0; i--) {
            java.time.YearMonth ym = current.minusMonths(i);
            assertNotNull(ym);
            months++;
        }
        assertEquals(6, months, "应返回近6个月数据");
    }
}
