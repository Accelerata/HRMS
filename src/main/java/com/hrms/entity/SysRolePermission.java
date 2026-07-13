package com.hrms.entity;

import lombok.Data;

/**
 * 角色权限关联表
 */
@Data
public class SysRolePermission {

    /** 角色ID */
    private Long roleId;

    /** 权限ID */
    private Long permissionId;
}
