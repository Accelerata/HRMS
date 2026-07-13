package com.hrms.common.constant;

/**
 * 通用提示信息常量
 */
public class MessageConstant {

    // ── 通用 ──
    public static final String SUCCESS = "操作成功";
    public static final String ERROR = "操作失败";
    public static final String PARAM_INVALID = "请求参数无效";

    // ── 认证 ──
    public static final String TOKEN_INVALID = "Token 无效或已过期";
    public static final String TOKEN_MISSING = "未携带认证 Token";
    public static final String LOGIN_FAILED = "用户名或密码错误";
    public static final String ACCOUNT_LOCKED = "账号已被锁定，请联系管理员";
    public static final String ACCOUNT_DISABLED = "账号已被禁用";
    public static final String PASSWORD_EXPIRED = "密码已过期，请修改密码";
    public static final String UNAUTHORIZED = "无权访问";

    // ── 用户 ──
    public static final String USERNAME_EXISTS = "用户名已存在";
    public static final String USER_NOT_FOUND = "用户不存在";

    // ── 角色 ──
    public static final String ROLE_HAS_USERS = "该角色下存在关联用户，无法删除";

    // ── 部门 ──
    public static final String DEPT_HAS_CHILDREN = "该部门下存在子部门，无法删除";
    public static final String DEPT_HAS_EMPLOYEES = "该部门下存在在职员工，无法删除";
    public static final String DEPT_DEPTH_EXCEED = "部门层级已达上限（5级），无法创建子部门";

    // ── 员工 ──
    public static final String EMPLOYEE_NOT_FOUND = "员工不存在";
    public static final String EMPLOYEE_NO_EXISTS = "工号已存在";

    // ── 审批 ──
    public static final String APPROVAL_STATUS_INVALID = "当前状态不允许此操作";
    public static final String APPROVAL_DUPLICATE = "您已审批过该申请";

    // ── 假期 ──
    public static final String LEAVE_BALANCE_INSUFFICIENT = "假期余额不足";
    public static final String LEAVE_DATE_OVERLAP = "与已批准的请假日期重叠";

    // ── 薪资 ──
    public static final String SALARY_NOT_SET = "员工薪资未设置";
    public static final String SALARY_SLIP_ALREADY_GENERATED = "该月工资条已生成";

}
