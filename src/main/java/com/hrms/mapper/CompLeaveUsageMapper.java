package com.hrms.mapper;

import com.hrms.entity.CompLeaveUsage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 调休占用 Mapper
 */
@Mapper
public interface CompLeaveUsageMapper {

    /** 插入占用记录 */
    int insert(CompLeaveUsage usage);

    /** 根据请假申请ID查询所有占用明细 */
    List<CompLeaveUsage> selectByApplicationId(@Param("applicationId") Long applicationId);

    /** 删除某申请的所有占用记录（回补时清理） */
    int deleteByApplicationId(@Param("applicationId") Long applicationId);
}
