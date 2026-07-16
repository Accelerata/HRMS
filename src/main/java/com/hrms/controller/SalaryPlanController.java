package com.hrms.controller;

import com.hrms.annotation.RequirePermission;
import com.hrms.entity.SalaryPlan;
import com.hrms.entity.SalaryPlanItem;
import com.hrms.entity.SalaryPlanScope;
import com.hrms.result.Result;
import com.hrms.service.SalaryPlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/salary/plans")
@RequiredArgsConstructor
public class SalaryPlanController {

    private final SalaryPlanService salaryPlanService;

    @GetMapping
    @RequirePermission("salary:plan:view")
    public Result<List<SalaryPlan>> list() {
        return Result.success(salaryPlanService.listAll());
    }

    @GetMapping("/{id}")
    @RequirePermission("salary:plan:view")
    public Result<SalaryPlan> detail(@PathVariable Long id) {
        return Result.success(salaryPlanService.getById(id));
    }

    @PostMapping
    @RequirePermission("salary:plan:manage")
    public Result<SalaryPlan> create(@RequestBody SalaryPlan plan) {
        return Result.success(salaryPlanService.create(plan));
    }

    @PutMapping("/{id}")
    @RequirePermission("salary:plan:manage")
    public Result<Void> update(@PathVariable Long id, @RequestBody SalaryPlan plan) {
        plan.setId(id);
        salaryPlanService.update(plan);
        return Result.success();
    }

    @PutMapping("/{id}/status")
    @RequirePermission("salary:plan:manage")
    public Result<Void> toggleStatus(@PathVariable Long id, @RequestParam Integer status) {
        salaryPlanService.updateStatus(id, status);
        return Result.success();
    }

    // ── 工资项目 ──

    @GetMapping("/{planId}/items")
    @RequirePermission("salary:plan:view")
    public Result<List<SalaryPlanItem>> items(@PathVariable Long planId) {
        return Result.success(salaryPlanService.listItems(planId));
    }

    @PostMapping("/{planId}/items")
    @RequirePermission("salary:plan:manage")
    public Result<Void> addItem(@PathVariable Long planId, @RequestBody SalaryPlanItem item) {
        item.setPlanId(planId);
        salaryPlanService.addItem(item);
        return Result.success();
    }

    @DeleteMapping("/{planId}/items/{id}")
    @RequirePermission("salary:plan:manage")
    public Result<Void> deleteItem(@PathVariable Long planId, @PathVariable Long id) {
        salaryPlanService.deleteItem(id);
        return Result.success();
    }

    // ── 适用范围 ──

    @GetMapping("/{planId}/scopes")
    @RequirePermission("salary:plan:view")
    public Result<List<SalaryPlanScope>> scopes(@PathVariable Long planId) {
        return Result.success(salaryPlanService.listScopes(planId));
    }

    @PostMapping("/{planId}/scopes")
    @RequirePermission("salary:plan:manage")
    public Result<Void> addScope(@PathVariable Long planId, @RequestBody SalaryPlanScope scope) {
        scope.setPlanId(planId);
        salaryPlanService.addScope(scope);
        return Result.success();
    }
}
