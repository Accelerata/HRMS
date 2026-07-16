package com.hrms.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 职级对招表
 * 定义各序列（M/P/S）下的职级编码与名称
 */
@Data
public class Grade {

    /** 主键ID */
    private Long id;

    /** 职位序列：M-管理 P-专业 S-支持 */
    private String sequence;

    /** 职级编码（如 P8、S4） */
    private String code;

    /** 职级名称（如 P8-资深专家） */
    private String name;

    /** 级别排序（数字越大级别越高） */
    private Integer levelOrder;

    /** 状态：0-禁用 1-正常 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
