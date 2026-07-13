package com.hrms.controller;

import com.hrms.annotation.RequirePermission;
import com.hrms.dto.ApprovalActionDTO;
import com.hrms.dto.ResignationSaveDTO;
import com.hrms.result.Result;
import com.hrms.service.ResignationService;
import com.hrms.vo.ResignationVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/**
 * 离职管理 Controller
 */
@RestController
@RequestMapping("/api/v1/resignations")
@RequiredArgsConstructor
@RequirePermission("resignation:manage")
public class ResignationController {

    private final ResignationService resignationService;

    @GetMapping("/page")
    public Result<Object> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword) {
        return Result.success(resignationService.page(status, keyword, page, size));
    }

    @GetMapping("/{id}")
    public Result<ResignationVO> getDetail(@PathVariable Long id) {
        return Result.success(resignationService.getDetail(id));
    }

    @PostMapping
    public Result<Object> submit(@Valid @RequestBody ResignationSaveDTO dto) {
        resignationService.submit(dto);
        return Result.success();
    }

    @PutMapping("/{id}/approve")
    public Result<Object> approve(@PathVariable Long id,
                                   @Valid @RequestBody ApprovalActionDTO dto) {
        resignationService.approve(id, dto);
        return Result.success();
    }
}
