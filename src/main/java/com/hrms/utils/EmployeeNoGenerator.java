package com.hrms.utils;

import com.hrms.entity.Department;
import com.hrms.mapper.DepartmentMapper;
import com.hrms.mapper.EmployeeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

import java.time.Year;

/**
 * 工号生成器
 *
 * 规则：年份(4位) + 部门编码(2位) + 序号(3位)，如 202401005
 * 序号按「年 + 部门编码」维度在 DB 中递增（查该年该部门现有最大序号 +1）
 * 并发场景通过唯一约束 + 重试保证唯一性
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmployeeNoGenerator {

    private final EmployeeMapper employeeMapper;
    private final DepartmentMapper departmentMapper;

    private static final int MAX_RETRIES = 3;

    /**
     * 为指定部门生成唯一工号
     *
     * @param deptId 部门ID
     * @return 唯一工号
     */
    public String generate(Long deptId) {
        String year = String.valueOf(Year.now().getValue());
        String deptCode = getDeptCode(deptId);

        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            try {
                int maxSeq = employeeMapper.selectMaxEmployeeNoSeq(year, deptCode);
                int nextSeq = maxSeq + 1;
                String employeeNo = year + deptCode + String.format("%03d", nextSeq);

                // 预检：如果 DB 中已存在该工号（非预期场景），跳过
                if (employeeMapper.selectByEmployeeNo(employeeNo) != null) {
                    log.warn("工号碰撞: {}, 尝试递增", employeeNo);
                    continue;
                }
                return employeeNo;
            } catch (DuplicateKeyException e) {
                log.warn("工号唯一约束冲突: attempt={}/{}", attempt + 1, MAX_RETRIES);
                // 捕获唯一约束冲突后重试
                if (attempt == MAX_RETRIES - 1) {
                    throw new RuntimeException("工号生成失败，已达最大重试次数", e);
                }
            }
        }
        throw new RuntimeException("工号生成失败");
    }

    /**
     * 获取部门编码（2位数字，不足两位前面补0）
     */
    private String getDeptCode(Long deptId) {
        Department dept = departmentMapper.selectById(deptId);
        if (dept != null && dept.getDeptCode() != null) {
            // 如果部门表有 dept_code 字段，取后2位
            String code = dept.getDeptCode();
            if (code.length() >= 2) {
                return code.substring(code.length() - 2);
            }
            return String.format("%02d", Integer.parseInt(code));
        }
        // 没有 dept_code 时使用部门ID后2位
        return String.format("%02d", deptId % 100);
    }
}
