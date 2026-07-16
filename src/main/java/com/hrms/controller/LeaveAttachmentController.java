package com.hrms.controller;

import com.hrms.annotation.RequirePermission;
import com.hrms.entity.LeaveAttachment;
import com.hrms.result.Result;
import com.hrms.service.LeaveAttachmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 请假附件控制器
 *
 * 权限说明：
 * - att:leave:apply - 上传附件（员工及以上）
 * - att:leave:view  - 查看附件（本人/审批人/HR）
 */
@RestController
@RequestMapping("/api/v1/leave")
@RequiredArgsConstructor
public class LeaveAttachmentController {

    private final LeaveAttachmentService leaveAttachmentService;

    /** 上传附件（multipart），返回附件 id 与 URL */
    @PostMapping("/attachments")
    @RequirePermission("att:leave:apply")
    public Result<LeaveAttachment> upload(@RequestParam("file") MultipartFile file) {
        return Result.success(leaveAttachmentService.upload(file));
    }

    /** 查看某申请的附件列表 */
    @GetMapping("/{id}/attachments")
    @RequirePermission("att:leave:view")
    public Result<List<LeaveAttachment>> list(@PathVariable Long id) {
        return Result.success(leaveAttachmentService.getAttachments(id));
    }
}
