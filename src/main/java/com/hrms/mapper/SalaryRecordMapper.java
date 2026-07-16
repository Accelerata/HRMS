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

    /** 根据ID查询 */
    SalaryRecord selectById(@Param("id") Long id);

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

    /** 批次内记录批量更新状态（批次审批通过/拒绝时使用） */
    int updateStatusByBatch(@Param("batchId") Long batchId,
                            @Param("fromStatus") String fromStatus,
                            @Param("status") String status);

    /** 删除批次内全部记录（重新核算前清理） */
    int deleteByBatch(@Param("batchId") Long batchId);

    /** 批次内记录数 */
    int countByBatch(@Param("batchId") Long batchId);

    /** 查询批次内全部记录 */
    List<SalaryRecord> selectByBatch(@Param("batchId") Long batchId);
}
