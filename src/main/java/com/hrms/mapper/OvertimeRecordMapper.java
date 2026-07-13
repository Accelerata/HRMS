package com.hrms.mapper;

import com.hrms.entity.OvertimeRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 加班记录 Mapper
 */
@Mapper
public interface OvertimeRecordMapper {

    /** 根据员工ID查询未转换调休的加班记录 */
    List<OvertimeRecord> selectUnconvertedByEmployee(@Param("employeeId") Long employeeId);

    /** 根据员工ID查询所有加班记录 */
    List<OvertimeRecord> selectByEmployee(@Param("employeeId") Long employeeId,
                                           @Param("offset") int offset,
                                           @Param("limit") int limit);

    /** 插入 */
    int insert(OvertimeRecord record);

    /** 标记为已转换调休 */
    int markAsConverted(@Param("id") Long id);

    /**
     * 汇总某月加班小时数（用于薪资核算预警）
     * @param employeeId 员工ID
     * @param year       年份
     * @param month      月份
     * @return 加班小时合计
     */
    java.math.BigDecimal sumHoursByMonth(@Param("employeeId") Long employeeId,
                                          @Param("year") int year,
                                          @Param("month") int month);
}
