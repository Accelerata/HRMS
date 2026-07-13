package com.hrms.mapper;

import com.hrms.entity.LeaveBalance;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 假期余额 Mapper
 */
@Mapper
public interface LeaveBalanceMapper {

    /** 根据员工ID和年份查询所有假期余额 */
    List<LeaveBalance> selectByEmployeeAndYear(@Param("employeeId") Long employeeId,
                                                @Param("year") Integer year);

    /** 根据员工ID、假期类型和年份查询 */
    LeaveBalance selectByEmployeeTypeAndYear(@Param("employeeId") Long employeeId,
                                              @Param("leaveType") Integer leaveType,
                                              @Param("year") Integer year);

    /** 插入 */
    int insert(LeaveBalance balance);

    /** 更新余额 */
    int update(LeaveBalance balance);

    /** 根据员工ID初始化年假余额 */
    int initAnnualLeave(@Param("employeeId") Long employeeId,
                        @Param("totalDays") java.math.BigDecimal totalDays,
                        @Param("year") Integer year);
}
