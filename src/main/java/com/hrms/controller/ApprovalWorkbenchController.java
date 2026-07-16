package com.hrms.controller;

import com.hrms.annotation.RequirePermission;
import com.hrms.dto.ApprovalDelegationDTO;
import com.hrms.dto.ApprovalTransferDTO;
import com.hrms.entity.ApprovalDelegation;
import com.hrms.result.Result;
import com.hrms.service.ApprovalDelegationService;
import com.hrms.service.ApprovalWorkbenchService;
import com.hrms.vo.ApprovalDetailVO;
import com.hrms.vo.ApprovalDoneVO;
import com.hrms.vo.ApprovalTodoVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 审批工作台 Controller（需求 8.2/8.3）
 *
 * 权限：approval:workbench（种子权限码，HR/部门主管/财务专员持有）
 * 审批操作（通过/拒绝）仍由各业务 Controller 承担
 */
@RestController
@RequestMapping("/api/v1/approvals")
@RequiredArgsConstructor
public class ApprovalWorkbenchController {

    private final ApprovalWorkbenchService workbenchService;
    private final ApprovalDelegationService delegationService;

    /** 我的待办（8.2.1：申请人/申请类型/申请时间/截止时间） */
    @GetMapping("/todo")
    @RequirePermission("approval:workbench")
    public Result<List<ApprovalTodoVO>> todo() {
        return Result.success(workbenchService.getTodoList());
    }

    /** 我的已办 */
    @GetMapping("/done")
    @RequirePermission("approval:workbench")
    public Result<List<ApprovalDoneVO>> done() {
        return Result.success(workbenchService.getDoneList());
    }

    /** 统一审批详情（8.2.2：申请信息 + 审批历史 + 可操作项） */
    @GetMapping("/detail/{businessType}/{businessId}")
    @RequirePermission({"approval:workbench", "approval:view"})
    public Result<ApprovalDetailVO> detail(@PathVariable Integer businessType,
                                           @PathVariable Long businessId) {
        return Result.success(workbenchService.getDetail(businessType, businessId));
    }

    /** 转交审批任务（8.2.1 操作） */
    @PostMapping("/records/{id}/transfer")
    @RequirePermission("approval:workbench")
    public Result<Void> transfer(@PathVariable Long id, @Valid @RequestBody ApprovalTransferDTO dto) {
        workbenchService.transfer(id, dto);
        return Result.success();
    }

    // ═══════════════ 委托审批（8.3） ═══════════════

    /** 设置委托 */
    @PostMapping("/delegations")
    @RequirePermission("approval:workbench")
    public Result<ApprovalDelegation> createDelegation(@Valid @RequestBody ApprovalDelegationDTO dto) {
        return Result.success(delegationService.create(dto));
    }

    /** 取消委托 */
    @DeleteMapping("/delegations/{id}")
    @RequirePermission("approval:workbench")
    public Result<Void> cancelDelegation(@PathVariable Long id) {
        delegationService.cancel(id);
        return Result.success();
    }

    /** 查询我的委托 */
    @GetMapping("/delegations/my")
    @RequirePermission("approval:workbench")
    public Result<List<ApprovalDelegation>> myDelegations() {
        return Result.success(delegationService.myDelegations());
    }
}
