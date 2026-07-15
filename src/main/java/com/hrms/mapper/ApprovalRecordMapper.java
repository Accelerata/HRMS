package com.hrms.mapper;

import com.hrms.entity.ApprovalRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 审批记录 Mapper
 */
@Mapper
public interface ApprovalRecordMapper {

    /** 批量插入审批记录 */
    int insertBatch(@Param("list") List<ApprovalRecord> list);

    /** 根据业务查询所有审批记录 */
    List<ApprovalRecord> selectByBusiness(@Param("businessType") Integer businessType,
                                          @Param("businessId") Long businessId);

    /** 查询某业务的待审批记录 */
    List<ApprovalRecord> selectPendingByBusiness(@Param("businessType") Integer businessType,
                                                  @Param("businessId") Long businessId);

    /** 更新审批结果 */
    int updateAction(@Param("id") Long id,
                     @Param("action") Integer action,
                     @Param("comment") String comment,
                     @Param("isPending") Integer isPending);

    /** 查询当前用户的待办列表 */
    List<ApprovalRecord> selectTodoByApprover(@Param("approverId") Long approverId);

    /** 查询当前用户的已办列表 */
    List<ApprovalRecord> selectDoneByApprover(@Param("approverId") Long approverId);

    /** 根据ID查询 */
    ApprovalRecord selectById(@Param("id") Long id);

    /** 查询是否有更低 step_order 的未处理记录（顺序门控） */
    int countLowerPending(@Param("businessType") Integer businessType,
                          @Param("businessId") Long businessId,
                          @Param("stepOrder") Integer stepOrder);

    /** 查询超时未处理的审批记录（due_time < now 且 is_pending=1） */
    List<ApprovalRecord> selectOverduePending();
}
