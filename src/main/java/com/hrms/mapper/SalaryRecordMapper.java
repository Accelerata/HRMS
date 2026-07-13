package com.hrms.mapper;

import com.hrms.entity.SalaryRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 薪资记录 Mapper
 */
@Mapper
public interface SalaryRecordMapper {

    /** 插入薪资记录 */
    int insert(SalaryRecord record);

    /** 批量插入 */
    int batchInsert(@Param("list") List<SalaryRecord> records);

    /** 根据员工ID和年月查询 */
    SalaryRecord selectByEmployeeAndMonth(@Param("employeeId") Long employeeId,
                                           @Param("year") Integer year,
                                           @Param("month") Integer month);

    /** 查询员工某年度所有薪资记录（用于累计预扣法） */
    List<SalaryRecord> selectByEmployeeAndYear(@Param("employeeId") Long employeeId,
                                                @Param("year") Integer year);

    /** 查询某部门某月薪资记录 */
    List<SalaryRecord> selectByDeptAndMonth(@Param("deptId") Long deptId,
                                             @Param("year") Integer year,
                                             @Param("month") Integer month);

    /** 更新状态 */
    int updateStatus(@Param("id") Long id, @Param("status") String status);
}
