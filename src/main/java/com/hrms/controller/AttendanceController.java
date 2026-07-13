package com.hrms.controller;

import com.hrms.annotation.RequirePermission;
import com.hrms.dto.AttendanceGroupSaveDTO;
import com.hrms.dto.AttendancePunchDTO;
import com.hrms.entity.AttendanceGroup;
import com.hrms.entity.AttendanceRecord;
import com.hrms.result.Result;
import com.hrms.service.AttendanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 考勤管理控制器
 *
 * 权限说明：
 * - attendance:punch - 员工打卡（普通员工及以上）
 * - attendance:record:view - 查看打卡记录（员工本人及管理者）
 * - attendance:group:manage - 考勤组管理（HR专员及以上）
 */
@RestController
@RequestMapping("/api/v1/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    // ═══════════════ 打卡操作 ═══════════════

    /** 上班打卡 */
    @PostMapping("/punch-in")
    @RequirePermission("attendance:punch")
    public Result<AttendanceRecord> punchIn(@Valid @RequestBody AttendancePunchDTO dto) {
        return Result.success(attendanceService.punchIn(dto));
    }

    /** 下班打卡 */
    @PostMapping("/punch-out")
    @RequirePermission("attendance:punch")
    public Result<AttendanceRecord> punchOut(@Valid @RequestBody AttendancePunchDTO dto) {
        return Result.success(attendanceService.punchOut(dto));
    }

    /** 查询员工打卡记录 */
    @GetMapping("/records/{employeeId}")
    @RequirePermission("attendance:record:view")
    public Result<List<AttendanceRecord>> records(
            @PathVariable Long employeeId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "31") int size) {
        return Result.success(attendanceService.getRecords(employeeId, page, size));
    }

    // ═══════════════ 考勤组管理 ═══════════════

    /** 考勤组列表 */
    @GetMapping("/groups")
    @RequirePermission("attendance:group:view")
    public Result<List<AttendanceGroup>> groupList() {
        return Result.success(attendanceService.listGroups());
    }

    /** 创建考勤组 */
    @PostMapping("/groups")
    @RequirePermission("attendance:group:manage")
    public Result<Void> createGroup(@Valid @RequestBody AttendanceGroupSaveDTO dto) {
        attendanceService.createGroup(dto);
        return Result.success();
    }

    /** 更新考勤组 */
    @PutMapping("/groups/{id}")
    @RequirePermission("attendance:group:manage")
    public Result<Void> updateGroup(@PathVariable Long id,
                                     @Valid @RequestBody AttendanceGroupSaveDTO dto) {
        dto.setId(id);
        attendanceService.updateGroup(dto);
        return Result.success();
    }

    /** 删除考勤组 */
    @DeleteMapping("/groups/{id}")
    @RequirePermission("attendance:group:manage")
    public Result<Void> deleteGroup(@PathVariable Long id) {
        attendanceService.deleteGroup(id);
        return Result.success();
    }
}
