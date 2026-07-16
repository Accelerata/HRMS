package com.hrms.mapper;

import com.hrms.entity.CompLeaveGrant;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * 调休入账 Mapper
 */
@Mapper
public interface CompLeaveGrantMapper {

    /** 根据ID查询 */
    CompLeaveGrant selectById(@Param("id") Long id);

    /** 插入入账记录 */
    int insert(CompLeaveGrant grant);

    /** 按员工+有效状态+过期日升序查询（FIFO 消耗用） */
    List<CompLeaveGrant> selectValidByEmployee(@Param("employeeId") Long employeeId);

    /** 更新已使用天数 */
    int updateUsedDays(@Param("id") Long id, @Param("usedDays") java.math.BigDecimal usedDays);

    /** 查询已过期但仍有效的入账（过期清零用） */
    List<CompLeaveGrant> selectExpiredButActive(@Param("today") LocalDate today);

    /** 将入账置为已过期 */
    int markExpired(@Param("id") Long id);
}
