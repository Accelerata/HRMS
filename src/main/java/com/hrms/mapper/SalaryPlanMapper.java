package com.hrms.mapper;

import com.hrms.entity.SalaryPlan;
import com.hrms.entity.SalaryPlanItem;
import com.hrms.entity.SalaryPlanScope;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SalaryPlanMapper {
    SalaryPlan selectById(@Param("id") Long id);
    List<SalaryPlan> selectAll();
    List<SalaryPlan> selectEnabled();
    int insert(SalaryPlan plan);
    int update(SalaryPlan plan);
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    // plan items
    List<SalaryPlanItem> selectItemsByPlanId(@Param("planId") Long planId);
    int insertItem(SalaryPlanItem item);
    int updateItem(SalaryPlanItem item);
    int deleteItem(@Param("id") Long id);

    // plan scope
    List<SalaryPlanScope> selectScopesByPlanId(@Param("planId") Long planId);
    int insertScope(SalaryPlanScope scope);
    int deleteScope(@Param("id") Long id);

    /** 按员工部门/职位/职级匹配有效账套（按优先级降序取第一个） */
    SalaryPlan matchPlan(@Param("deptId") Long deptId,
                         @Param("positionId") Long positionId,
                         @Param("grade") String grade);
}
