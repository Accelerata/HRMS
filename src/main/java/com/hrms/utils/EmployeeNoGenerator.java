package com.hrms.utils;

import com.hrms.entity.Department;
import com.hrms.mapper.DepartmentMapper;
import com.hrms.mapper.EmployeeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

import java.time.Year;
import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * 工号生成器
 *
 * 规则：年份(4位) + 部门编码(2位) + 序号(3位)，如 202401005
 * 序号按「年 + 部门编码」维度在 DB 中递增（查该年该部门现有最大序号 +1）
 * 并发场景通过唯一约束 + 重试保证唯一性
 *
 * 支持工号回收复用：员工离职时释放工号后缀序号至可复用池，
 * 新员工入职时优先从回收池分配。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmployeeNoGenerator {

    private final EmployeeMapper employeeMapper;
    private final DepartmentMapper departmentMapper;

    private static final int MAX_RETRIES = 3;

    /** 工号后缀序号回收池（内存缓存，服务重启后清空） */
    private final Deque<Integer> recycledSeqs = new ConcurrentLinkedDeque<>();

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
                // 优先从回收池取序号
                int nextSeq;
                Integer recycled = recycledSeqs.pollFirst();
                if (recycled != null) {
                    nextSeq = recycled;
                    log.info("工号序号从回收池复用: seq={}", nextSeq);
                } else {
                    int maxSeq = employeeMapper.selectMaxEmployeeNoSeq(year, deptCode);
                    nextSeq = maxSeq + 1;
                }

                String employeeNo = year + deptCode + String.format("%03d", nextSeq);

                // 预检：如果 DB 中已存在该工号（非预期场景），跳过
                if (employeeMapper.selectByEmployeeNo(employeeNo) != null) {
                    log.warn("工号碰撞: {}, 尝试递增", employeeNo);
                    continue;
                }
                return employeeNo;
            } catch (DuplicateKeyException e) {
                log.warn("工号唯一约束冲突: attempt={}/{}", attempt + 1, MAX_RETRIES);
                if (attempt == MAX_RETRIES - 1) {
                    throw new RuntimeException("工号生成失败，已达最大重试次数", e);
                }
            }
        }
        throw new RuntimeException("工号生成失败");
    }

    /**
     * 释放工号后缀序号至可复用池。
     * 仅释放部门编码部分匹配的工号（同部门同年度优先复用）。
     *
     * @param employeeNo 待释放的工号（如 202601005）
     */
    public void releaseEmployeeNo(String employeeNo) {
        if (employeeNo == null || employeeNo.length() < 9) {
            log.warn("无效工号，无法释放: {}", employeeNo);
            return;
        }
        try {
            // 工号格式：年份(4) + 部门编码(2) + 序号(3)
            String seqPart = employeeNo.substring(employeeNo.length() - 3);
            int seq = Integer.parseInt(seqPart);
            recycledSeqs.addLast(seq);
            log.info("工号序号回收成功: employeeNo={}, seq={}", employeeNo, seq);
        } catch (NumberFormatException e) {
            log.warn("工号序号解析失败，无法释放: {}", employeeNo);
        }
    }

    /**
     * 获取当前回收池中待复用的序号数量
     */
    public int getRecycledCount() {
        return recycledSeqs.size();
    }

    /**
     * 获取部门编码（2位数字，不足两位前面补0）
     */
    private String getDeptCode(Long deptId) {
        Department dept = departmentMapper.selectById(deptId);
        if (dept != null && dept.getDeptCode() != null) {
            String code = dept.getDeptCode();
            if (code.length() >= 2) {
                return code.substring(code.length() - 2);
            }
            return String.format("%02d", Integer.parseInt(code));
        }
        return String.format("%02d", deptId % 100);
    }
}
