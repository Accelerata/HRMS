package com.hrms.mapper;

import com.hrms.entity.SysRole;
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
}
