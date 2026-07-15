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
import java.util.Map;

/**
 * 入职管理 Controller
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

    @DeleteMapping("/{id}")
    public Result<Object> deleteDraft(@PathVariable Long id) {
        onboardingService.deleteDraft(id);
        return Result.success();
    }

    @PostMapping("/{id}/withdraw")
    public Result<Object> withdraw(@PathVariable Long id) {
        onboardingService.withdraw(id);
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

    @PostMapping("/{id}/confirm-arrival")
    public Result<Object> confirmArrival(@PathVariable Long id) {
        onboardingService.confirmArrival(id);
        return Result.success();
    }

    @PutMapping("/{id}/entry-date")
    public Result<Object> updateEntryDate(@PathVariable Long id, @RequestBody Map<String, String> body) {
        onboardingService.updateEntryDate(id, body.get("entryDate"));
        return Result.success();
    }

    @PostMapping("/{id}/abandon")
    public Result<Object> markAbandon(@PathVariable Long id) {
        onboardingService.markAbandon(id);
        return Result.success();
    }
}
