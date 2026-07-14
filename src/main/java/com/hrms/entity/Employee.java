package com.hrms.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 员工表
 */
@Data
public class Employee {

    /** 主键ID */
    private Long id;

    /** 员工工号（系统自动生成，格式：年份4位+部门编码2位+序号3位） */
    private String employeeNo;

    /** 姓名 */
    private String name;

    // ────────────── 个人信息 ──────────────

    /** 性别: 0-未知 1-男 2-女 */
    private Integer gender;

    /** 手机号（与登录账号一致） */
    private String phone;

    /** 邮箱 */
    private String email;

    /** 身份证号 */
    private String idCard;

    /** 生日（可从身份证号自动提取） */
    private LocalDate birthday;

    /** 户籍地址 */
    private String registeredAddress;

    /** 现居住地址 */
    private String currentAddress;

    // ────────────── 部门与职位 ──────────────

    /** 所属部门ID */
    private Long deptId;

    /** 职位ID */
    private Long positionId;

    /** 职级（如 P3、M5） */
    private String grade;

    // ────────────── 工作信息 ──────────────

    /** 直接汇报人ID（关联 employee.id） */
    private Long reportTo;

    /** 工作地点 */
    private String workLocation;

    /** 入职类型: 1-社招 2-校招 3-内推 4-调动 */
    private Integer entryType;

    // ────────────── 薪资信息 ──────────────

    /** 薪资账套ID */
    private Long salaryAccountId;

    /** 基本工资（精确到分） */
    private BigDecimal baseSalary;

    /** 银行账号 */
    private String bankAccount;

    /** 开户行 */
    private String bankName;

    // ────────────── 加密哈希索引（用于精确查询）──────────────

    /** 手机号哈希 */
    private String phoneHash;

    /** 身份证号哈希 */
    private String idCardHash;

    /** 银行账号哈希 */
    private String bankAccountHash;

    /** 加密版本号 */
    private Integer encryptionVersion;

    // ────────────── 状态与时间 ──────────────

    /**
     * 员工状态：
     * 0-待入职, 1-试用期, 2-正式, 3-待离职, 4-已离职
     */
    private Integer status;

    /** 入职日期 */
    private LocalDate entryDate;

    /** 转正日期 */
    private LocalDate regularDate;

    /** 离职日期 */
    private LocalDate resignDate;

    /** 关联系统用户ID（sys_user.id，入职审批通过后回填） */
    private Long userId;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
