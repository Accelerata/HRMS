package com.hrms.controller;

import com.hrms.annotation.RequirePermission;
import com.hrms.dto.ApprovalActionDTO;
import com.hrms.dto.RegularizationSaveDTO;
import com.hrms.result.PageResult;
import com.hrms.result.Result;
import com.hrms.service.RegularizationService;
import com.hrms.vo.ExpiringEmployeeVO;
import com.hrms.vo.RegularizationVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * 转正管理 Controller
 */
@RestController
@RequestMapping("/api/v1/regularization")
@RequiredArgsConstructor
@RequirePermission("regularization:manage")
public class RegularizationController {

    private final RegularizationService regularizationService;

    @GetMapping("/page")
    public Result<PageResult> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword) {
        return Result.success(regularizationService.page(status, keyword, page, size));
    }

    @GetMapping("/{id}")
    public Result<RegularizationVO> getDetail(@PathVariable Long id) {
        return Result.success(regularizationService.getDetail(id));
    }

    @PostMapping
    public Result<Object> submit(@Valid @RequestBody RegularizationSaveDTO dto) {
        regularizationService.submit(dto);
        return Result.success();
    }

    @PutMapping("/{id}/approve")
    public Result<Object> approve(@PathVariable Long id,
                                   @Valid @RequestBody ApprovalActionDTO dto) {
        regularizationService.approve(id, dto);
        return Result.success();
    }

    @GetMapping("/expiring")
    public Result<List<ExpiringEmployeeVO>> expiring(
            @RequestParam(defaultValue = "7") int days) {
        return Result.success(regularizationService.getExpiringEmployees(days));
    }
}
