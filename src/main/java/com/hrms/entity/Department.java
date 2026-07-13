package com.hrms.entity;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 部门表
 * 支持最高 5 级树形结构
 */
@Data
public class Department {

    /** 主键ID */
    private Long id;

    /** 上级部门ID（0 表示根部门） */
    private Long parentId;

    /** 部门名称 */
    private String deptName;

    /** 部门编码 */
    private String deptCode;

    /** 排序号 */
    private Integer sortOrder;

    /** 层级深度（1-5） */
    private Integer level;

    /** 状态：0-禁用，1-正常 */
    private Integer status;

    /** 负责人ID（关联 employee.id） */
    private Long managerId;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    // ── 非数据库字段（查询时填充） ──

    /** 子部门列表 */
    private List<Department> children;

    /** 在职员工数（试用期 + 正式） */
    private Integer employeeCount;

    /** 试用期员工数 */
    private Integer probationCount;

    /** 正式员工数 */
    private Integer regularCount;
}
