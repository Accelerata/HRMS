package com.hrms.service;

import com.hrms.common.context.BaseContext;
import com.hrms.common.exception.BaseException;
import com.hrms.entity.*;
import com.hrms.enums.LeaveType;
import com.hrms.mapper.*;
import com.hrms.vo.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * 请假统计服务
 *
 * 数据权限按 RBAC 控制：
 * - 系统管理员/HR：全量
 * - 部门主管：仅本部门及下属
 * - 普通员工：仅本人
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LeaveStatisticsService {

    private final LeaveApplicationMapper leaveApplicationMapper;
    private final LeaveBalanceMapper leaveBalanceMapper;
    private final EmployeeMapper employeeMapper;
    private final DepartmentMapper departmentMapper;
    private final WorkCalendarService workCalendarService;

    /**
     * 个人请假统计：当月分类型已通过请假天数 + 当年年假余额
     */
    public PersonalLeaveStatsVO personalStats(Long employeeId, int year, int month) {
        // 数据范围：员工仅本人
        Long currentEmployeeId = BaseContext.getCurrentEmployeeId();
        Employee currentEmp = employeeMapper.selectById(currentEmployeeId);
        if (currentEmp == null) {
            throw BaseException.forbidden("无权限");
        }
        // 普通员工只能查本人
        if (!isHrOrAdmin() && !isDeptManager()) {
            if (!currentEmployeeId.equals(employeeId)) {
                throw BaseException.forbidden("仅可查看本人请假统计");
            }
        }

        Employee target = employeeMapper.selectById(employeeId);
        if (target == null) {
            throw BaseException.notFound("员工不存在");
        }

        YearMonth ym = YearMonth.of(year, month);
        LocalDate monthStart = ym.atDay(1);
        LocalDate monthEnd = ym.atEndOfMonth();

        List<LeaveStatsByTypeVO> byType = leaveApplicationMapper.countApprovedByTypeAndEmployee(
                employeeId, monthStart, monthEnd);
        // 填充类型名称
        if (byType != null) {
            for (LeaveStatsByTypeVO vo : byType) {
                LeaveType lt = LeaveType.fromCode(vo.getLeaveType());
                vo.setLeaveTypeName(lt != null ? lt.getLabel() : "未知");
            }
        }

        // 当年年假余额
        LeaveBalance annualBalance = leaveBalanceMapper.selectByEmployeeTypeAndYear(
                employeeId, LeaveType.ANNUAL.getCode(), year);

        PersonalLeaveStatsVO vo = new PersonalLeaveStatsVO();
        vo.setEmployeeId(employeeId);
        vo.setEmployeeName(target.getName());
        vo.setMonth(String.format("%d-%02d", year, month));
        vo.setByType(byType);
        vo.setAnnualRemainingDays(annualBalance != null ? annualBalance.getRemainingDays() : BigDecimal.ZERO);
        return vo;
    }

    /**
     * 部门请假率统计
     */
    public DeptLeaveRateVO deptStats(Long deptId, int year, int month) {
        // 数据范围校验
        validateDeptAccess(deptId);

        Department dept = departmentMapper.selectById(deptId);
        if (dept == null) {
            throw BaseException.notFound("部门不存在");
        }

        YearMonth ym = YearMonth.of(year, month);
        LocalDate monthStart = ym.atDay(1);
        LocalDate monthEnd = ym.atEndOfMonth();

        // 部门在职人数（试用期+正式）
        List<Employee> deptEmps = employeeMapper.selectByDeptId(deptId);
        long activeCount = deptEmps.stream()
                .filter(e -> e.getStatus() != null && (e.getStatus() == 1 || e.getStatus() == 2))
                .count();

        // 当月工作日数
        List<LocalDate> workdays = workCalendarService.workdaysBetween(monthStart, monthEnd);
        int workdayCount = workdays.size();

        // 部门已通过请假天数合计
        BigDecimal totalLeaveDays = leaveApplicationMapper.sumApprovedByDept(deptId, monthStart, monthEnd);
        if (totalLeaveDays == null) totalLeaveDays = BigDecimal.ZERO;

        // 请假率
        BigDecimal leaveRate = BigDecimal.ZERO;
        if (activeCount > 0 && workdayCount > 0) {
            BigDecimal denominator = BigDecimal.valueOf(activeCount).multiply(BigDecimal.valueOf(workdayCount));
            leaveRate = totalLeaveDays.divide(denominator, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }

        // 分类型
        List<LeaveStatsByTypeVO> byType = leaveApplicationMapper.countApprovedByTypeAndDept(
                deptId, monthStart, monthEnd);
        if (byType != null) {
            for (LeaveStatsByTypeVO vo : byType) {
                LeaveType lt = LeaveType.fromCode(vo.getLeaveType());
                vo.setLeaveTypeName(lt != null ? lt.getLabel() : "未知");
            }
        }

        DeptLeaveRateVO vo = new DeptLeaveRateVO();
        vo.setDeptId(deptId);
        vo.setDeptName(dept.getDeptName());
        vo.setMonth(String.format("%d-%02d", year, month));
        vo.setActiveEmployeeCount((int) activeCount);
        vo.setWorkdayCount(workdayCount);
        vo.setTotalLeaveDays(totalLeaveDays);
        vo.setLeaveRate(leaveRate);
        vo.setByType(byType);
        return vo;
    }

    /**
     * 请假类型分布
     */
    public List<LeaveTypeDistributionVO> typeDistribution(Long deptId, int year, int month) {
        // 数据范围校验
        if (deptId != null) {
            validateDeptAccess(deptId);
        } else if (!isHrOrAdmin()) {
            // 非HR/管理员不能查全公司
            throw BaseException.forbidden("仅HR/管理员可查看全公司统计");
        }

        YearMonth ym = YearMonth.of(year, month);
        LocalDate monthStart = ym.atDay(1);
        LocalDate monthEnd = ym.atEndOfMonth();

        List<LeaveStatsByTypeVO> raw = leaveApplicationMapper.countApprovedTypeDistribution(
                deptId, monthStart, monthEnd);

        if (raw == null) return List.of();

        return raw.stream().map(r -> {
            LeaveTypeDistributionVO d = new LeaveTypeDistributionVO();
            d.setLeaveType(r.getLeaveType());
            LeaveType lt = LeaveType.fromCode(r.getLeaveType());
            d.setLeaveTypeName(lt != null ? lt.getLabel() : "未知");
            d.setTotalDays(r.getTotalDays());
            d.setCount(r.getCount());
            return d;
        }).toList();
    }

    // ═══════════════ 数据范围校验 ═══════════════

    private void validateDeptAccess(Long deptId) {
        if (isHrOrAdmin()) return;

        Long currentEmployeeId = BaseContext.getCurrentEmployeeId();
        Employee currentEmp = employeeMapper.selectById(currentEmployeeId);
        if (currentEmp == null) {
            throw BaseException.forbidden("无权限");
        }

        // 部门主管仅本部门
        if (currentEmp.getDeptId() != null && currentEmp.getDeptId().equals(deptId)) return;

        throw BaseException.forbidden("无权限查看该部门统计");
    }

    private boolean isHrOrAdmin() {
        Integer dataScope = BaseContext.getDataScope();
        return dataScope != null && (dataScope == 1 || dataScope == 2);
    }

    private boolean isDeptManager() {
        Integer dataScope = BaseContext.getDataScope();
        return dataScope != null && dataScope == 3;
    }
}
