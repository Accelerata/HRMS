package com.hrms.controller;

import com.hrms.annotation.RequirePermission;
import com.hrms.dto.ApprovalActionDTO;
import com.hrms.dto.TransferSaveDTO;
import com.hrms.result.Result;
import com.hrms.service.TransferService;
import com.hrms.vo.TransferVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/**
 * 调岗管理 Controller
 */
@RestController
@RequestMapping("/api/v1/transfers")
@RequiredArgsConstructor
@RequirePermission("transfer:manage")
public class TransferController {

    private final TransferService transferService;

    @GetMapping("/page")
    public Result<Object> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword) {
        return Result.success(transferService.page(status, keyword, page, size));
    }

    @GetMapping("/{id}")
    public Result<TransferVO> getDetail(@PathVariable Long id) {
        return Result.success(transferService.getDetail(id));
    }

    @PostMapping
    public Result<Object> submit(@Valid @RequestBody TransferSaveDTO dto) {
        transferService.submit(dto);
        return Result.success();
    }

    @PutMapping("/{id}/approve")
    public Result<Object> approve(@PathVariable Long id,
                                   @Valid @RequestBody ApprovalActionDTO dto) {
        transferService.approve(id, dto);
        return Result.success();
    }
}
