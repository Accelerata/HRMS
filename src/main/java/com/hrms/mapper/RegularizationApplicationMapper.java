package com.hrms.mapper;

import com.hrms.entity.RegularizationApplication;
import com.hrms.vo.RegularizationVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 转正申请 Mapper
 */
@Mapper
public interface RegularizationApplicationMapper {

    int insert(RegularizationApplication entity);

    int update(RegularizationApplication entity);

    RegularizationApplication selectById(@Param("id") Long id);

    RegularizationVO selectVOById(@Param("id") Long id);

    List<RegularizationVO> selectPage(@Param("status") Integer status,
                                       @Param("keyword") String keyword,
                                       @Param("offset") int offset,
                                       @Param("size") int size);

    int countPage(@Param("status") Integer status,
                  @Param("keyword") String keyword);
}
