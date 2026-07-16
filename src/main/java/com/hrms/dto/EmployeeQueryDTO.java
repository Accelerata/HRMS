package com.hrms.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 员工高级搜索查询参数
 */
@Data
public class EmployeeQueryDTO {

    /** 关键词：姓名/工号模糊匹配 */
    private String keyword;

    /** 手机号（明文，Service层计算hash后匹配） */
    private String phone;

    /** 部门ID列表（多选） */
    private List<Long> deptIds;

    /** 职位ID列表（多选） */
    private List<Long> positionIds;

    /** 在职状态列表（多选，如 1-试用期 2-正式） */
    private List<Integer> statuses;

    /** 职级列表（多选，如 P5, P6, P7） */
    private List<String> grades;

    /** 入职日期开始（含） */
    private LocalDate startDate;

    /** 入职日期截止（含） */
    private LocalDate endDate;
}
