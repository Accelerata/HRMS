package com.hrms.mapper;

import com.hrms.entity.LeaveApplication;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 请假申请 Mapper
 */
@Mapper
public interface LeaveApplicationMapper {

    /** 根据ID查询 */
    LeaveApplication selectById(@Param("id") Long id);

    /** 根据员工ID查询请假记录（分页） */
    List<LeaveApplication> selectByEmployee(@Param("employeeId") Long employeeId,
                                             @Param("offset") int offset,
                                             @Param("limit") int limit);

    /** 根据员工ID统计请假记录数 */
    int countByEmployee(@Param("employeeId") Long employeeId);

    /** 根据状态查询 */
    List<LeaveApplication> selectByStatus(@Param("status") Integer status,
                                           @Param("offset") int offset,
                                           @Param("limit") int limit);

    /** 插入 */
    int insert(LeaveApplication application);

    /** 更新状态 */
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    /**
     * 汇总某类型已通过请假的天数（用于薪资核算事假扣款）
     * @param employeeId 员工ID
     * @param leaveType  假期类型（3=事假）
     * @param startDate  日期范围起始
     * @param endDate    日期范围结束
     * @return 请假天数合计
     */
    java.math.BigDecimal sumApprovedLeaveDays(@Param("employeeId") Long employeeId,
                                               @Param("leaveType") Integer leaveType,
                                               @Param("startDate") java.time.LocalDate startDate,
                                               @Param("endDate") java.time.LocalDate endDate);
}
