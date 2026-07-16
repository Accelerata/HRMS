package com.hrms.mapper;

import com.hrms.entity.SalaryChangeHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SalaryChangeHistoryMapper {
    int insert(SalaryChangeHistory history);
    List<SalaryChangeHistory> selectByEmployeeId(@Param("employeeId") Long employeeId);
    List<SalaryChangeHistory> selectByAccountId(@Param("accountId") Long accountId);
}
