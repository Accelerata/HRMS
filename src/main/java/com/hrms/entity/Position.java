package com.hrms.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 职位表
 * 支持 M (管理)、P (专业)、S (支持) 序列
 */
@Data
public class Position {

    /** 主键ID */
    private Long id;

    /** 职位名称 */
    private String positionName;

    /** 职位编码 */
    private String positionCode;

    /**
     * 职位序列：
     * M - 管理序列 (Management)
     * P - 专业序列 (Professional)
     * S - 支持序列 (Support)
     */
    private String sequence;

    /** 职级范围（如 "P1-P5"、"M1-M3"） */
    private String gradeRange;

    /** 默认试用期（月） */
    private Integer defaultProbationMonths;

    /** 职位描述 */
    private String description;

    /** 状态：0-禁用，1-正常 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
