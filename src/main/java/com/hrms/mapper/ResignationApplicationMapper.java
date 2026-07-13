package com.hrms.mapper;

import com.hrms.entity.ResignationApplication;
import com.hrms.vo.ResignationVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 离职申请 Mapper
 */
@Mapper
public interface ResignationApplicationMapper {

    int insert(ResignationApplication entity);

    int update(ResignationApplication entity);

    ResignationApplication selectById(@Param("id") Long id);

    ResignationVO selectVOById(@Param("id") Long id);

    List<ResignationVO> selectPage(@Param("status") Integer status,
                                    @Param("keyword") String keyword,
                                    @Param("offset") int offset,
                                    @Param("size") int size);

    int countPage(@Param("status") Integer status,
                  @Param("keyword") String keyword);
}
