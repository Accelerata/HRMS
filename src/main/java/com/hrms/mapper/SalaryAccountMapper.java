package com.hrms.mapper;

import com.hrms.entity.SalaryAccount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 薪资账套 Mapper
 */
@Mapper
public interface SalaryAccountMapper {

    /** 根据ID查询 */
    SalaryAccount selectById(@Param("id") Long id);

    /** 根据员工ID查询生效中的薪资账套 */
    SalaryAccount selectByEmployeeId(@Param("employeeId") Long employeeId);

    /** 批量查询所有在职员工的薪资账套 */
    List<SalaryAccount> selectAllActive();

    /** 插入薪资账套 */
    int insert(SalaryAccount account);

    /** 更新薪资账套 */
    int update(SalaryAccount account);

    /** 失效某个账套（员工调岗/离职时） */
    int deactivate(@Param("id") Long id);
}
