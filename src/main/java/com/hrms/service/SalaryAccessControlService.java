package com.hrms.service;

import com.hrms.common.context.BaseContext;
import com.hrms.common.exception.BaseException;
import com.hrms.entity.Employee;
import com.hrms.mapper.EmployeeMapper;
import com.hrms.mapper.SalaryRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 薪资数据权限控制服务
 *
 * 数据范围：
 * - 管理员(1)/HR(2): 全量
 * - 财务(4): 薪资全量
 * - 主管(3): 本部门及下属汇总，不可看明细
 * - 员工(5): 仅本人
 */
@Service
@RequiredArgsConstructor
public class SalaryAccessControlService {

    private final EmployeeMapper employeeMapper;

    /** 校验当前用户能否查询该员工的薪资记录 */
    public void checkEmployeeAccess(Long targetEmployeeId) {
        Integer scope = BaseContext.getDataScope();
        if (scope == null) throw BaseException.forbidden("无薪资查看权限");
        if (scope == 1 || scope == 2 || scope == 4) return; // 管理员/HR/财务全量
        if (scope == 3) throw BaseException.forbidden("部门主管不可查看员工薪资明细");
        if (scope == 5) {
            Long currentEmpId = BaseContext.getCurrentEmployeeId();
            if (!targetEmployeeId.equals(currentEmpId)) {
                throw BaseException.forbidden("仅可查看本人薪资");
            }
        }
    }

    /** 校验当前用户能否查看该部门的薪资汇总 */
    public void checkDeptAccess(Long deptId) {
        Integer scope = BaseContext.getDataScope();
        if (scope == null) throw BaseException.forbidden("无薪资查看权限");
        if (scope == 1 || scope == 2 || scope == 4) return;
        if (scope == 3) {
            Long currentDeptId = BaseContext.getCurrentDeptId();
            if (currentDeptId == null || !currentDeptId.equals(deptId)) {
                throw BaseException.forbidden("仅可查看本部门薪资");
            }
        }
        if (scope == 5) throw BaseException.forbidden("普通员工不可查看部门薪资");
    }

    /** 校验是否有薪资管理权限（账套/档案管理） */
    public void checkManageAccess() {
        Integer scope = BaseContext.getDataScope();
        if (scope == null || (scope != 1 && scope != 2)) {
            throw BaseException.forbidden("仅HR/管理员可管理薪资档案");
        }
    }

    /** 校验财务权限 */
    public boolean isFinance() {
        Integer scope = BaseContext.getDataScope();
        return scope != null && scope == 4;
    }
}
