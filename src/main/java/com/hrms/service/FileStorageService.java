package com.hrms.service;

import com.hrms.common.exception.BaseException;
import com.hrms.dto.FileUploadResult;
import com.hrms.utils.AliOssUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.UUID;

/**
 * 文件存储服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "pdf");
    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyyMM");

    @Value("${hrms.attachment.max-size:10485760}")
    private long maxFileSize;

    private final AliOssUtil aliOssUtil;

    /**
     * 上传文件到 OSS，返回 objectKey 和 URL
     */
    public FileUploadResult upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw BaseException.badRequest("文件不能为空");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.contains(".")) {
            throw BaseException.badRequest("文件名不合法");
        }
        String ext = originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw BaseException.badRequest("文件类型不支持，仅允许 jpg/jpeg/png/pdf");
        }

        if (file.getSize() > maxFileSize) {
            throw BaseException.badRequest("文件过大，最大允许 " + (maxFileSize / 1024 / 1024) + "MB");
        }

        String month = LocalDate.now().format(MONTH_FMT);
        String objectKey = "leave/" + month + "/" + UUID.randomUUID() + "." + ext;

        try {
            byte[] bytes = file.getBytes();
            String url = aliOssUtil.upload(bytes, objectKey);
            log.info("文件上传成功: objectKey={}, url={}", objectKey, url);
            return new FileUploadResult(objectKey, url);
        } catch (IOException e) {
            log.error("文件上传失败: {}", e.getMessage(), e);
            throw BaseException.badRequest("文件上传失败");
        }
    }
}

