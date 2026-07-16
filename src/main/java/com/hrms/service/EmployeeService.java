package com.hrms.service;

import com.hrms.common.context.BaseContext;
import com.hrms.common.exception.BaseException;
import com.hrms.crypto.EncryptionUtil;
import com.hrms.dto.EmployeeQueryDTO;
import com.hrms.dto.EmployeeSaveDTO;
import com.hrms.entity.Department;
import com.hrms.entity.Employee;
import com.hrms.enums.SensitiveFieldPolicy;
import com.hrms.mapper.DepartmentMapper;
import com.hrms.mapper.EmployeeMapper;
import com.hrms.vo.EmployeeListVO;
import com.hrms.vo.EmployeeVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 员工档案管理服务
 * 负责员工 CRUD、工号自动生成、唯一性校验
 * 敏感字段通过 EncryptionUtil 计算哈希后写入 hash 列，支持加密后精确查询
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeMapper employeeMapper;
    private final DepartmentMapper departmentMapper;
    private final EncryptionUtil encryptionUtil;

    /** 工号序号缓存（简化实现，生产环境应用 Redis 原子自增） */
    private static final DateTimeFormatter YEAR_FMT = DateTimeFormatter.ofPattern("yyyy");

    // ═══════════════ 查询 ═══════════════

    /** 分页列表（轻量 JOIN，仅返回列表所需字段） */
    public List<EmployeeListVO> list(Long deptId, Integer status, String keyword, int page, int size) {
        int offset = (page - 1) * size;
        List<EmployeeListVO> list = employeeMapper.selectForList(deptId, status, keyword, offset, size);
        // 填充 statusLabel
        for (EmployeeListVO vo : list) {
            vo.setStatusLabel(statusLabel(vo.getStatus()));
        }
        return list;
    }

    /** 分页总数 */
    public int count(Long deptId, Integer status, String keyword) {
        return employeeMapper.countForList(deptId, status, keyword);
    }

    /**
     * 高级搜索（多条件 AND 组合查询）
     * 支持：关键词、手机号哈希匹配、部门/职位/职级/状态多选、入职日期范围
     */
    public List<EmployeeListVO> queryEmployees(EmployeeQueryDTO dto, int page, int size) {
        int offset = (page - 1) * size;
        // 手机号搜索：在 Service 层计算哈希后传入 Mapper 精确匹配
        String phoneHash = null;
        if (dto.getPhone() != null && !dto.getPhone().isBlank()) {
            phoneHash = encryptionUtil.computeHash(dto.getPhone());
        }
        List<EmployeeListVO> list = employeeMapper.selectByConditions(dto, phoneHash, offset, size);
        // 填充 statusLabel
        for (EmployeeListVO vo : list) {
            vo.setStatusLabel(statusLabel(vo.getStatus()));
        }
        return list;
    }

    /** 高级搜索总数 */
    public int countByConditions(EmployeeQueryDTO dto) {
        String phoneHash = null;
        if (dto.getPhone() != null && !dto.getPhone().isBlank()) {
            phoneHash = encryptionUtil.computeHash(dto.getPhone());
        }
        return employeeMapper.countByConditions(dto, phoneHash);
    }

    /** 状态→文字 */
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

    /** 根据ID查询 */
    public EmployeeVO getById(Long id) {
        Employee emp = employeeMapper.selectById(id);
        if (emp == null) {
            throw BaseException.notFound("员工不存在");
        }
        return toVO(emp);
    }

    /** 公开的实体转 VO 方法（供其他 Service 调用，含字段过滤） */
    public EmployeeVO toEmployeeVO(Employee emp) {
        return toVO(emp);
    }

    // ═══════════════ 创建 ═══════════════

    @Transactional
    public EmployeeVO create(EmployeeSaveDTO dto) {
        // 1. 校验手机号唯一性（通过哈希索引查询）
        String phoneHash = encryptionUtil.computeHash(dto.getPhone());
        if (phoneHash != null && employeeMapper.selectByPhoneHash(phoneHash) != null) {
            throw BaseException.badRequest("手机号已被其他员工使用");
        }

        // 2. 校验身份证号唯一性（通过哈希索引查询）
        String idCardHash = encryptionUtil.computeHash(dto.getIdCard());
        if (idCardHash != null && employeeMapper.selectByIdCardHash(idCardHash) != null) {
            throw BaseException.badRequest("身份证号已存在");
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
        emp.setPhoneHash(phoneHash);
        emp.setEmail(dto.getEmail());
        emp.setIdCard(dto.getIdCard());
        emp.setIdCardHash(idCardHash);
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
        emp.setBankAccountHash(encryptionUtil.computeHash(dto.getBankAccount()));
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

        // 手机号唯一性（排除自己，通过哈希索引查询）
        String phoneHash = encryptionUtil.computeHash(dto.getPhone());
        if (phoneHash != null) {
            Employee phoneOwner = employeeMapper.selectByPhoneHash(phoneHash);
            if (phoneOwner != null && !phoneOwner.getId().equals(dto.getId())) {
                throw BaseException.badRequest("手机号已被其他员工使用");
            }
        }

        // 身份证号唯一性（排除自己，通过哈希索引查询）
        String idCardHash = encryptionUtil.computeHash(dto.getIdCard());
        if (idCardHash != null) {
            Employee idCardOwner = employeeMapper.selectByIdCardHash(idCardHash);
            if (idCardOwner != null && !idCardOwner.getId().equals(dto.getId())) {
                throw BaseException.badRequest("身份证号已存在");
            }
        }

        // 组装更新
        Employee emp = new Employee();
        emp.setId(dto.getId());
        emp.setName(dto.getName());
        emp.setGender(dto.getGender());
        emp.setPhone(dto.getPhone());
        emp.setPhoneHash(phoneHash);
        emp.setEmail(dto.getEmail());
        emp.setIdCard(dto.getIdCard());
        emp.setIdCardHash(idCardHash);
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
        emp.setBankAccountHash(encryptionUtil.computeHash(dto.getBankAccount()));
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

        // 序号：该「年份+部门」前缀下现有最大工号序号 + 1
        // （用 MAX 而非 COUNT，容忍删号/断号，避免与历史工号冲突；employee_no 非敏感字段无需解密）
        String prefix = yearPart + deptPart;
        String maxNo = employeeMapper.selectMaxEmployeeNo(prefix);
        int seq = 1;
        if (maxNo != null && maxNo.length() > prefix.length()) {
            try {
                seq = Integer.parseInt(maxNo.substring(prefix.length())) + 1;
            } catch (NumberFormatException e) {
                log.warn("工号最大序号解析失败，回退为1: maxNo={}", maxNo);
                seq = 1;
            }
        }

        return prefix + String.format("%03d", seq);
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

    /** 将 Employee 实体转为 EmployeeVO（含关联查询 + 字段级权限过滤） */
    private EmployeeVO toVO(Employee emp) {
        if (emp == null) return null;

        // 先构建完整 VO
        EmployeeVO vo = new EmployeeVO();
        vo.setId(emp.getId());
        vo.setEmployeeNo(emp.getEmployeeNo());
        vo.setName(emp.getName());

        // 个人信息（先填充全部，后续按策略过滤）
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

        // 薪资信息（先填充全部，后续按策略过滤）
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

        // 应用字段级权限过滤
        applyFieldFilter(vo, emp.getId());

        return vo;
    }

    /**
     * 根据当前用户数据权限对 EmployeeVO 进行字段级过滤。
     *
     * 策略规则（白名单模式）：
     * - SHOW_ALL（Admin/HR）→ 全部字段可见
     * - HIDE_SALARY_BANK（Manager）→ 隐藏身份证号、薪资信息、银行卡号
     * - SELF_ONLY（Employee）→ 仅本人可看敏感字段，他人仅基本信息
     * - SALARY_ONLY（Finance）→ 仅薪资信息可见，个人隐私如身份证/手机/银行卡隐藏
     */
    void applyFieldFilter(EmployeeVO vo, Long targetEmployeeId) {
        if (vo == null) return;

        Integer dataScope = BaseContext.getDataScope();
        // 无上下文的场景（如定时任务、测试）默认不过滤
        if (dataScope == null) return;

        SensitiveFieldPolicy policy = SensitiveFieldPolicy.fromDataScope(dataScope);
        Long currentEmployeeId = BaseContext.getCurrentEmployeeId();

        switch (policy) {
            case SHOW_ALL:
                // Admin / HR：全部字段可见，不做过滤
                return;

            case HIDE_SALARY_BANK:
                // 部门主管：隐藏身份证号、薪资信息、银行卡号
                vo.setIdCard(null);
                vo.setSalaryAccountId(null);
                vo.setBaseSalary(null);
                vo.setBankAccount(null);
                vo.setBankName(null);
                break;

            case SELF_ONLY:
                // 普通员工：仅本人可看完整信息，他人仅基本非敏感信息
                if (currentEmployeeId != null && currentEmployeeId.equals(targetEmployeeId)) {
                    // 本人完整返回
                    return;
                }
                // 非本人：仅保留姓名、性别、部门、职位、职级、在职状态等基本信息
                vo.setPhone(null);
                vo.setEmail(null);
                vo.setIdCard(null);
                vo.setBirthday(null);
                vo.setRegisteredAddress(null);
                vo.setCurrentAddress(null);
                vo.setSalaryAccountId(null);
                vo.setBaseSalary(null);
                vo.setBankAccount(null);
                vo.setBankName(null);
                vo.setReportTo(null);
                vo.setReportToName(null);
                vo.setWorkLocation(null);
                vo.setEntryType(null);
                vo.setEntryTypeLabel(null);
                vo.setEntryDate(null);
                vo.setRegularDate(null);
                vo.setResignDate(null);
                vo.setUserId(null);
                break;

            case SALARY_ONLY:
                // 财务专员：仅看薪资汇总，隐藏个人隐私
                vo.setPhone(null);
                vo.setIdCard(null);
                vo.setBankAccount(null);
                vo.setRegisteredAddress(null);
                vo.setCurrentAddress(null);
                break;
        }
    }

    /**
     * 对已离职员工的敏感字段进行脱敏处理。
     *
     * 脱敏规则：
     * - 身份证号：保留首3尾4（如 110***********1234）
     * - 手机号：保留首3尾4（如 138****5678）
     * - 银行卡号：保留尾4（如 ****1234）
     * - 薪资信息：仅保留 baseSalary 汇总，隐藏 salaryAccountId/bankName
     */
    public EmployeeVO maskSensitiveFields(EmployeeVO vo) {
        if (vo == null) return null;

        // 身份证号脱敏：保留首3尾4
        if (vo.getIdCard() != null && vo.getIdCard().length() >= 7) {
            String prefix = vo.getIdCard().substring(0, 3);
            String suffix = vo.getIdCard().substring(vo.getIdCard().length() - 4);
            vo.setIdCard(prefix + "*".repeat(vo.getIdCard().length() - 7) + suffix);
        } else if (vo.getIdCard() != null) {
            vo.setIdCard("***");
        }

        // 手机号脱敏：保留首3尾4
        if (vo.getPhone() != null && vo.getPhone().length() >= 7) {
            String prefix = vo.getPhone().substring(0, 3);
            String suffix = vo.getPhone().substring(vo.getPhone().length() - 4);
            vo.setPhone(prefix + "*".repeat(vo.getPhone().length() - 7) + suffix);
        } else if (vo.getPhone() != null) {
            vo.setPhone("***");
        }

        // 银行卡号脱敏：保留尾4
        if (vo.getBankAccount() != null && vo.getBankAccount().length() > 4) {
            vo.setBankAccount("*".repeat(vo.getBankAccount().length() - 4)
                    + vo.getBankAccount().substring(vo.getBankAccount().length() - 4));
        } else if (vo.getBankAccount() != null) {
            vo.setBankAccount("****");
        }

        // 薪资：隐藏账套ID和开户行，仅保留基本工资汇总
        vo.setSalaryAccountId(null);
        vo.setBankName(null);

        return vo;
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
