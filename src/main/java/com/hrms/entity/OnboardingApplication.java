package com.hrms.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 入职申请表
 */
@Data
public class OnboardingApplication {

    private Long id;
    /** 候选人姓名 */
    private String realName;
    /** 手机号 */
    private String phone;
    /** 邮箱 */
    private String email;
    /** 身份证号 */
    private String idCard;
    /** 目标部门ID */
    private Long targetDeptId;
    /** 目标职位ID */
    private Long targetPositionId;
    /** 入职薪酬 */
    private BigDecimal offerSalary;
    /** 试用期月数 */
    private Integer probationMonths;
    /** 预计入职日期 */
    private LocalDate entryDate;
    /** 状态: 0-草稿 1-审批中 2-已通过 3-已拒绝 4-待入职 5-已入职 */
    private Integer status;
    /** 入职后关联员工ID */
    private Long employeeId;
    /** 提交人ID */
    private Long submitterId;
    /** 创建时间 */
    private LocalDateTime createTime;
    /** 更新时间 */
    private LocalDateTime updateTime;

    // ── 入职时需要写入的员工扩展信息 ──
    /** 性别: 0-未知 1-男 2-女 */
    private Integer gender;
    /** 户籍地址 */
    private String registeredAddress;
    /** 现居住地址 */
    private String currentAddress;
    /** 职级（如 P3、M5） */
    private String grade;
    /** 直接汇报人ID */
    private Long reportTo;
    /** 工作地点 */
    private String workLocation;
    /** 入职类型: 1-社招 2-校招 3-内推 4-调动 */
    private Integer entryType;
    /** 薪资账套ID */
    private Long salaryAccountId;
    /** 银行账号 */
    private String bankAccount;
    /** 开户行 */
    private String bankName;
}
