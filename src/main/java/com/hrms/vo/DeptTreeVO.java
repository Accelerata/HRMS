package com.hrms.vo;

import com.hrms.entity.Department;
import lombok.Data;

import java.util.List;

/**
 * 部门树 VO（含实时人数统计）
 */
@Data
public class DeptTreeVO {

    /** 部门ID */
    private Long id;

    /** 上级部门ID */
    private Long parentId;

    /** 部门名称 */
    private String deptName;

    /** 部门编码 */
    private String deptCode;

    /** 排序号 */
    private Integer sortOrder;

    /** 层级深度 */
    private Integer level;

    /** 状态 */
    private Integer status;

    /** 负责人ID */
    private Long managerId;

    /** 负责人姓名 */
    private String managerName;

    /** 在职员工总数（试用期 + 正式） */
    private Integer employeeCount;

    /** 试用期员工数 */
    private Integer probationCount;

    /** 正式员工数 */
    private Integer regularCount;

    /** 子部门列表 */
    private List<DeptTreeVO> children;
}
