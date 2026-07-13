package com.hrms.mapper;

import com.hrms.entity.AttendanceGroup;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 考勤组 Mapper
 */
@Mapper
public interface AttendanceGroupMapper {

    /** 查询所有考勤组 */
    List<AttendanceGroup> selectAll();

    /** 根据ID查询 */
    AttendanceGroup selectById(@Param("id") Long id);

    /** 根据名称查询 */
    AttendanceGroup selectByName(@Param("groupName") String groupName);

    /** 插入 */
    int insert(AttendanceGroup group);

    /** 更新 */
    int update(AttendanceGroup group);

    /** 删除 */
    int deleteById(@Param("id") Long id);
}
