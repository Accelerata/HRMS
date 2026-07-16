package com.hrms.mapper;

import com.hrms.entity.ShiftSchedule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 排班制班次 Mapper
 */
@Mapper
public interface ShiftScheduleMapper {

    /** 根据考勤组ID查询所有班次 */
    List<ShiftSchedule> selectByGroupId(@Param("groupId") Long groupId);

    /** 根据ID查询 */
    ShiftSchedule selectById(@Param("id") Long id);

    /** 插入 */
    int insert(ShiftSchedule schedule);

    /** 批量插入 */
    int insertBatch(@Param("list") List<ShiftSchedule> list);

    /** 更新 */
    int update(ShiftSchedule schedule);

    /** 根据考勤组ID删除所有班次 */
    int deleteByGroupId(@Param("groupId") Long groupId);

    /** 根据ID删除 */
    int deleteById(@Param("id") Long id);
}
