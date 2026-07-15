package com.hrms.mapper;

import com.hrms.entity.SysRole;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 系统角色 Mapper
 */
@Mapper
public interface SysRoleMapper {

    @Select("SELECT r.* FROM sys_role r " +
            "INNER JOIN sys_user_role ur ON r.id = ur.role_id " +
            "WHERE ur.user_id = #{userId} AND r.status = 1 LIMIT 1")
    SysRole findByUserId(@Param("userId") Long userId);

    @Select("SELECT * FROM sys_role WHERE role_code = #{roleCode} AND status = 1 LIMIT 1")
    SysRole findByRoleCode(@Param("roleCode") String roleCode);

    @Insert("INSERT INTO sys_user_role (user_id, role_id) VALUES (#{userId}, #{roleId})")
    int insertUserRole(@Param("userId") Long userId, @Param("roleId") Long roleId);
}
