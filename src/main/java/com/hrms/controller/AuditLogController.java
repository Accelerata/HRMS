package com.hrms.controller;

import com.hrms.annotation.Auditable;
import com.hrms.annotation.RequirePermission;
import com.hrms.entity.AuditLog;
import com.hrms.mapper.AuditLogMapper;
import com.hrms.result.PageResult;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 审计日志查询与导出
 * 仅系统管理员可访问
 */
@RestController
@RequestMapping("/api/v1/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogMapper auditLogMapper;

    @GetMapping
    @RequirePermission("audit:log:view")
    public PageResult<AuditLog> page(
            @RequestParam(required = false) Long operatorId,
            @RequestParam(required = false) String operation,
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        int offset = (page - 1) * size;
        List<AuditLog> records = auditLogMapper.selectPage(
                operatorId, operation, resourceType, startTime, endTime, offset, size);
        int total = auditLogMapper.countPage(
                operatorId, operation, resourceType, startTime, endTime);
        return PageResult.of(records, total, page, size);
    }

    @GetMapping("/export")
    @Auditable(operation = "EXPORT", resourceType = "AUDIT_LOG")
    @RequirePermission("audit:log:export")
    public void export(
            @RequestParam(required = false) Long operatorId,
            @RequestParam(required = false) String operation,
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            HttpServletResponse response) throws IOException {

        List<AuditLog> records = auditLogMapper.selectAll(
                operatorId, operation, resourceType, startTime, endTime);

        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader("Content-Disposition",
                "attachment; filename=audit_logs_" + java.time.LocalDate.now() + ".csv");

        // BOM for Excel UTF-8 compatibility
        ServletOutputStream out = response.getOutputStream();
        out.write(0xEF);
        out.write(0xBB);
        out.write(0xBF);

        StringBuilder csv = new StringBuilder();
        csv.append("ID,操作人,操作类型,资源类型,资源ID,操作结果,错误信息,客户端IP,操作时间\n");
        for (AuditLog log : records) {
            csv.append(escapeCsv(log.getId()))
                    .append(",").append(escapeCsv(log.getOperatorName()))
                    .append(",").append(escapeCsv(log.getOperation()))
                    .append(",").append(escapeCsv(log.getResourceType()))
                    .append(",").append(escapeCsv(log.getResourceId()))
                    .append(",").append(escapeCsv(log.getResult()))
                    .append(",").append(escapeCsv(log.getErrorMessage()))
                    .append(",").append(escapeCsv(log.getClientIp()))
                    .append(",").append(escapeCsv(log.getCreateTime()))
                    .append("\n");
        }
        out.write(csv.toString().getBytes(StandardCharsets.UTF_8));
        out.flush();
    }

    private String escapeCsv(Object value) {
        if (value == null) return "";
        String s = value.toString();
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}
