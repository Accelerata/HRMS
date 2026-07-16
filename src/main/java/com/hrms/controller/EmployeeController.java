package com.hrms.controller;

import com.hrms.annotation.RequirePermission;
import com.hrms.dto.EmployeeQueryDTO;
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

    /**
     * 分页列表（支持高级搜索多条件 AND 组合）
     * 兼容旧参数：deptId, status, keyword
     * 新增参数：phone, deptIds, positionIds, statuses, grades, startDate, endDate
     */
    @GetMapping("/list")
    @RequirePermission("emp:view")
    public Result<PageResult<EmployeeListVO>> list(
            @RequestParam(required = false) Long deptId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) List<Long> deptIds,
            @RequestParam(required = false) List<Long> positionIds,
            @RequestParam(required = false) List<Integer> statuses,
            @RequestParam(required = false) List<String> grades,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate startDate,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate endDate,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {

        // 构建查询 DTO
        EmployeeQueryDTO dto = new EmployeeQueryDTO();
        dto.setKeyword(keyword);
        dto.setPhone(phone);
        dto.setDeptIds(deptIds);
        dto.setPositionIds(positionIds);
        dto.setStatuses(statuses);
        dto.setGrades(grades);
        dto.setStartDate(startDate);
        dto.setEndDate(endDate);

        // 兼容旧参数：如果传了 deptId 且 deptIds 为空，则使用 deptId
        if (deptId != null && (deptIds == null || deptIds.isEmpty())) {
            dto.setDeptIds(List.of(deptId));
        }
        // 兼容旧参数：如果传了 status 且 statuses 为空，则使用 status
        if (status != null && (statuses == null || statuses.isEmpty())) {
            dto.setStatuses(List.of(status));
        }

        // 判断是否使用高级搜索：有任一高级搜索参数则走新逻辑
        boolean useAdvanced = phone != null
                || (deptIds != null && !deptIds.isEmpty())
                || (positionIds != null && !positionIds.isEmpty())
                || (statuses != null && !statuses.isEmpty())
                || (grades != null && !grades.isEmpty())
                || startDate != null
                || endDate != null;

        List<EmployeeListVO> list;
        int total;
        if (useAdvanced) {
            list = employeeService.queryEmployees(dto, page, size);
            total = employeeService.countByConditions(dto);
        } else {
            list = employeeService.list(deptId, status, keyword, page, size);
            total = employeeService.count(deptId, status, keyword);
        }
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
    public Result<EmployeeVO> create(@Valid @RequestBody EmployeeSaveDTO dto) {
        return Result.success(employeeService.create(dto));
    }

    /** 更新员工档案 */
    @PutMapping("/{id}")
    @RequirePermission("emp:edit")
    public Result<EmployeeVO> update(@PathVariable Long id,
                                     @Valid @RequestBody EmployeeSaveDTO dto) {
        dto.setId(id);
        return Result.success(employeeService.update(dto));
    }

    /** 删除员工档案 */
    @DeleteMapping("/{id}")
    @RequirePermission("emp:delete")
    public Result<Void> delete(@PathVariable Long id) {
        employeeService.delete(id);
        return Result.success();
    }
}
