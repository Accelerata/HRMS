package com.hrms.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 员工详情 VO（含部门名、职位名等关联查询字段）
 * 薪资字段根据用户角色权限过滤
 */
@Data
public class EmployeeVO {

    /** 主键ID */
    private Long id;

    /** 员工工号 */
    private String employeeNo;

    /** 姓名 */
    private String name;

    // ── 个人信息 ──

    /** 性别: 0-未知 1-男 2-女 */
    private Integer gender;

    /** 手机号 */
    private String phone;

    /** 邮箱 */
    private String email;

    /** 身份证号 */
    private String idCard;

    /** 生日 */
    private LocalDate birthday;

    /** 户籍地址 */
    private String registeredAddress;

    /** 现居住地址 */
    private String currentAddress;

    // ── 工作信息 ──

    /** 所属部门ID */
    private Long deptId;

    /** 所属部门名称 */
    private String deptName;

    /** 职位ID */
    private Long positionId;

    /** 职位名称 */
    private String positionName;

    /** 职级（如 P3、M5） */
    private String grade;

    /** 直接汇报人ID */
    private Long reportTo;

    /** 直接汇报人姓名 */
    private String reportToName;

    /** 工作地点 */
    private String workLocation;

    /** 入职类型: 1-社招 2-校招 3-内推 4-调动 */
    private Integer entryType;

    /** 入职类型文字描述 */
    private String entryTypeLabel;

    // ── 薪资信息（按角色过滤：仅 HR/财务可见） ──

    /** 薪资账套ID */
    private Long salaryAccountId;

    /** 基本工资 */
    private BigDecimal baseSalary;

    /** 银行账号 */
    private String bankAccount;

    /** 开户行 */
    private String bankName;

    // ── 状态与时间 ──

    /**
     * 员工状态：
     * 0-待入职, 1-试用期, 2-正式, 3-待离职, 4-已离职
     */
    private Integer status;

    /** 状态文字 */
    private String statusLabel;

    /** 入职日期 */
    private LocalDate entryDate;

    /** 转正日期 */
    private LocalDate regularDate;

    /** 离职日期 */
    private LocalDate resignDate;

    /** 关联用户ID */
    private Long userId;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
