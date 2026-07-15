package com.hrms.mapper;

import com.hrms.entity.GradeSalaryRange;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 职级薪资范围 Mapper
 */
@Mapper
public interface GradeSalaryRangeMapper {

    /** 查询所有启用的薪资范围 */
    List<GradeSalaryRange> selectAll();

    /** 根据职级编码查询薪资范围 */
    GradeSalaryRange selectByGradeCode(@Param("gradeCode") String gradeCode);

    /** 插入 */
    int insert(GradeSalaryRange range);

    /** 更新 */
    int update(GradeSalaryRange range);

    /** 删除 */
    int deleteById(@Param("id") Long id);
}
