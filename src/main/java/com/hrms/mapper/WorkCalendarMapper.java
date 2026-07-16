package com.hrms.mapper;

import com.hrms.entity.WorkCalendar;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * 工作日历 Mapper
 */
@Mapper
public interface WorkCalendarMapper {

    /** 根据日期查询 */
    WorkCalendar selectByDate(@Param("calendarDate") LocalDate calendarDate);

    /** 根据年份查询 */
    List<WorkCalendar> selectByYear(@Param("year") int year);

    /** 插入 */
    int insert(WorkCalendar workCalendar);

    /** 更新 */
    int update(WorkCalendar workCalendar);

    /** 根据日期删除 */
    int deleteByDate(@Param("calendarDate") LocalDate calendarDate);

    /**
     * 批量插入或更新（ON DUPLICATE KEY UPDATE）
     * 同一天已有记录时更新 day_type 与 name
     */
    int batchUpsert(@Param("list") List<WorkCalendar> list);
}
