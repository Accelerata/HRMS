package com.hrms.service;

import com.hrms.common.context.BaseContext;
import com.hrms.common.exception.BaseException;
import com.hrms.dto.ApprovalActionDTO;
import com.hrms.dto.SupplementaryCardApplyDTO;
import com.hrms.entity.ApprovalRecord;
import com.hrms.entity.AttendanceRecord;
import com.hrms.entity.Employee;
import com.hrms.entity.SupplementaryCardApplication;
import com.hrms.enums.AttendanceStatus;
import com.hrms.enums.BusinessTypeEnum;
import com.hrms.mapper.AttendanceRecordMapper;
import com.hrms.mapper.EmployeeMapper;
import com.hrms.mapper.SupplementaryCardMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 补卡审批服务
 *
 * 业务规则（需求 8.1）：
 * 1. 员工针对缺卡（MISSING_PUNCH）日期发起补卡，直接上级单级审批
 * 2. 同一员工同一日期同一卡型不允许重复申请（草稿/审批中/已通过）
 * 3. 审批通过后回写考勤记录：打卡时间=补卡时间，状态=NORMAL
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SupplementaryCardService {

    /** 卡型 */
    private static final int CARD_TYPE_IN = 1;
    private static final int CARD_TYPE_OUT = 2;

    /** 状态: 1-审批中 2-已通过 3-已拒绝（申请即进入审批） */
    private static final int STATUS_PENDING = 1;
    private static final int STATUS_APPROVED = 2;
    private static final int STATUS_REJECTED = 3;

    private final SupplementaryCardMapper cardMapper;
    private final AttendanceRecordMapper attendanceRecordMapper;
    private final EmployeeMapper employeeMapper;
    private final ApprovalStateMachineService stateMachine;

    /**
     * 发起补卡申请（直接进入审批）
     */
    @Transactional
    public SupplementaryCardApplication apply(SupplementaryCardApplyDTO dto) {
        Employee employee = resolveCurrentEmployee();

        if (dto.getCardType() != CARD_TYPE_IN && dto.getCardType() != CARD_TYPE_OUT) {
            throw BaseException.badRequest("卡型非法: 1-上班卡 2-下班卡");
        }

        // 1. 校验该日期对应卡型确为缺卡
        AttendanceRecord record = attendanceRecordMapper.selectByEmployeeAndDate(
                employee.getId(), dto.getAttendanceDate());
        if (record == null) {
            throw BaseException.badRequest("该日期无考勤记录，无法补卡");
        }
        String punchStatus = dto.getCardType() == CARD_TYPE_IN
                ? record.getPunchInStatus() : record.getPunchOutStatus();
        if (!AttendanceStatus.MISSING_PUNCH.name().equals(punchStatus)) {
            throw BaseException.badRequest("该日期" + (dto.getCardType() == CARD_TYPE_IN ? "上班" : "下班")
                    + "卡并非缺卡状态，无需补卡");
        }

        // 2. 每月最多2次补卡限制
        int currentMonthCount = cardMapper.countByEmployeeAndMonth(
                employee.getId(), dto.getAttendanceDate().getYear(),
                dto.getAttendanceDate().getMonthValue());
        if (currentMonthCount >= 2) {
            throw BaseException.badRequest("每月最多申请 2 次补卡，本月已达上限");
        }

        // 3. 防重复申请
        int active = cardMapper.countActiveByEmployeeDateType(
                employee.getId(), dto.getAttendanceDate(), dto.getCardType());
        if (active > 0) {
            throw BaseException.badRequest("该日期该卡型已存在补卡申请，请勿重复提交");
        }

        // 3. 创建申请（直接审批中）
        SupplementaryCardApplication application = new SupplementaryCardApplication();
        application.setEmployeeId(employee.getId());
        application.setAttendanceDate(dto.getAttendanceDate());
        application.setCardType(dto.getCardType());
        application.setSupplementTime(dto.getSupplementTime());
        application.setReason(dto.getReason());
        application.setStatus(STATUS_PENDING);
        cardMapper.insert(application);

        // 4. 生成直接上级审批待办
        stateMachine.startApproval(BusinessTypeEnum.CARD.getCode(), application.getId(),
                ApprovalStateMachineService.ApprovalContext.ofEmployee(
                        employee.getId(), employee.getDeptId(), employee.getUserId()));

        log.info("补卡申请已提交: id={}, employeeId={}, date={}, cardType={}",
                application.getId(), employee.getId(), dto.getAttendanceDate(), dto.getCardType());
        return application;
    }

    /**
     * 审批补卡申请
     */
    @Transactional
    public void approve(Long id, ApprovalActionDTO dto) {
        SupplementaryCardApplication application = cardMapper.selectById(id);
        if (application == null) {
            throw BaseException.notFound("补卡申请不存在");
        }
        if (application.getStatus() != STATUS_PENDING) {
            throw BaseException.badRequest("当前状态不可审批");
        }

        List<ApprovalRecord> records = stateMachine.getApprovalRecords(BusinessTypeEnum.CARD.getCode(), id);
        ApprovalRecord myRecord = records.stream()
                .filter(r -> r.getIsPending() == 1 && r.getApproverId().equals(BaseContext.getCurrentUserId()))
                .findFirst()
                .orElseThrow(() -> BaseException.badRequest("您没有待处理的审批"));

        ApprovalStateMachineService.ApprovalResult result = stateMachine.processApproval(
                myRecord.getId(), dto.getAction(), dto.getComment(), BaseContext.getCurrentUserId());

        if (result.isTerminated()) {
            cardMapper.updateStatus(id, STATUS_REJECTED);
            log.info("补卡申请被拒绝: id={}", id);
            return;
        }

        if (result.isApproved()) {
            cardMapper.updateStatus(id, STATUS_APPROVED);
            writeBackAttendance(application);
            log.info("补卡申请审批通过，考勤已回写: id={}", id);
        }
    }

    /** 查询我的补卡申请 */
    public List<SupplementaryCardApplication> myApplications() {
        Employee employee = resolveCurrentEmployee();
        return cardMapper.selectByEmployee(employee.getId());
    }

    /**
     * 审批通过后回写考勤记录：打卡时间=补卡时间，状态=NORMAL
     */
    private void writeBackAttendance(SupplementaryCardApplication application) {
        AttendanceRecord record = attendanceRecordMapper.selectByEmployeeAndDate(
                application.getEmployeeId(), application.getAttendanceDate());
        if (record == null) {
            log.warn("回写考勤失败，记录不存在: employeeId={}, date={}",
                    application.getEmployeeId(), application.getAttendanceDate());
            return;
        }
        if (application.getCardType() == CARD_TYPE_IN) {
            attendanceRecordMapper.updatePunchIn(record.getId(),
                    application.getSupplementTime(), AttendanceStatus.NORMAL.name());
        } else {
            attendanceRecordMapper.updatePunchOut(record.getId(),
                    application.getSupplementTime(), AttendanceStatus.NORMAL.name());
        }
    }

    private Employee resolveCurrentEmployee() {
        Long employeeId = BaseContext.getCurrentEmployeeId();
        if (employeeId == null) {
            throw BaseException.badRequest("当前用户无关联员工档案");
        }
        Employee employee = employeeMapper.selectById(employeeId);
        if (employee == null) {
            throw BaseException.notFound("员工档案不存在");
        }
        return employee;
    }
}
