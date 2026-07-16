package com.hrms.mapper;

import com.hrms.entity.SupplementaryCardApplication;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * 补卡申请 Mapper
 */
@Mapper
public interface SupplementaryCardMapper {

    /** 插入（回填主键） */
    int insert(SupplementaryCardApplication application);

    /** 根据ID查询 */
    SupplementaryCardApplication selectById(@Param("id") Long id);

    /** 查询员工补卡申请（倒序） */
    List<SupplementaryCardApplication> selectByEmployee(@Param("employeeId") Long employeeId);

    /** 统计同人同日同卡型未拒绝的申请数（防重复申请） */
    int countActiveByEmployeeDateType(@Param("employeeId") Long employeeId,
                                      @Param("attendanceDate") LocalDate attendanceDate,
                                      @Param("cardType") Integer cardType);

    /** 更新状态 */
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);
}
