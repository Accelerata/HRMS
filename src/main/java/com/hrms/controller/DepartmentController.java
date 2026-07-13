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
 */
@RestController
@RequestMapping("/api/v1/dept")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService departmentService;

    /** 获取部门树（含实时人数统计） */
    @GetMapping("/tree")
    @RequirePermission("dept:view")
    public Result<List<DeptTreeVO>> tree() {
        return Result.success(departmentService.getDeptTree());
    }

    /** 创建部门 */
    @PostMapping
    @RequirePermission("dept:manage")
    public Result<Void> create(@Valid @RequestBody DepartmentSaveDTO dto) {
        departmentService.create(dto);
        return Result.success();
    }

    /** 更新部门 */
    @PutMapping("/{id}")
    @RequirePermission("dept:manage")
    public Result<Void> update(@PathVariable Long id,
                               @Valid @RequestBody DepartmentSaveDTO dto) {
        dto.setId(id);
        departmentService.update(dto);
        return Result.success();
    }

    /** 删除部门 */
    @DeleteMapping("/{id}")
    @RequirePermission("dept:manage")
    public Result<Void> delete(@PathVariable Long id) {
        departmentService.delete(id);
        return Result.success();
    }
}
