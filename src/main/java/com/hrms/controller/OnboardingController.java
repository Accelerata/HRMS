package com.hrms.controller;

import com.hrms.annotation.RequirePermission;
import com.hrms.dto.ApprovalActionDTO;
import com.hrms.dto.OnboardingSaveDTO;
import com.hrms.result.PageResult;
import com.hrms.result.Result;
import com.hrms.service.OnboardingService;
import com.hrms.vo.OnboardingResultVO;
import com.hrms.vo.OnboardingVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/**
 * 入职管理 Controller
 *
 * 权限说明：
 * - 提交/编辑入职申请：ROLE_ADMIN, ROLE_HR（需 onboarding:manage）
 * - 审批入职申请：ROLE_ADMIN, ROLE_HR, 目标部门主管（Service 层校验审批人身份）
 */
@RestController
@RequestMapping("/api/v1/onboarding")
@RequiredArgsConstructor
@RequirePermission("onboarding:manage")
public class OnboardingController {

    private final OnboardingService onboardingService;

    @GetMapping("/page")
    public Result<PageResult> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword) {
        return Result.success(onboardingService.page(status, keyword, page, size));
    }

    @GetMapping("/{id}")
    public Result<OnboardingVO> getDetail(@PathVariable Long id) {
        return Result.success(onboardingService.getDetail(id));
    }

    @PostMapping
    public Result<Object> submit(@Valid @RequestBody OnboardingSaveDTO dto) {
        onboardingService.submit(dto);
        return Result.success();
    }

    @PostMapping("/draft")
    public Result<Object> saveDraft(@Valid @RequestBody OnboardingSaveDTO dto) {
        onboardingService.saveDraft(dto);
        return Result.success();
    }

    @PutMapping("/{id}")
    public Result<Object> update(@PathVariable Long id, @Valid @RequestBody OnboardingSaveDTO dto) {
        dto.setId(id);
        onboardingService.update(dto);
        return Result.success();
    }

    @PutMapping("/{id}/approve")
    public Result<OnboardingResultVO> approve(@PathVariable Long id,
                                               @Valid @RequestBody ApprovalActionDTO dto) {
        OnboardingResultVO result = onboardingService.approve(id, dto);
        if (result != null) {
            return Result.success(result);
        }
        return Result.success();
    }
}
