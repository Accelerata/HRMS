package com.hrms.service;

import com.hrms.common.context.BaseContext;
import com.hrms.common.exception.BaseException;
import com.hrms.dto.FileUploadResult;
import com.hrms.entity.Employee;
import com.hrms.entity.LeaveAttachment;
import com.hrms.mapper.EmployeeMapper;
import com.hrms.mapper.LeaveAttachmentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 请假附件服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LeaveAttachmentService {

    private final LeaveAttachmentMapper leaveAttachmentMapper;
    private final FileStorageService fileStorageService;
    private final EmployeeMapper employeeMapper;

    @Transactional
    public LeaveAttachment upload(MultipartFile file) {
        Employee employee = resolveCurrentEmployee();
        FileUploadResult result = fileStorageService.upload(file);

        LeaveAttachment attachment = new LeaveAttachment();
        attachment.setApplicationId(null);
        attachment.setFileName(file.getOriginalFilename());
        attachment.setObjectKey(result.getObjectKey());
        attachment.setFileUrl(result.getFileUrl());
        attachment.setFileSize(file.getSize());
        attachment.setContentType(file.getContentType());
        attachment.setUploadBy(employee.getId());
        leaveAttachmentMapper.insert(attachment);

        log.info("请假附件上传成功: id={}, fileName={}, uploadBy={}",
                attachment.getId(), attachment.getFileName(), employee.getId());
        return attachment;
    }

    @Transactional
    public void bindToApplication(Long attachmentId, Long applicationId, Long employeeId) {
        LeaveAttachment attachment = leaveAttachmentMapper.selectById(attachmentId);
        if (attachment == null) {
            throw BaseException.notFound("附件不存在");
        }
        if (!attachment.getUploadBy().equals(employeeId)) {
            throw BaseException.forbidden("不能绑定他人上传的附件");
        }
        leaveAttachmentMapper.bindToApplication(attachmentId, applicationId);
    }

    public List<LeaveAttachment> getAttachments(Long applicationId) {
        return leaveAttachmentMapper.selectByApplicationId(applicationId);
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

