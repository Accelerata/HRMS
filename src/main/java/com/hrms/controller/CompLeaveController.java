package com.hrms.controller;

import com.hrms.annotation.RequirePermission;
import com.hrms.result.Result;
import com.hrms.service.CompLeaveService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * 调休管理控制器
 *
 * 权限说明：
 * - att:record:manage - HR 手动触发调休折算（HR 专员/管理员）
 */
@RestController
@RequestMapping("/api/v1/comp-leave")
@RequiredArgsConstructor
public class CompLeaveController {

    private final CompLeaveService compLeaveService;

    /** HR 手动触发指定员工加班折算入账 */
    @PostMapping("/convert/{employeeId}")
    @RequirePermission("att:record:manage")
    public Result<BigDecimal> convert(@PathVariable Long employeeId) {
        BigDecimal days = compLeaveService.convertOvertime(employeeId);
        return Result.success(days);
    }
}
