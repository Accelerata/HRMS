package com.hrms.mapper;

import com.hrms.entity.TransferApplication;
import com.hrms.vo.TransferVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 调岗申请 Mapper
 */
@Mapper
public interface TransferApplicationMapper {

    int insert(TransferApplication entity);

    int update(TransferApplication entity);

    TransferApplication selectById(@Param("id") Long id);

    TransferVO selectVOById(@Param("id") Long id);

    List<TransferVO> selectPage(@Param("status") Integer status,
                                 @Param("keyword") String keyword,
                                 @Param("offset") int offset,
                                 @Param("size") int size);

    int countPage(@Param("status") Integer status,
                  @Param("keyword") String keyword);
}
