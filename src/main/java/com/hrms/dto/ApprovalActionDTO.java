package com.hrms.dto;

import lombok.Data;
import jakarta.validation.constraints.*;

/**
 * 审批操作DTO（通用，适用于所有业务类型）
 */
@Data
public class ApprovalActionDTO {

    @NotNull(message = "审批动作不能为空")
    private Integer action;  // 1-通过 2-拒绝 3-退回

    /** 审批意见 */
    private String comment;

    // ── 调岗专用（区分原/新部门负责人审批） ──
    /** 审批角色: "old"=原部门负责人 / "new"=新部门负责人（仅调岗业务使用） */
    private String role;
}
