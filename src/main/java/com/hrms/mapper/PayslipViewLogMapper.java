package com.hrms.mapper;

import com.hrms.entity.PayslipViewLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PayslipViewLogMapper {
    int insert(PayslipViewLog log);
    List<PayslipViewLog> selectByEmployeeAndRecord(@Param("employeeId") Long employeeId,
                                                     @Param("recordId") Long recordId);
}
