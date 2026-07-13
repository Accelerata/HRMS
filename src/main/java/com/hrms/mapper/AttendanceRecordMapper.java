package com.hrms.mapper;

import com.hrms.entity.AttendanceRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * 打卡记录 Mapper
 */
@Mapper
public interface AttendanceRecordMapper {

    /** 根据员工ID和日期查询打卡记录 */
    AttendanceRecord selectByEmployeeAndDate(@Param("employeeId") Long employeeId,
                                              @Param("attendanceDate") LocalDate attendanceDate);

    /** 根据员工ID查询打卡记录（分页） */
    List<AttendanceRecord> selectByEmployee(@Param("employeeId") Long employeeId,
                                            @Param("offset") int offset,
                                            @Param("limit") int limit);

    /** 根据员工ID统计打卡记录数 */
    int countByEmployee(@Param("employeeId") Long employeeId);

    /** 根据部门ID和日期范围查询 */
    List<AttendanceRecord> selectByDeptAndDateRange(@Param("deptId") Long deptId,
                                                     @Param("startDate") LocalDate startDate,
                                                     @Param("endDate") LocalDate endDate);

    /** 插入打卡记录 */
    int insert(AttendanceRecord record);

    /** 更新上班打卡 */
    int updatePunchIn(@Param("id") Long id,
                      @Param("punchInTime") java.time.LocalTime punchInTime,
                      @Param("punchInStatus") String punchInStatus);

    /** 更新下班打卡 */
    int updatePunchOut(@Param("id") Long id,
                       @Param("punchOutTime") java.time.LocalTime punchOutTime,
                       @Param("punchOutStatus") String punchOutStatus);

    /** 查询员工某月所有打卡记录（用于薪资核算汇总） */
    List<AttendanceRecord> selectByEmployeeAndMonth(@Param("employeeId") Long employeeId,
                                                     @Param("year") int year,
                                                     @Param("month") int month);
}
