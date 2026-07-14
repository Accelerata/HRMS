package com.hrms.controller;

import com.hrms.annotation.RequirePermission;
import com.hrms.dto.EmployeeSaveDTO;
import com.hrms.result.PageResult;
import com.hrms.result.Result;
import com.hrms.service.EmployeeService;
import com.hrms.vo.EmployeeListVO;
import com.hrms.vo.EmployeeVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 员工档案管理控制器
 */
@RestController
@RequestMapping("/api/v1/employee")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;

    /** 分页列表 */
    @GetMapping("/list")
    @RequirePermission("emp:view")
    public Result<PageResult<EmployeeListVO>> list(
            @RequestParam(required = false) Long deptId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        List<EmployeeListVO> list = employeeService.list(deptId, status, keyword, page, size);
        int total = employeeService.count(deptId, status, keyword);
        return Result.success(PageResult.of(list, total, page, size));
    }

    /** 查看详情 */
    @GetMapping("/{id}")
    @RequirePermission("emp:view")
    public Result<EmployeeVO> getById(@PathVariable Long id) {
        return Result.success(employeeService.getById(id));
    }

    /** 创建员工档案 */
    @PostMapping
    @RequirePermission("emp:create")
    public Result<Void> create(@Valid @RequestBody EmployeeSaveDTO dto) {
        return Result.success();
    }

    /** 更新员工档案 */
    @PutMapping("/{id}")
    @RequirePermission("emp:edit")
    public Result<Void> update(@PathVariable Long id,
                                     @Valid @RequestBody EmployeeSaveDTO dto) {
        dto.setId(id);
        return Result.success();
    }

    /** 删除员工档案 */
    @DeleteMapping("/{id}")
    @RequirePermission("emp:delete")
    public Result<Void> delete(@PathVariable Long id) {
        employeeService.delete(id);
        return Result.success();
    }
}
