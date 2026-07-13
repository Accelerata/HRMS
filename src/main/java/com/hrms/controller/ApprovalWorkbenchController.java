package com.hrms.controller;

import com.hrms.annotation.RequirePermission;
import com.hrms.result.Result;
import com.hrms.service.ApprovalWorkbenchService;
import com.hrms.vo.ApprovalDoneVO;
import com.hrms.vo.ApprovalTodoVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 审批工作台 Controller（待办/已办）
 *
 * 权限：持有任一入转调离管理权限即可访问
 */
@RestController
@RequestMapping("/api/v1/approvals")
@RequiredArgsConstructor
@RequirePermission({"onboarding:manage", "regularization:manage", "transfer:manage", "resignation:manage"})
public class ApprovalWorkbenchController {

    private final ApprovalWorkbenchService workbenchService;

    @GetMapping("/todo")
    public Result<List<ApprovalTodoVO>> todo() {
        return Result.success(workbenchService.getTodoList());
    }

    @GetMapping("/done")
    public Result<List<ApprovalDoneVO>> done() {
        return Result.success(workbenchService.getDoneList());
    }
}
