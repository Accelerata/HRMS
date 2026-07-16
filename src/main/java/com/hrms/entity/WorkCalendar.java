package com.hrms.entity;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 工作日历（法定节假日/调班工作日）
 *
 * 默认周一至周五为工作日、周六日休息；本表仅存储例外：
 * day_type=1 法定节假日/休息 → 非工作日
 * day_type=2 调班工作日 → 工作日
 */
@Data
public class WorkCalendar {

    /** 主键ID */
    private Long id;

    /** 日期 */
    private LocalDate calendarDate;

    /** 类型：1-法定节假日/休息 2-调班工作日 */
    private Integer dayType;

    /** 节日/调班说明 */
    private String name;

    /** 年份 */
    private Integer year;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
