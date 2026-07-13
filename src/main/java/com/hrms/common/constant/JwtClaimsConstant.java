package com.hrms.common.constant;

/**
 * JWT Claims 常量
 */
public class JwtClaimsConstant {

    /** 用户ID */
    public static final String USER_ID = "userId";

    /** 用户名 */
    public static final String USERNAME = "username";

    /** 员工ID（可能为null，管理员没有employee） */
    public static final String EMPLOYEE_ID = "employeeId";

    /** 角色编码（如 ROLE_HR） */
    public static final String ROLE_CODE = "roleCode";

    /** 数据权限范围（1-5） */
    public static final String DATA_SCOPE = "dataScope";

    /** 所属部门ID */
    public static final String DEPT_ID = "deptId";

    /** 权限码列表（逗号分隔，如 dept:view,emp:view） */
    public static final String PERMISSIONS = "permissions";

}
