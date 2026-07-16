package com.hrms.service;

import com.hrms.entity.AttendanceRecord;
import com.hrms.entity.Employee;
import com.hrms.mapper.AttendanceRecordMapper;
import com.hrms.mapper.EmployeeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

/**
 * 考勤统计服务
 *
 * 提供个人和部门维度的考勤数据汇总统计。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AttendanceStatisticsService {

    private final AttendanceRecordMapper attendanceRecordMapper;
    private final EmployeeMapper employeeMapper;

    /**
     * 个人月度考勤统计
     *
     * @param employeeId 员工ID
     * @param year       年份
     * @param month      月份
     * @return 统计数据
     */
    public PersonalStats personalMonthlyStats(Long employeeId, int year, int month) {
        List<AttendanceRecord> records = attendanceRecordMapper.selectByEmployeeAndMonth(
                employeeId, year, month);

        PersonalStats stats = new PersonalStats();
        stats.employeeId = employeeId;
        stats.year = year;
        stats.month = month;

        if (records == null || records.isEmpty()) {
            stats.totalWorkDays = BigDecimal.ZERO;
            stats.normalDays = 0;
            stats.lateDays = 0;
            stats.earlyDays = 0;
            stats.absentHalfDays = BigDecimal.ZERO;
            stats.missingPunchDays = 0;
            stats.attendanceRate = BigDecimal.ZERO;
            return stats;
        }

        stats.totalWorkDays = BigDecimal.valueOf(records.size());

        int normalCount = 0;
        int lateCount = 0;
        int earlyCount = 0;
        BigDecimal absentHalfCount = BigDecimal.ZERO;
        int missingCount = 0;

        for (AttendanceRecord r : records) {
            // 上班打卡状态
            String punchIn = r.getPunchInStatus();
            if ("NORMAL".equals(punchIn)) {
                // 同时检查下班打卡
                String punchOut = r.getPunchOutStatus();
                if ("NORMAL".equals(punchOut)) {
                    normalCount++;
                } else if ("EARLY".equals(punchOut)) {
                    earlyCount++;
                } else if ("ABSENT_HALF_DAY".equals(punchOut)) {
                    absentHalfCount = absentHalfCount.add(new BigDecimal("0.5"));
                } else if ("MISSING_PUNCH".equals(punchOut)) {
                    missingCount++;
                }
            } else if ("LATE".equals(punchIn)) {
                lateCount++;
                // 下班可能 NORMAL/EARLY/MISSING
                String punchOut = r.getPunchOutStatus();
                if ("EARLY".equals(punchOut)) {
                    earlyCount++;
                }
            } else if ("ABSENT_HALF_DAY".equals(punchIn)) {
                absentHalfCount = absentHalfCount.add(new BigDecimal("0.5"));
            } else if ("MISSING_PUNCH".equals(punchIn)) {
                missingCount++;
            }
        }

        stats.normalDays = normalCount;
        stats.lateDays = lateCount;
        stats.earlyDays = earlyCount;
        stats.absentHalfDays = absentHalfCount;
        stats.missingPunchDays = missingCount;

        // 出勤率 = 正常天数 / 应出勤天数
        if (stats.totalWorkDays.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal normalAndLate = BigDecimal.valueOf(normalCount + lateCount);
            stats.attendanceRate = normalAndLate
                    .divide(stats.totalWorkDays, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);
        } else {
            stats.attendanceRate = BigDecimal.ZERO;
        }

        return stats;
    }

    /**
     * 部门月度考勤统计
     *
     * @param deptId 部门ID
     * @param year   年份
     * @param month  月份
     * @return 部门统计数据
     */
    public DeptStats deptMonthlyStats(Long deptId, int year, int month) {
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.plusMonths(1).minusDays(1);

        List<AttendanceRecord> records = attendanceRecordMapper.selectByDeptAndDateRange(
                deptId, startDate, endDate);

        // 查询部门在职员工
        List<Employee> deptEmployees = employeeMapper.selectByDeptId(deptId);

        DeptStats stats = new DeptStats();
        stats.deptId = deptId;
        stats.year = year;
        stats.month = month;
        stats.totalEmployees = deptEmployees != null ? deptEmployees.size() : 0;

        if (records == null || records.isEmpty()) {
            stats.normalDays = 0;
            stats.lateDays = 0;
            stats.earlyDays = 0;
            stats.absentHalfDays = BigDecimal.ZERO;
            stats.missingPunchDays = 0;
            stats.deptAttendanceRate = BigDecimal.ZERO;
            return stats;
        }

        // 按员工分组统计
        Map<Long, List<AttendanceRecord>> byEmployee = new HashMap<>();
        for (AttendanceRecord r : records) {
            byEmployee.computeIfAbsent(r.getEmployeeId(), k -> new ArrayList<>()).add(r);
        }

        int totalNormal = 0;
        int totalLate = 0;
        int totalEarly = 0;
        BigDecimal totalAbsent = BigDecimal.ZERO;
        int totalMissing = 0;
        BigDecimal totalDays = BigDecimal.ZERO;

        for (List<AttendanceRecord> empRecords : byEmployee.values()) {
            for (AttendanceRecord r : empRecords) {
                totalDays = totalDays.add(BigDecimal.ONE);
                String in = r.getPunchInStatus();
                String out = r.getPunchOutStatus();

                boolean isLate = "LATE".equals(in);
                boolean isEarly = "EARLY".equals(out);
                boolean isAbsent = "ABSENT_HALF_DAY".equals(in) || "ABSENT_HALF_DAY".equals(out);
                boolean isMissing = "MISSING_PUNCH".equals(in) || "MISSING_PUNCH".equals(out);

                if (!isLate && !isEarly && !isAbsent && !isMissing) {
                    totalNormal++;
                }
                if (isLate) totalLate++;
                if (isEarly) totalEarly++;
                if (isAbsent) totalAbsent = totalAbsent.add(new BigDecimal("0.5"));
                if (isMissing) totalMissing++;
            }
        }

        stats.normalDays = totalNormal;
        stats.lateDays = totalLate;
        stats.earlyDays = totalEarly;
        stats.absentHalfDays = totalAbsent;
        stats.missingPunchDays = totalMissing;

        // 部门出勤率
        if (totalDays.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal normalAndLate = BigDecimal.valueOf(totalNormal + totalLate);
            stats.deptAttendanceRate = normalAndLate
                    .divide(totalDays, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);
        } else {
            stats.deptAttendanceRate = BigDecimal.ZERO;
        }

        stats.recordedEmployeeCount = byEmployee.size();

        return stats;
    }

    /** 个人月考勤统计 */
    public static class PersonalStats {
        public Long employeeId;
        public int year;
        public int month;
        public BigDecimal totalWorkDays = BigDecimal.ZERO;    // 应出勤天数
        public int normalDays = 0;                             // 正常出勤天数
        public int lateDays = 0;                               // 迟到天数
        public int earlyDays = 0;                              // 早退天数
        public BigDecimal absentHalfDays = BigDecimal.ZERO;    // 旷工半天次数
        public int missingPunchDays = 0;                       // 缺卡天数
        public BigDecimal attendanceRate = BigDecimal.ZERO;    // 出勤率(%)
    }

    /** 部门月考勤统计 */
    public static class DeptStats {
        public Long deptId;
        public int year;
        public int month;
        public int totalEmployees = 0;          // 部门总人数
        public int recordedEmployeeCount = 0;   // 有打卡记录人数
        public int normalDays = 0;
        public int lateDays = 0;
        public int earlyDays = 0;
        public BigDecimal absentHalfDays = BigDecimal.ZERO;
        public int missingPunchDays = 0;
        public BigDecimal deptAttendanceRate = BigDecimal.ZERO; // 部门出勤率(%)
    }
}
