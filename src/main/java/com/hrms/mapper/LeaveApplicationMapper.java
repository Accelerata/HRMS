package com.hrms.mapper;

import com.hrms.entity.LeaveApplication;
import com.hrms.vo.LeaveStatsByTypeVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
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

    /** 根据员工ID和时间范围查询请假记录（日历视图用） */
    List<LeaveApplication> selectByEmployeeAndMonth(@Param("employeeId") Long employeeId,
                                                     @Param("startDate") LocalDate startDate,
                                                     @Param("endDate") LocalDate endDate);

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
     */
    java.math.BigDecimal sumApprovedLeaveDays(@Param("employeeId") Long employeeId,
                                               @Param("leaveType") Integer leaveType,
                                               @Param("startDate") java.time.LocalDate startDate,
                                               @Param("endDate") java.time.LocalDate endDate);

    /**
     * 统计指定员工某月分类型已通过请假天数（个人统计）
     */
    List<LeaveStatsByTypeVO> countApprovedByTypeAndEmployee(
            @Param("employeeId") Long employeeId,
            @Param("monthStart") LocalDate monthStart,
            @Param("monthEnd") LocalDate monthEnd);

    /**
     * 统计指定部门某月分类型已通过请假天数（部门统计）
     */
    List<LeaveStatsByTypeVO> countApprovedByTypeAndDept(
            @Param("deptId") Long deptId,
            @Param("monthStart") LocalDate monthStart,
            @Param("monthEnd") LocalDate monthEnd);

    /**
     * 统计全公司或指定部门某月请假类型分布
     */
    List<LeaveStatsByTypeVO> countApprovedTypeDistribution(
            @Param("deptId") Long deptId,
            @Param("monthStart") LocalDate monthStart,
            @Param("monthEnd") LocalDate monthEnd);

    /**
     * 部门某月已通过请假天数合计
     */
    java.math.BigDecimal sumApprovedByDept(
            @Param("deptId") Long deptId,
            @Param("monthStart") LocalDate monthStart,
            @Param("monthEnd") LocalDate monthEnd);
}
