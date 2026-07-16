package com.hrms.controller;

import com.hrms.annotation.RequirePermission;
import com.hrms.dto.ApprovalActionDTO;
import com.hrms.dto.SupplementaryCardApplyDTO;
import com.hrms.entity.SupplementaryCardApplication;
import com.hrms.result.Result;
import com.hrms.service.SupplementaryCardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 补卡审批控制器
 *
 * 权限说明：
 * - att:card:apply   - 发起补卡（普通员工及以上）
 * - att:card:approve - 审批补卡（直接上级/部门主管/HR）
 */
@RestController
@RequestMapping("/api/v1/supplementary-card")
@RequiredArgsConstructor
public class SupplementaryCardController {

    private final SupplementaryCardService cardService;

    /** 发起补卡申请 */
    @PostMapping("/apply")
    @RequirePermission("att:card:apply")
    public Result<SupplementaryCardApplication> apply(@Valid @RequestBody SupplementaryCardApplyDTO dto) {
        return Result.success(cardService.apply(dto));
    }

    /** 审批补卡申请 */
    @PostMapping("/{id}/approve")
    @RequirePermission("att:card:approve")
    public Result<Void> approve(@PathVariable Long id, @RequestBody ApprovalActionDTO dto) {
        cardService.approve(id, dto);
        return Result.success();
    }

    /** 查询我的补卡申请 */
    @GetMapping("/my")
    @RequirePermission("att:card:apply")
    public Result<List<SupplementaryCardApplication>> my() {
        return Result.success(cardService.myApplications());
    }
}
