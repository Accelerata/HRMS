package com.hrms.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 请假附件元数据
 */
@Data
public class LeaveAttachment {

    /** 主键ID */
    private Long id;

    /** 关联 leave_application.id（绑定前为 NULL） */
    private Long applicationId;

    /** 原始文件名 */
    private String fileName;

    /** OSS objectName */
    private String objectKey;

    /** 文件访问 URL */
    private String fileUrl;

    /** 文件大小（字节） */
    private Long fileSize;

    /** MIME 类型 */
    private String contentType;

    /** 上传人（employee.id） */
    private Long uploadBy;

    /** 创建时间 */
    private LocalDateTime createTime;
}
