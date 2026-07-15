package com.hrms.mapper;

import com.hrms.entity.OnboardingApplication;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 入职申请 Mapper
 */
@Mapper
public interface OnboardingApplicationMapper {

    int insert(OnboardingApplication entity);

    int update(OnboardingApplication entity);

    OnboardingApplication selectById(@Param("id") Long id);

    /** 分页查询（含部门名称等关联字段VO） */
    List<com.hrms.vo.OnboardingVO> selectPage(@Param("status") Integer status,
                                               @Param("keyword") String keyword,
                                               @Param("offset") int offset,
                                               @Param("size") int size);

    int countPage(@Param("status") Integer status,
                  @Param("keyword") String keyword);

    /** 根据状态查询列表 */
    List<OnboardingApplication> selectByStatus(@Param("status") Integer status);

    /** 根据ID删除 */
    int deleteById(@Param("id") Long id);
}
