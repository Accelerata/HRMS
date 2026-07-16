package com.hrms.controller;

import com.hrms.annotation.RequirePermission;
import com.hrms.common.context.BaseContext;
import com.hrms.common.exception.BaseException;
import com.hrms.entity.Employee;
import com.hrms.entity.LeaveApplication;
import com.hrms.enums.LeaveType;
import com.hrms.mapper.EmployeeMapper;
import com.hrms.mapper.LeaveApplicationMapper;
import com.hrms.result.Result;
import com.hrms.service.AttendanceService;
import com.hrms.service.EmployeeService;
import com.hrms.service.PayslipService;
import com.hrms.vo.EmployeeVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 个人中心控制器
 * 当前登录员工查看和编辑个人信息、考勤日历、薪资趋势
 */
@RestController
@RequestMapping("/api/v1/personal")
@RequiredArgsConstructor
public class PersonalCenterController {

    private final EmployeeService employeeService;
    private final EmployeeMapper employeeMapper;
    private final AttendanceService attendanceService;
    private final PayslipService payslipService;
    private final LeaveApplicationMapper leaveApplicationMapper;

    /** 我的档案 */
    @GetMapping("/profile")
    public Result<Map<String, Object>> getProfile() {
        Long employeeId = BaseContext.getCurrentEmployeeId();
        if (employeeId == null) {
            throw new BaseException(401, "未关联员工身份");
        }

        Employee emp = employeeMapper.selectById(employeeId);
        if (emp == null) {
            throw BaseException.notFound("员工档案不存在");
        }

        EmployeeVO vo = employeeService.toEmployeeVO(emp);

        Map<String, Object> result = new HashMap<>();
        result.put("profile", vo);
        result.put("editability", buildFieldEditability());
        return Result.success(result);
    }

    /** 更新可编辑字段 */
    @PutMapping("/profile")
    public Result<EmployeeVO> updateProfile(@RequestBody Map<String, Object> updates) {
        Long employeeId = BaseContext.getCurrentEmployeeId();
        if (employeeId == null) {
            throw new BaseException(401, "未关联员工身份");
        }

        // 仅允许更新可编辑字段
        Employee emp = employeeMapper.selectById(employeeId);
        if (emp == null) throw BaseException.notFound("员工档案不存在");

        // 禁止修改锁定字段
        String[] lockedFields = {"deptId", "positionId", "grade", "reportTo",
                "baseSalary", "salaryAccountId", "bankAccount", "employeeNo", "status"};
        for (String field : lockedFields) {
            if (updates.containsKey(field)) {
                throw BaseException.badRequest("字段 " + field + " 不可自行修改，如需修改请联系 HR");
            }
        }

        // 允许更新的字段
        if (updates.containsKey("email")) emp.setEmail((String) updates.get("email"));
        if (updates.containsKey("currentAddress")) emp.setCurrentAddress((String) updates.get("currentAddress"));
        if (updates.containsKey("registeredAddress")) emp.setRegisteredAddress((String) updates.get("registeredAddress"));
        if (updates.containsKey("birthday") && updates.get("birthday") != null)
            emp.setBirthday(LocalDate.parse(updates.get("birthday").toString()));

        employeeMapper.update(emp);
        return Result.success(employeeService.toEmployeeVO(emp));
    }

