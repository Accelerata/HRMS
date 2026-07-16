package com.hrms.mapper;

import com.hrms.entity.LeaveAttachment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 请假附件 Mapper
 */
@Mapper
public interface LeaveAttachmentMapper {

    /** 根据ID查询 */
    LeaveAttachment selectById(@Param("id") Long id);

    /** 根据请假申请ID查询附件列表 */
    List<LeaveAttachment> selectByApplicationId(@Param("applicationId") Long applicationId);

    /** 插入 */
    int insert(LeaveAttachment attachment);

    /** 回写 application_id（绑定到申请） */
    int bindToApplication(@Param("id") Long id, @Param("applicationId") Long applicationId);
}
