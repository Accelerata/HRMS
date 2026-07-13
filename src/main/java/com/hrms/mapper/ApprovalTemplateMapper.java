package com.hrms.mapper;

import com.hrms.entity.ApprovalTemplate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 审批模板 Mapper
 */
@Mapper
public interface ApprovalTemplateMapper {

    /** 根据业务类型查询审批步骤 */
    List<ApprovalTemplate> selectByBusinessType(@Param("businessType") Integer businessType);
}
