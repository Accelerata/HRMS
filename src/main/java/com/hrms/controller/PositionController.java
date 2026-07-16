package com.hrms.controller;

import com.hrms.annotation.RequirePermission;
import com.hrms.dto.PositionSaveDTO;
import com.hrms.entity.Position;
import com.hrms.result.Result;
import com.hrms.service.PositionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 职位管理控制器
 *
 * 权限说明（与 RBAC 种子对齐）：
 * - org:position:view  — 查看职位列表（HR/主管及以上）
 * - org:position:manage — 职位管理（HR及以上）
 */
@RestController
@RequestMapping("/api/v1/position")
@RequiredArgsConstructor
public class PositionController {

    private final PositionService positionService;

    /** 查询所有职位（可按序列筛选 M/P/S） */
    @GetMapping("/list")
    @RequirePermission("org:position:view")
    public Result<List<Position>> list(@RequestParam(required = false) String sequence) {
        if (sequence != null && !sequence.isBlank()) {
            return Result.success(positionService.listBySequence(sequence.toUpperCase()));
        }
        return Result.success(positionService.listAll());
    }

    /** 根据ID查询职位 */
    @GetMapping("/{id}")
    @RequirePermission("org:position:view")
    public Result<Position> getById(@PathVariable Long id) {
        return Result.success(positionService.getById(id));
    }

    /** 创建职位 */
    @PostMapping
    @RequirePermission("org:position:manage")
    public Result<Void> create(@Valid @RequestBody PositionSaveDTO dto) {
        positionService.create(dto);
        return Result.success();
    }

    /** 更新职位 */
    @PutMapping("/{id}")
    @RequirePermission("org:position:manage")
    public Result<Void> update(@PathVariable Long id,
                               @Valid @RequestBody PositionSaveDTO dto) {
        dto.setId(id);
        positionService.update(dto);
        return Result.success();
    }

    /** 删除职位 */
    @DeleteMapping("/{id}")
    @RequirePermission("org:position:manage")
    public Result<Void> delete(@PathVariable Long id) {
        positionService.delete(id);
        return Result.success();
    }
}
