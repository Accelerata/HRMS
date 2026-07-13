package com.hrms.mapper;

import com.hrms.entity.Department;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 部门 Mapper
 */
@Mapper
public interface DepartmentMapper {

    /** 查询所有部门 */
    List<Department> selectAll();

    /** 根据ID查询 */
    Department selectById(@Param("id") Long id);

    /** 根据父ID查询子部门 */
    List<Department> selectByParentId(@Param("parentId") Long parentId);

    /** 查询某层级下的所有部门 */
    List<Department> selectByLevel(@Param("level") Integer level);

    /** 统计子部门数量 */
    int countByParentId(@Param("parentId") Long parentId);

    /** 插入 */
    int insert(Department dept);

    /** 更新 */
    int update(Department dept);

    /** 删除 */
    int deleteById(@Param("id") Long id);

    /** 校验部门编码唯一性 */
    int countByCode(@Param("deptCode") String deptCode, @Param("excludeId") Long excludeId);

    /** 校验部门名称同层级唯一性 */
    int countByNameAndParent(@Param("deptName") String deptName,
                             @Param("parentId") Long parentId,
                             @Param("excludeId") Long excludeId);
}
