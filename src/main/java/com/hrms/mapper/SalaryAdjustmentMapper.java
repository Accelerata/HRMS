package com.hrms.mapper;

import com.hrms.entity.SalaryAdjustment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SalaryAdjustmentMapper {
    int insert(SalaryAdjustment adjustment);
    List<SalaryAdjustment> selectByBatchId(@Param("batchId") Long batchId);
    List<SalaryAdjustment> selectByEmployeeAndBatch(@Param("employeeId") Long employeeId,
                                                     @Param("batchId") Long batchId);
    int deleteById(@Param("id") Long id);
    int deleteByBatchId(@Param("batchId") Long batchId);
}
