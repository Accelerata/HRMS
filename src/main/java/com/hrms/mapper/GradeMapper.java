package com.hrms.mapper;

import com.hrms.entity.Grade;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 职级 Mapper
 */
@Mapper
public interface GradeMapper {

    /** 查询所有职级 */
    List<Grade> selectAll();

    /** 根据序列查询 */
    List<Grade> selectBySequence(@Param("sequence") String sequence);

    /** 根据编码查询 */
    Grade selectByCode(@Param("code") String code);

    /** 根据ID查询 */
    Grade selectById(@Param("id") Long id);

    /** 插入 */
    int insert(Grade grade);

    /** 更新 */
    int update(Grade grade);

    /** 删除 */
    int deleteById(@Param("id") Long id);
}
