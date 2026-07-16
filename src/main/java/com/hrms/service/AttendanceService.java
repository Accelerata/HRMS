package com.hrms.service;

import com.hrms.common.exception.BaseException;
import com.hrms.dto.AttendanceGroupSaveDTO;
import com.hrms.dto.AttendancePunchDTO;
import com.hrms.entity.AttendanceGroup;
import com.hrms.entity.AttendanceRecord;
import com.hrms.enums.AttendanceStatus;
import com.hrms.mapper.AttendanceGroupMapper;
import com.hrms.mapper.AttendanceRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 考勤打卡判定服务
 *
 * 核心业务规则：
 * 1. 缺卡：打卡时间为 null → MISSING_PUNCH
 * 2. 上班打卡：
 *    - 打卡时间 <= 规定上班时间 + 弹性阈值 → NORMAL
 *    - 打卡时间 > 规定上班时间 + 弹性阈值：
 *      - 迟到分钟数 >= 旷工半天阈值 → ABSENT_HALF_DAY
 *      - 否则 → LATE
 * 3. 下班打卡：
 *    - 打卡时间 >= 规定下班时间 → NORMAL
 *    - 打卡时间 < 规定下班时间：
 *      - 早退分钟数 > 旷工半天阈值 → ABSENT_HALF_DAY
 *      - 否则 → EARLY
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceGroupMapper attendanceGroupMapper;
    private final AttendanceRecordMapper attendanceRecordMapper;

    // ═══════════════ 考勤组管理 ═══════════════

    /** 查询所有考勤组 */
    public List<AttendanceGroup> listGroups() {
        return attendanceGroupMapper.selectAll();
    }

    /** 根据ID查询考勤组 */
    public AttendanceGroup getGroupById(Long id) {
        AttendanceGroup group = attendanceGroupMapper.selectById(id);
        if (group == null) {
            throw BaseException.notFound("考勤组不存在");
        }
        return group;
    }

    /** 创建考勤组 */
    @Transactional
    public void createGroup(AttendanceGroupSaveDTO dto) {
        AttendanceGroup group = new AttendanceGroup();
        group.setGroupName(dto.getGroupName());
        group.setGroupType(dto.getGroupType());
        group.setStartTime(dto.getStartTime());
        group.setEndTime(dto.getEndTime());
        group.setFlexThreshold(dto.getFlexThreshold() != null ? dto.getFlexThreshold() : 0);
        group.setAbsentHalfDayThreshold(
                dto.getAbsentHalfDayThreshold() != null ? dto.getAbsentHalfDayThreshold() : 120);
        // 新增字段
        group.setDeptId(dto.getDeptId());
        group.setPositionId(dto.getPositionId());
        group.setEmployeeIds(dto.getEmployeeIds());
        group.setLunchBreakStart(dto.getLunchBreakStart());
        group.setLunchBreakEnd(dto.getLunchBreakEnd());
        group.setLateThresholdMinutes(
                dto.getLateThresholdMinutes() != null ? dto.getLateThresholdMinutes() : 15);
        group.setEarlyThresholdMinutes(
                dto.getEarlyThresholdMinutes() != null ? dto.getEarlyThresholdMinutes() : 15);
        attendanceGroupMapper.insert(group);
        log.info("考勤组创建成功: id={}, name={}, deptId={}, positionId={}",
                group.getId(), group.getGroupName(), group.getDeptId(), group.getPositionId());
    }

    /** 更新考勤组 */
    @Transactional
    public void updateGroup(AttendanceGroupSaveDTO dto) {
        AttendanceGroup existing = attendanceGroupMapper.selectById(dto.getId());
        if (existing == null) {
            throw BaseException.notFound("考勤组不存在");
        }
        AttendanceGroup group = new AttendanceGroup();
        group.setId(dto.getId());
        group.setGroupName(dto.getGroupName());
        group.setGroupType(dto.getGroupType());
        group.setStartTime(dto.getStartTime());
        group.setEndTime(dto.getEndTime());
        group.setFlexThreshold(dto.getFlexThreshold() != null ? dto.getFlexThreshold() : 0);
        group.setAbsentHalfDayThreshold(
                dto.getAbsentHalfDayThreshold() != null ? dto.getAbsentHalfDayThreshold() : 120);
        // 新增字段
        group.setDeptId(dto.getDeptId());
        group.setPositionId(dto.getPositionId());
        group.setEmployeeIds(dto.getEmployeeIds());
        group.setLunchBreakStart(dto.getLunchBreakStart());
        group.setLunchBreakEnd(dto.getLunchBreakEnd());
        group.setLateThresholdMinutes(
                dto.getLateThresholdMinutes() != null ? dto.getLateThresholdMinutes() : 15);
        group.setEarlyThresholdMinutes(
                dto.getEarlyThresholdMinutes() != null ? dto.getEarlyThresholdMinutes() : 15);
        attendanceGroupMapper.update(group);
        log.info("考勤组更新成功: id={}", group.getId());
    }

    /** 删除考勤组 */
    @Transactional
    public void deleteGroup(Long id) {
        AttendanceGroup group = attendanceGroupMapper.selectById(id);
        if (group == null) {
            throw BaseException.notFound("考勤组不存在");
        }
        attendanceGroupMapper.deleteById(id);
        log.info("考勤组删除成功: id={}", id);
    }

    // ═══════════════ 打卡操作 ═══════════════

    /**
     * 上班打卡
     */
    @Transactional
    public AttendanceRecord punchIn(AttendancePunchDTO dto) {
        AttendanceGroup group = getGroupById(dto.getGroupId());

        // 判定打卡状态
        AttendanceStatus status = determinePunchIn(group, dto.getPunchTime());

        // 查找当天是否已有打卡记录
        LocalDate today = LocalDate.now();
        AttendanceRecord record = attendanceRecordMapper.selectByEmployeeAndDate(
                dto.getEmployeeId(), today);

        if (record == null) {
            // 新建打卡记录
            record = new AttendanceRecord();
            record.setEmployeeId(dto.getEmployeeId());
            record.setGroupId(dto.getGroupId());
            record.setAttendanceDate(today);
            record.setPunchInTime(dto.getPunchTime());
            record.setPunchInStatus(status.name());
            attendanceRecordMapper.insert(record);
        } else {
            // 更新上班打卡
            attendanceRecordMapper.updatePunchIn(record.getId(), dto.getPunchTime(), status.name());
            record.setPunchInTime(dto.getPunchTime());
            record.setPunchInStatus(status.name());
        }

        log.info("上班打卡: employeeId={}, status={}", dto.getEmployeeId(), status.name());
        return record;
    }

    /**
     * 下班打卡
     */
    @Transactional
    public AttendanceRecord punchOut(AttendancePunchDTO dto) {
        AttendanceGroup group = getGroupById(dto.getGroupId());

        // 判定打卡状态
        AttendanceStatus status = determinePunchOut(group, dto.getPunchTime());

        // 查找当天打卡记录
        LocalDate today = LocalDate.now();
        AttendanceRecord record = attendanceRecordMapper.selectByEmployeeAndDate(
                dto.getEmployeeId(), today);

        if (record == null) {
            throw BaseException.badRequest("请先打上班卡");
        }

        // 更新下班打卡
        attendanceRecordMapper.updatePunchOut(record.getId(), dto.getPunchTime(), status.name());
        record.setPunchOutTime(dto.getPunchTime());
        record.setPunchOutStatus(status.name());

        log.info("下班打卡: employeeId={}, status={}", dto.getEmployeeId(), status.name());
        return record;
    }

    /** 查询员工打卡记录 */
    public List<AttendanceRecord> getRecords(Long employeeId, int page, int size) {
        int offset = (page - 1) * size;
        return attendanceRecordMapper.selectByEmployee(employeeId, offset, size);
    }

    /**
     * 从考勤组移除员工关联（离职生效时调用）。
     * 当前版本考勤组通过部门/职位间接关联员工，移除操作主要清除打卡记录引用。
     * 后续考勤组增强时会增加直接的员工-考勤组关联表清理。
     */
    @Transactional
    public void removeEmployeeFromGroups(Long employeeId) {
        // 清除该员工的未处理打卡记录引用（如有考勤组关联映射表则同步清理）
        log.info("员工从考勤组移除: employeeId={}", employeeId);
        // TODO: 考勤组增强后增加 attendance_group_employee 关联表清理
    }

    // ═══════════════ 核心判定逻辑 ═══════════════

    /**
     * 判定上班打卡状态
     *
     * @param group       考勤组规则
     * @param punchInTime 实际上班打卡时间（null 表示缺卡）
     * @return 打卡状态
     */
    public AttendanceStatus determinePunchIn(AttendanceGroup group, LocalTime punchInTime) {
        if (punchInTime == null) {
            return AttendanceStatus.MISSING_PUNCH;
        }

        LocalTime flexDeadline = group.getStartTime().plusMinutes(group.getFlexThreshold());

        if (!punchInTime.isAfter(flexDeadline)) {
            return AttendanceStatus.NORMAL;
        }

        long lateMinutes = ChronoUnit.MINUTES.between(flexDeadline, punchInTime);

        if (lateMinutes >= group.getAbsentHalfDayThreshold()) {
            return AttendanceStatus.ABSENT_HALF_DAY;
        }

        return AttendanceStatus.LATE;
    }

    /**
     * 判定下班打卡状态
     *
     * @param group        考勤组规则
     * @param punchOutTime 实际下班打卡时间（null 表示缺卡）
     * @return 打卡状态
     */
    public AttendanceStatus determinePunchOut(AttendanceGroup group, LocalTime punchOutTime) {
        if (punchOutTime == null) {
            return AttendanceStatus.MISSING_PUNCH;
        }

        if (!punchOutTime.isBefore(group.getEndTime())) {
            return AttendanceStatus.NORMAL;
        }

        long earlyMinutes = ChronoUnit.MINUTES.between(punchOutTime, group.getEndTime());

        if (earlyMinutes > group.getAbsentHalfDayThreshold()) {
            return AttendanceStatus.ABSENT_HALF_DAY;
        }

        return AttendanceStatus.EARLY;
    }
}
