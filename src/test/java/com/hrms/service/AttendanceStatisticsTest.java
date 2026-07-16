package com.hrms.service;

import com.hrms.mapper.AttendanceRecordMapper;
import com.hrms.mapper.EmployeeMapper;
import com.hrms.entity.AttendanceRecord;
import com.hrms.entity.Employee;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 考勤统计测试 (任务 6.9)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AttendanceStatisticsService 考勤统计测试")
class AttendanceStatisticsTest {

    @Mock
    private AttendanceRecordMapper attendanceRecordMapper;

    @Mock
    private EmployeeMapper employeeMapper;

    @InjectMocks
    private AttendanceStatisticsService statisticsService;

    @Test
    @DisplayName("6.6 个人统计 → 正常+迟到+早退+缺卡")
    void shouldCalculatePersonalStats() {
        List<AttendanceRecord> records = new ArrayList<>();

        // Day 1: Normal
        AttendanceRecord r1 = new AttendanceRecord();
        r1.setEmployeeId(1L);
        r1.setAttendanceDate(LocalDate.of(2026, 7, 1));
        r1.setPunchInStatus("NORMAL");
        r1.setPunchOutStatus("NORMAL");
        records.add(r1);

        // Day 2: Late + Normal out
        AttendanceRecord r2 = new AttendanceRecord();
        r2.setEmployeeId(1L);
        r2.setAttendanceDate(LocalDate.of(2026, 7, 2));
        r2.setPunchInStatus("LATE");
        r2.setPunchOutStatus("NORMAL");
        records.add(r2);

        // Day 3: Early leave
        AttendanceRecord r3 = new AttendanceRecord();
        r3.setEmployeeId(1L);
        r3.setAttendanceDate(LocalDate.of(2026, 7, 3));
        r3.setPunchInStatus("NORMAL");
        r3.setPunchOutStatus("EARLY");
        records.add(r3);

        when(attendanceRecordMapper.selectByEmployeeAndMonth(1L, 2026, 7))
                .thenReturn(records);

        AttendanceStatisticsService.PersonalStats stats =
                statisticsService.personalMonthlyStats(1L, 2026, 7);

        assertEquals(1L, stats.employeeId);
        assertEquals(BigDecimal.valueOf(3), stats.totalWorkDays);
        assertEquals(1, stats.normalDays);
        assertEquals(1, stats.lateDays);
        assertEquals(1, stats.earlyDays);
    }

    @Test
    @DisplayName("6.6 个人统计 → 无打卡记录")
    void shouldReturnZeroStatsWhenNoRecords() {
        when(attendanceRecordMapper.selectByEmployeeAndMonth(1L, 2026, 7))
                .thenReturn(List.of());

        AttendanceStatisticsService.PersonalStats stats =
                statisticsService.personalMonthlyStats(1L, 2026, 7);

        assertEquals(BigDecimal.ZERO, stats.totalWorkDays);
        assertEquals(0, stats.normalDays);
    }

    @Test
    @DisplayName("6.7 部门统计 → 汇总多员工数据")
    void shouldCalculateDeptStats() {
        List<AttendanceRecord> records = new ArrayList<>();

        AttendanceRecord r1 = new AttendanceRecord();
        r1.setEmployeeId(1L);
        r1.setAttendanceDate(LocalDate.of(2026, 7, 1));
        r1.setPunchInStatus("NORMAL");
        r1.setPunchOutStatus("NORMAL");
        records.add(r1);

        AttendanceRecord r2 = new AttendanceRecord();
        r2.setEmployeeId(2L);
        r2.setAttendanceDate(LocalDate.of(2026, 7, 1));
        r2.setPunchInStatus("LATE");
        r2.setPunchOutStatus("NORMAL");
        records.add(r2);

        when(attendanceRecordMapper.selectByDeptAndDateRange(
                eq(1L), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(records);
        when(employeeMapper.selectByDeptId(1L)).thenReturn(List.of(new Employee(), new Employee()));

        AttendanceStatisticsService.DeptStats stats =
                statisticsService.deptMonthlyStats(1L, 2026, 7);

        assertEquals(1L, stats.deptId);
        assertEquals(2, stats.totalEmployees);
        assertEquals(1, stats.lateDays);
        assertNotNull(stats.deptAttendanceRate);
    }
}
