package com.hrms.mapper;

import com.hrms.entity.SalaryBatch;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 薪资批次 Mapper
 */
@Mapper
public interface SalaryBatchMapper {

    /** 插入（回填主键） */
    int insert(SalaryBatch batch);

    /** 根据ID查询 */
    SalaryBatch selectById(@Param("id") Long id);

    /** 根据学年月查询（唯一约束 uk_year_month） */
    SalaryBatch selectByYearMonth(@Param("year") Integer year, @Param("month") Integer month);

    /** 查询批次列表（倒序） */
    List<SalaryBatch> selectList();

    /** 更新状态 */
    int updateStatus(@Param("id") Long id, @Param("status") String status);

    /** 更新批次统计（人数与实发合计） */
    int updateAggregates(@Param("id") Long id,
                         @Param("employeeCount") Integer employeeCount,
                         @Param("totalNetPay") java.math.BigDecimal totalNetPay);

    /** 更新提交人 */
    int updateSubmitter(@Param("id") Long id, @Param("submitterId") Long submitterId);
}
