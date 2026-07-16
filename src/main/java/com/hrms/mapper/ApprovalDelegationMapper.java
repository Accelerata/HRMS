package com.hrms.mapper;

import com.hrms.entity.ApprovalDelegation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 审批委托 Mapper
 */
@Mapper
public interface ApprovalDelegationMapper {

    /** 插入委托（回填主键） */
    int insert(ApprovalDelegation delegation);

    /** 根据ID查询 */
    ApprovalDelegation selectById(@Param("id") Long id);

    /** 查询某用户的全部委托（含已取消，倒序） */
    List<ApprovalDelegation> selectByDelegator(@Param("delegatorId") Long delegatorId);

    /** 查询某审批人当前生效中的委托（status=1 且 now 处于起止区间内） */
    ApprovalDelegation selectActiveByDelegator(@Param("delegatorId") Long delegatorId,
                                               @Param("now") LocalDateTime now);

    /** 统计同一委托人时间区间重叠的生效委托数 */
    int countOverlapping(@Param("delegatorId") Long delegatorId,
                         @Param("startTime") LocalDateTime startTime,
                         @Param("endTime") LocalDateTime endTime);

    /** 取消委托 */
    int cancel(@Param("id") Long id);
}
