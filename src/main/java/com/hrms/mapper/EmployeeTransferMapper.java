package com.hrms.mapper;

import com.hrms.entity.EmployeeTransfer;
import com.hrms.vo.EmployeeTransferVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 异动日志 Mapper
 */
@Mapper
public interface EmployeeTransferMapper {

    int insert(EmployeeTransfer entity);

    /** 根据员工ID查询异动日志 */
    List<EmployeeTransferVO> selectByEmployee(@Param("employeeId") Long employeeId,
                                               @Param("transferType") Integer transferType);
}
