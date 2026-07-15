package com.hrms.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 登录响应 VO
 */
@Data
@Builder
public class LoginVO {

    /** JWT Token */
    private String token;

    /** 用户ID */
    private Long userId;

    /** 用户名 */
    private String username;

    /** 员工ID */
    private Long employeeId;

    /** 角色编码 */
    private String roleCode;

    /** 角色名称 */
    private String roleName;

    /** 数据权限范围 */
    private Integer dataScope;

    /** 权限码列表 */
    private List<String> permissions;

    /** 是否需要强制改密（首次登录） */
    private Boolean needChangePwd;
}
