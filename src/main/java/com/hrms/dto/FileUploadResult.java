package com.hrms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 文件上传结果
 */
@Data
@AllArgsConstructor
public class FileUploadResult {
    /** OSS objectKey */
    private String objectKey;
    /** 文件访问 URL */
    private String fileUrl;
}
