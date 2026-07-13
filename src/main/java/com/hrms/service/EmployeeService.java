package com.hrms.service;

import com.hrms.common.exception.BaseException;
import com.hrms.dto.EmployeeSaveDTO;
import com.hrms.entity.Department;
import com.hrms.entity.Employee;
import com.hrms.mapper.DepartmentMapper;
import com.hrms.mapper.EmployeeMapper;
import com.hrms.vo.EmployeeVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 员工档案管理服务
 * 负责员工 CRUD、工号自动生成、唯一性校验
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeMapper employeeMapper;
    private final DepartmentMapper departmentMapper;

    /** 工号序号缓存（简化实现，生产环境应用 Redis 原子自增） */
    private static final DateTimeFormatter YEAR_FMT = DateTimeFormatter.ofPattern("yyyy");

    // ═══════════════ 查询 ═══════════════

    /** 分页列表 */
    public List<EmployeeVO> list(Long deptId, Integer status, String keyword, int page, int size) {
        int offset = (page - 1) * size;
        List<Employee> employees = employeeMapper.selectByCondition(deptId, status, keyword, offset, size);
        return employees.stream().map(this::toVO).collect(Collectors.toList());
    }

    /** 分页总数 */
    public int count(Long deptId, Integer status, String keyword) {
        return employeeMapper.countByCondition(deptId, status, keyword);
    }

    /** 根据ID查询 */
    public EmployeeVO getById(Long id) {
        Employee emp = employeeMapper.selectById(id);
        if (emp == null) {
            throw BaseException.notFound("员工不存在");
        }
        return toVO(emp);
    }

    // ═══════════════ 创建 ═══════════════

    @Transactional
    public EmployeeVO create(EmployeeSaveDTO dto) {
        // 1. 校验手机号唯一性
        if (employeeMapper.selectByPhone(dto.getPhone()) != null) {
            throw BaseException.badRequest("手机号[" + dto.getPhone() + "]已被其他员工使用");
        }

        // 2. 校验身份证号唯一性
        if (employeeMapper.selectByIdCard(dto.getIdCard()) != null) {
            throw BaseException.badRequest("身份证号[" + dto.getIdCard() + "]已存在");
        }

        // 3. 校验部门存在
        Department dept = departmentMapper.selectById(dto.getDeptId());
        if (dept == null) {
            throw BaseException.badRequest("所属部门不存在");
        }

        // 4. 组装实体
        Employee emp = new Employee();
        emp.setEmployeeNo(generateEmployeeNo(dto.getDeptId(), dept.getDeptCode()));
        emp.setName(dto.getName());
        emp.setGender(dto.getGender());
        emp.setPhone(dto.getPhone());
        emp.setEmail(dto.getEmail());
        emp.setIdCard(dto.getIdCard());
        // 生日：不填则从身份证号自动提取
        emp.setBirthday(dto.getBirthday() != null ? dto.getBirthday() : extractBirthdayFromIdCard(dto.getIdCard()));
        emp.setRegisteredAddress(dto.getRegisteredAddress());
        emp.setCurrentAddress(dto.getCurrentAddress());
        emp.setDeptId(dto.getDeptId());
        emp.setPositionId(dto.getPositionId());
        emp.setGrade(dto.getGrade());
        emp.setReportTo(dto.getReportTo());
        emp.setWorkLocation(dto.getWorkLocation());
        emp.setEntryType(dto.getEntryType());
        emp.setSalaryAccountId(dto.getSalaryAccountId());
        emp.setBaseSalary(dto.getBaseSalary());
        emp.setBankAccount(dto.getBankAccount());
        emp.setBankName(dto.getBankName());
        emp.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);  // 默认试用期
        emp.setEntryDate(dto.getEntryDate());

        employeeMapper.insert(emp);
        log.info("员工创建成功: id={}, employeeNo={}, name={}", emp.getId(), emp.getEmployeeNo(), emp.getName());
        return toVO(emp);
    }

    // ═══════════════ 更新 ═══════════════

    @Transactional
    public EmployeeVO update(EmployeeSaveDTO dto) {
        if (dto.getId() == null) {
            throw BaseException.badRequest("更新时员工ID不能为空");
        }

        Employee existing = employeeMapper.selectById(dto.getId());
        if (existing == null) {
            throw BaseException.notFound("员工不存在");
        }

        // 手机号唯一性（排除自己）
        Employee phoneOwner = employeeMapper.selectByPhone(dto.getPhone());
        if (phoneOwner != null && !phoneOwner.getId().equals(dto.getId())) {
            throw BaseException.badRequest("手机号[" + dto.getPhone() + "]已被其他员工使用");
        }

        // 身份证号唯一性（排除自己）
        Employee idCardOwner = employeeMapper.selectByIdCard(dto.getIdCard());
        if (idCardOwner != null && !idCardOwner.getId().equals(dto.getId())) {
            throw BaseException.badRequest("身份证号[" + dto.getIdCard() + "]已存在");
        }

        // 组装更新
        Employee emp = new Employee();
        emp.setId(dto.getId());
        emp.setName(dto.getName());
        emp.setGender(dto.getGender());
        emp.setPhone(dto.getPhone());
        emp.setEmail(dto.getEmail());
        emp.setIdCard(dto.getIdCard());
        emp.setBirthday(dto.getBirthday() != null ? dto.getBirthday() : extractBirthdayFromIdCard(dto.getIdCard()));
        emp.setRegisteredAddress(dto.getRegisteredAddress());
        emp.setCurrentAddress(dto.getCurrentAddress());
        emp.setDeptId(dto.getDeptId());
        emp.setPositionId(dto.getPositionId());
        emp.setGrade(dto.getGrade());
        emp.setReportTo(dto.getReportTo());
        emp.setWorkLocation(dto.getWorkLocation());
        emp.setEntryType(dto.getEntryType());
        emp.setSalaryAccountId(dto.getSalaryAccountId());
        emp.setBaseSalary(dto.getBaseSalary());
        emp.setBankAccount(dto.getBankAccount());
        emp.setBankName(dto.getBankName());
        emp.setStatus(dto.getStatus());
        emp.setEntryDate(dto.getEntryDate());

        employeeMapper.update(emp);
        log.info("员工更新成功: id={}, name={}", emp.getId(), emp.getName());
        return getById(dto.getId());
    }

    // ═══════════════ 删除 ═══════════════

    @Transactional
    public void delete(Long id) {
        Employee emp = employeeMapper.selectById(id);
        if (emp == null) {
            throw BaseException.notFound("员工不存在");
        }
        if (emp.getStatus() != null && (emp.getStatus() == 1 || emp.getStatus() == 2 || emp.getStatus() == 3)) {
            throw BaseException.badRequest("该员工仍为在职状态，请先完成离职流程后再删除");
        }
        employeeMapper.deleteById(id);
        log.info("员工删除成功: id={}, name={}", id, emp.getName());
    }

    // ═══════════════ 工具方法 ═══════════════

    /**
     * 自动生成工号
     * 格式：年份(4位) + 部门编码(2位) + 序号(3位)
     * 示例: 202601001 → 2026年 + 01部门 + 第001个员工
     */
    private String generateEmployeeNo(Long deptId, String deptCode) {
        String yearPart = LocalDate.now().format(YEAR_FMT);  // 2026

        // 取部门编码后2位做标识，不足2位补0
        String deptPart;
        if (deptCode != null && deptCode.length() >= 2) {
            deptPart = deptCode.substring(deptCode.length() - 2);
        } else {
            deptPart = String.format("%02d", deptId % 100);
        }

        // 序号：当前该部门员工数 + 1
        List<Employee> deptEmployees = employeeMapper.selectByDeptId(deptId);
        int seq = (deptEmployees != null ? deptEmployees.size() : 0) + 1;

        return yearPart + deptPart + String.format("%03d", seq);
    }

    /**
     * 从身份证号提取生日（18位身份证: 第7-14位为YYYYMMDD）
     */
    private LocalDate extractBirthdayFromIdCard(String idCard) {
        if (idCard == null || idCard.length() < 14) {
            return null;
        }
        try {
            String dateStr = idCard.substring(6, 14);
            return LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyyMMdd"));
        } catch (Exception e) {
            log.warn("无法从身份证号提取生日: idCard={}", idCard);
            return null;
        }
    }

    // ═══════════════ VO 转换 ═══════════════

    /** 将 Employee 实体转为 EmployeeVO（含关联查询） */
    private EmployeeVO toVO(Employee emp) {
        if (emp == null) return null;

        EmployeeVO vo = new EmployeeVO();
        vo.setId(emp.getId());
        vo.setEmployeeNo(emp.getEmployeeNo());
        vo.setName(emp.getName());

        // 个人信息
        vo.setGender(emp.getGender());
        vo.setPhone(emp.getPhone());
        vo.setEmail(emp.getEmail());
        vo.setIdCard(emp.getIdCard());
        vo.setBirthday(emp.getBirthday());
        vo.setRegisteredAddress(emp.getRegisteredAddress());
        vo.setCurrentAddress(emp.getCurrentAddress());

        // 部门
        vo.setDeptId(emp.getDeptId());
        if (emp.getDeptId() != null) {
            Department dept = departmentMapper.selectById(emp.getDeptId());
            vo.setDeptName(dept != null ? dept.getDeptName() : null);
        }

        // 职位
        vo.setPositionId(emp.getPositionId());
        // 职位名称由调用方按需填充（避免循环依赖 PositionService）

        // 职级
        vo.setGrade(emp.getGrade());

        // 汇报人
        vo.setReportTo(emp.getReportTo());
        if (emp.getReportTo() != null) {
            Employee reportTo = employeeMapper.selectById(emp.getReportTo());
            vo.setReportToName(reportTo != null ? reportTo.getName() : null);
        }

        // 工作信息
        vo.setWorkLocation(emp.getWorkLocation());
        vo.setEntryType(emp.getEntryType());
        vo.setEntryTypeLabel(entryTypeLabel(emp.getEntryType()));

        // 薪资信息
        vo.setSalaryAccountId(emp.getSalaryAccountId());
        vo.setBaseSalary(emp.getBaseSalary());
        vo.setBankAccount(emp.getBankAccount());
        vo.setBankName(emp.getBankName());

        // 状态与时间
        vo.setStatus(emp.getStatus());
        vo.setStatusLabel(statusLabel(emp.getStatus()));
        vo.setEntryDate(emp.getEntryDate());
        vo.setRegularDate(emp.getRegularDate());
        vo.setResignDate(emp.getResignDate());
        vo.setUserId(emp.getUserId());
        vo.setCreateTime(emp.getCreateTime());
        vo.setUpdateTime(emp.getUpdateTime());

        return vo;
    }

    private String statusLabel(Integer status) {
        if (status == null) return "未知";
        return switch (status) {
            case 0 -> "待入职";
            case 1 -> "试用期";
            case 2 -> "正式";
            case 3 -> "待离职";
            case 4 -> "已离职";
            default -> "未知";
        };
    }

    private String entryTypeLabel(Integer entryType) {
        if (entryType == null) return null;
        return switch (entryType) {
            case 1 -> "社会招聘";
            case 2 -> "校园招聘";
            case 3 -> "内部推荐";
            case 4 -> "内部调动";
            default -> "未知";
        };
    }
}
