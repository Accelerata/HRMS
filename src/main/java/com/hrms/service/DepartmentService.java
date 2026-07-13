package com.hrms.service;

import com.hrms.common.exception.BaseException;
import com.hrms.dto.DepartmentSaveDTO;
import com.hrms.dto.DeptEmployeeCountDTO;
import com.hrms.entity.Department;
import com.hrms.mapper.DepartmentMapper;
import com.hrms.mapper.EmployeeMapper;
import com.hrms.vo.DeptTreeVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 部门管理服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DepartmentService {

    private static final int MAX_DEPT_LEVEL = 5;

    private final DepartmentMapper departmentMapper;
    private final EmployeeMapper employeeMapper;

    // ═══════════════ 查询 ═══════════════

    /**
     * 获取部门树（含实时人数统计）
     */
    public List<DeptTreeVO> getDeptTree() {
        List<Department> allDepts = departmentMapper.selectAll();
        if (allDepts.isEmpty()) {
            return Collections.emptyList();
        }

        // 获取各部门人数统计
        List<DeptEmployeeCountDTO> counts = employeeMapper.countByDeptGroupByStatus();
        Map<Long, Map<Integer, Integer>> countMap = new HashMap<>();
        for (DeptEmployeeCountDTO row : counts) {
            countMap.computeIfAbsent(row.getDeptId(), k -> new HashMap<>())
                     .put(row.getStatus(), row.getCount());
        }

        // 转为 VO 并构建树
        Map<Long, DeptTreeVO> voMap = allDepts.stream()
                .map(d -> toVO(d, countMap.getOrDefault(d.getId(), Collections.emptyMap())))
                .collect(Collectors.toMap(DeptTreeVO::getId, v -> v));

        List<DeptTreeVO> tree = new ArrayList<>();
        for (DeptTreeVO vo : voMap.values()) {
            if (vo.getParentId() == 0) {
                tree.add(vo);
            } else {
                DeptTreeVO parent = voMap.get(vo.getParentId());
                if (parent != null) {
                    if (parent.getChildren() == null) {
                        parent.setChildren(new ArrayList<>());
                    }
                    parent.getChildren().add(vo);
                }
            }
        }

        // 排序
        tree.sort(Comparator.comparing(DeptTreeVO::getSortOrder));
        for (DeptTreeVO vo : voMap.values()) {
            if (vo.getChildren() != null) {
                vo.getChildren().sort(Comparator.comparing(DeptTreeVO::getSortOrder));
            }
        }

        return tree;
    }

    /** 部门实体 → VO */
    private DeptTreeVO toVO(Department dept, Map<Integer, Integer> statusCounts) {
        DeptTreeVO vo = new DeptTreeVO();
        vo.setId(dept.getId());
        vo.setParentId(dept.getParentId());
        vo.setDeptName(dept.getDeptName());
        vo.setDeptCode(dept.getDeptCode());
        vo.setSortOrder(dept.getSortOrder());
        vo.setLevel(dept.getLevel());
        vo.setStatus(dept.getStatus());
        vo.setManagerId(dept.getManagerId());

        int probation = statusCounts.getOrDefault(1, 0); // status=1 试用期
        int regular = statusCounts.getOrDefault(2, 0);    // status=2 正式
        vo.setProbationCount(probation);
        vo.setRegularCount(regular);
        vo.setEmployeeCount(probation + regular);

        return vo;
    }

    // ═══════════════ 创建 ═══════════════

    @Transactional
    public void create(DepartmentSaveDTO dto) {
        // 1. 校验父部门存在（parentId=0 表示根部门）
        int level = 1;
        if (dto.getParentId() != 0) {
            Department parent = departmentMapper.selectById(dto.getParentId());
            if (parent == null) {
                throw BaseException.badRequest("父部门不存在");
            }
            if (parent.getLevel() >= MAX_DEPT_LEVEL) {
                throw BaseException.badRequest("部门层级不能超过" + MAX_DEPT_LEVEL + "级");
            }
            level = parent.getLevel() + 1;
        }

        // 2. 编码唯一性
        if (departmentMapper.countByCode(dto.getDeptCode(), null) > 0) {
            throw BaseException.badRequest("部门编码[" + dto.getDeptCode() + "]已存在");
        }

        // 3. 同层级名称唯一性
        if (departmentMapper.countByNameAndParent(dto.getDeptName(), dto.getParentId(), null) > 0) {
            throw BaseException.badRequest("同级下已存在同名部门[" + dto.getDeptName() + "]");
        }

        // 4. 插入
        Department dept = new Department();
        dept.setParentId(dto.getParentId());
        dept.setDeptName(dto.getDeptName());
        dept.setDeptCode(dto.getDeptCode());
        dept.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0);
        dept.setLevel(level);
        dept.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        dept.setManagerId(dto.getManagerId());

        departmentMapper.insert(dept);
        log.info("部门创建成功: id={}, name={}, level={}", dept.getId(), dept.getDeptName(), level);
    }

    // ═══════════════ 更新 ═══════════════

    @Transactional
    public void update(DepartmentSaveDTO dto) {
        if (dto.getId() == null) {
            throw BaseException.badRequest("更新时部门ID不能为空");
        }

        Department existing = departmentMapper.selectById(dto.getId());
        if (existing == null) {
            throw BaseException.notFound("部门不存在");
        }

        // 不能将自己设为父部门
        if (dto.getParentId().equals(dto.getId())) {
            throw BaseException.badRequest("上级部门不能选择自身");
        }

        // 计算层级
        int level = 1;
        if (dto.getParentId() != 0) {
            Department parent = departmentMapper.selectById(dto.getParentId());
            if (parent == null) {
                throw BaseException.badRequest("父部门不存在");
            }
            if (parent.getLevel() >= MAX_DEPT_LEVEL) {
                throw BaseException.badRequest("部门层级不能超过" + MAX_DEPT_LEVEL + "级");
            }
            level = parent.getLevel() + 1;
        }

        // 编码唯一性
        if (departmentMapper.countByCode(dto.getDeptCode(), dto.getId()) > 0) {
            throw BaseException.badRequest("部门编码[" + dto.getDeptCode() + "]已存在");
        }

        // 同层级名称唯一性
        if (departmentMapper.countByNameAndParent(dto.getDeptName(), dto.getParentId(), dto.getId()) > 0) {
            throw BaseException.badRequest("同级下已存在同名部门[" + dto.getDeptName() + "]");
        }

        Department dept = new Department();
        dept.setId(dto.getId());
        dept.setParentId(dto.getParentId());
        dept.setDeptName(dto.getDeptName());
        dept.setDeptCode(dto.getDeptCode());
        dept.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0);
        dept.setLevel(level);
        dept.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        dept.setManagerId(dto.getManagerId());

        departmentMapper.update(dept);
        log.info("部门更新成功: id={}, name={}", dept.getId(), dept.getDeptName());
    }

    // ═══════════════ 删除 ═══════════════

    @Transactional
    public void delete(Long id) {
        Department dept = departmentMapper.selectById(id);
        if (dept == null) {
            throw BaseException.notFound("部门不存在");
        }

        // 检查是否有子部门
        if (departmentMapper.countByParentId(id) > 0) {
            throw BaseException.badRequest("该部门下存在子部门，无法删除");
        }

        // 检查是否有在职员工
        List<com.hrms.entity.Employee> employees = employeeMapper.selectByDeptId(id);
        if (employees != null && !employees.isEmpty()) {
            throw BaseException.badRequest("该部门下存在在职员工，无法删除");
        }

        departmentMapper.deleteById(id);
        log.info("部门删除成功: id={}, name={}", id, dept.getDeptName());
    }
}
