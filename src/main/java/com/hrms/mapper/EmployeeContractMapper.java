package com.hrms.mapper;

import com.hrms.entity.EmployeeContract;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 员工合同 Mapper
 */
@Mapper
public interface EmployeeContractMapper {

    /** 根据ID查询 */
    EmployeeContract selectById(@Param("id") Long id);

    /** 根据员工ID查询所有合同（按日期倒序） */
    List<EmployeeContract> selectByEmployeeId(@Param("employeeId") Long employeeId);

    /** 查询员工当前生效的合同 */
    EmployeeContract selectActiveByEmployeeId(@Param("employeeId") Long employeeId);

    /** 新增合同 */
    int insert(EmployeeContract contract);

    /** 更新合同 */
    int update(EmployeeContract contract);

    /** 终止合同（设置 status=0） */
    int terminateById(@Param("id") Long id);
}
