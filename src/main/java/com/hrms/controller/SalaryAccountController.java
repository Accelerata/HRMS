package com.hrms.controller;

import com.hrms.annotation.RequirePermission;
import com.hrms.entity.SalaryAccount;
import com.hrms.entity.SalaryChangeHistory;
import com.hrms.result.Result;
import com.hrms.service.SalaryAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/salary/accounts")
@RequiredArgsConstructor
public class SalaryAccountController {

    private final SalaryAccountService salaryAccountService;

    @GetMapping("/{employeeId}")
    @RequirePermission("salary:account:view")
    public Result<SalaryAccount> get(@PathVariable Long employeeId) {
        return Result.success(salaryAccountService.getByEmployeeId(employeeId));
    }

    @GetMapping("/{employeeId}/history")
    @RequirePermission("salary:account:view")
    public Result<List<SalaryChangeHistory>> history(@PathVariable Long employeeId) {
        return Result.success(salaryAccountService.listHistory(employeeId));
    }

    @PostMapping
    @RequirePermission("salary:account:manage")
    public Result<SalaryAccount> create(@RequestBody SalaryAccount account) {
        return Result.success(salaryAccountService.create(account));
    }

    @PutMapping("/{id}/adjust")
    @RequirePermission("salary:account:manage")
    public Result<Void> adjust(@PathVariable Long id, @RequestBody SalaryAccount account) {
        salaryAccountService.adjust(id, account);
        return Result.success();
    }

    @PutMapping("/{id}/deactivate")
    @RequirePermission("salary:account:manage")
    public Result<Void> deactivate(@PathVariable Long id) {
        salaryAccountService.deactivate(id);
        return Result.success();
    }
}
