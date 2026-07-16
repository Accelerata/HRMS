package com.hrms.mapper;

import com.hrms.entity.SysUser;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 系统用户 Mapper
 */
@Mapper
public interface SysUserMapper {

    @Select("SELECT * FROM sys_user WHERE username = #{username}")
    SysUser findByUsername(@Param("username") String username);

    @Select("SELECT * FROM sys_user WHERE id = #{id}")
    SysUser findById(@Param("id") Long id);

    /** 查找拥有指定角色的用户列表 */
    @Select("SELECT u.* FROM sys_user u " +
            "INNER JOIN sys_user_role ur ON u.id = ur.user_id " +
            "INNER JOIN sys_role r ON ur.role_id = r.id " +
            "WHERE r.role_code = #{roleCode} AND u.status = 1 " +
            "LIMIT 1")
    SysUser findFirstByRoleCode(@Param("roleCode") String roleCode);

    /** 新增系统用户（自动回填主键） */
    @Insert("INSERT INTO sys_user (username, password, status, login_fail_count, force_change_pwd, pwd_update_time, create_time) " +
            "VALUES (#{username}, #{password}, #{status}, #{loginFailCount}, #{forceChangePwd}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(SysUser user);

    /** 更新用户状态（禁用/启用） */
    @Update("UPDATE sys_user SET status = #{status}, update_time = NOW() WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    /** 更新密码 */
    @Update("UPDATE sys_user SET password = #{password}, force_change_pwd = 0, pwd_update_time = NOW(), update_time = NOW() WHERE id = #{id}")
    int updatePassword(@Param("id") Long id, @Param("password") String password);

    /** 更新用户信息 */
    @Update("UPDATE sys_user SET username = #{username}, password = #{password}, status = #{status}, " +
            "force_change_pwd = #{forceChangePwd}, pwd_update_time = #{pwdUpdateTime}, " +
            "last_login_time = #{lastLoginTime}, update_time = NOW() WHERE id = #{id}")
    int update(SysUser user);

    /** 按密码更新时间范围查找用户（用于密码到期提醒） */
    @Select("SELECT * FROM sys_user WHERE pwd_update_time >= #{start} AND pwd_update_time <= #{end} AND status = 1")
    List<SysUser> findByPwdUpdateTimeBetween(@Param("start") java.time.LocalDateTime start,
                                              @Param("end") java.time.LocalDateTime end);
}

