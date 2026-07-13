package com.hrms.mapper;

import com.hrms.entity.Position;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 职位 Mapper
 */
@Mapper
public interface PositionMapper {

    /** 查询所有职位 */
    List<Position> selectAll();

    /** 根据序列查询 */
    List<Position> selectBySequence(@Param("sequence") String sequence);

    /** 根据ID查询 */
    Position selectById(@Param("id") Long id);

    /** 插入 */
    int insert(Position position);

    /** 更新 */
    int update(Position position);

    /** 删除 */
    int deleteById(@Param("id") Long id);

    /** 校验编码唯一性 */
    int countByCode(@Param("positionCode") String positionCode, @Param("excludeId") Long excludeId);
}