    /** 考勤日历视图（按日汇总考勤状态 + 请假详情） */
    @GetMapping("/attendance-calendar")
    public Result<List<Map<String, Object>>> getAttendanceCalendar(
            @RequestParam String yearMonth) {
        Long employeeId = BaseContext.getCurrentEmployeeId();
        if (employeeId == null) {
            throw new BaseException(401, "未关联员工身份");
        }

        YearMonth ym = YearMonth.parse(yearMonth);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();

        // 查询当月已批准的请假记录
        List<LeaveApplication> approvedLeaves = leaveApplicationMapper.selectByEmployeeAndMonth(
                employeeId, start, end);

        // 构建日期→请假详情映射
        Map<LocalDate, Map<String, Object>> leaveMap = new HashMap<>();
        if (approvedLeaves != null) {
            for (LeaveApplication leave : approvedLeaves) {
                // 仅已通过的请假
                if (leave.getStatus() == null || leave.getStatus() != 2) continue;
                LeaveType type = LeaveType.fromCode(leave.getLeaveType());
                for (LocalDate d = leave.getStartDate();
                     !d.isAfter(leave.getEndDate());
                     d = d.plusDays(1)) {
                    Map<String, Object> leaveInfo = new HashMap<>();
                    leaveInfo.put("leaveType", leave.getLeaveType());
                    leaveInfo.put("leaveTypeName", type != null ? type.getLabel() : "未知");
                    leaveInfo.put("reason", leave.getReason());
                    leaveInfo.put("applicationId", leave.getId());
                    leaveMap.put(d, leaveInfo);
                }
            }
        }

        // 查询当月打卡记录
        List<?> records = attendanceService.getRecords(employeeId, 1, 31);

        List<Map<String, Object>> calendar = new ArrayList<>();
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            Map<String, Object> day = new HashMap<>();
            day.put("date", date.toString());
            day.put("isWorkday", date.getDayOfWeek().getValue() <= 5);

            String status = determineDayStatus(date, records);

            // 如果当天有请假，覆盖状态并附上请假详情
            if (leaveMap.containsKey(date)) {
                status = "LEAVE";
                day.put("leaveDetail", leaveMap.get(date));
            }

            day.put("status", status);
            calendar.add(day);
        }
        return Result.success(calendar);
    }

    /** 个人薪资趋势（近6个月实发工资） */
    @GetMapping("/salary-trend")
    @RequirePermission("salary:payslip:self")
    public Result<List<Map<String, Object>>> getSalaryTrend() {
        Long employeeId = BaseContext.getCurrentEmployeeId();
        if (employeeId == null) {
            throw new BaseException(401, "未关联员工身份");
        }

        List<Map<String, Object>> trend = new ArrayList<>();
        YearMonth current = YearMonth.now();
        for (int i = 5; i >= 0; i--) {
            YearMonth ym = current.minusMonths(i);
            Map<String, Object> point = new HashMap<>();
            point.put("yearMonth", ym.toString());
            try {
                var record = payslipService.getPayslipForMonth(employeeId, ym);
                point.put("netPay", record != null ? record.get("netPay") : null);
            } catch (Exception e) {
                point.put("netPay", null);
            }
            trend.add(point);
        }
        return Result.success(trend);
    }

    private Map<String, Object> buildFieldEditability() {
        Map<String, Object> map = new HashMap<>();
        // 可编辑
        map.put("email", fieldMeta(true, null));
        map.put("currentAddress", fieldMeta(true, null));
        map.put("registeredAddress", fieldMeta(true, null));
        map.put("birthday", fieldMeta(true, null));
        // 锁定
        map.put("deptId", fieldMeta(false, "需调岗流程"));
        map.put("positionId", fieldMeta(false, "需调岗流程"));
        map.put("grade", fieldMeta(false, "需调岗流程"));
        map.put("baseSalary", fieldMeta(false, "如需修改请联系 HR"));
        map.put("bankAccount", fieldMeta(false, "如需修改请联系 HR"));
        return map;
    }

    private Map<String, Object> fieldMeta(boolean editable, String lockReason) {
        Map<String, Object> meta = new HashMap<>();
        meta.put("editable", editable);
        meta.put("lockReason", lockReason);
        return meta;
    }

    private String determineDayStatus(LocalDate date, List<?> records) {
        if (date.isAfter(LocalDate.now())) return "FUTURE";
        if (date.getDayOfWeek().getValue() >= 6) return "WEEKEND";
        // Simplified: default to NORMAL for past weekdays
        return "NORMAL";
    }
}
