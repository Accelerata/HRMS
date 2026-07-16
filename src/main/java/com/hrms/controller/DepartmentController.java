package com.hrms.controller;

import com.hrms.annotation.RequirePermission;
import com.hrms.dto.DepartmentSaveDTO;
import com.hrms.result.Result;
import com.hrms.service.DepartmentService;
import com.hrms.vo.DeptTreeVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 部门管理控制器
 *
 * 权限说明（与 RBAC 种子对齐）：
 * - org:dept:view  — 查看部门树（HR/主管及以上）
 * - org:dept:manage — 部门管理（HR及以上）
 */
@RestController
@RequestMapping("/api/v1/dept")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService departmentService;

    /** 获取部门树（含实时人数统计） */
    @GetMapping("/tree")
    @RequirePermission("org:dept:view")
    public Result<List<DeptTreeVO>> tree() {
        return Result.success(departmentService.getDeptTree());
    }

    /** 创建部门 */
    @PostMapping
    @RequirePermission("org:dept:manage")
    public Result<Void> create(@Valid @RequestBody DepartmentSaveDTO dto) {
        departmentService.create(dto);
        return Result.success();
    }

    /** 更新部门 */
    @PutMapping("/{id}")
    @RequirePermission("org:dept:manage")
    public Result<Void> update(@PathVariable Long id,
                               @Valid @RequestBody DepartmentSaveDTO dto) {
        dto.setId(id);
        departmentService.update(dto);
        return Result.success();
    }

    /** 删除部门 */
    @DeleteMapping("/{id}")
    @RequirePermission("org:dept:manage")
    public Result<Void> delete(@PathVariable Long id) {
        departmentService.delete(id);
        return Result.success();
    }

    /** 部门合并：将源部门所有员工转移至目标部门，源部门标记为已合并 */
    @PostMapping("/{id}/merge")
    @RequirePermission("org:dept:manage")
    public Result<Void> merge(@PathVariable Long id,
                              @RequestParam Long targetDeptId) {
        departmentService.mergeDepartments(id, targetDeptId);
        return Result.success();
    }
}
