package com.hrms.mapper;

import com.hrms.entity.AuditLog;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 审计日志 Mapper
 */
@Mapper
public interface AuditLogMapper {

    @Insert("INSERT INTO audit_log (operator_id, operator_name, operation, resource_type, resource_id, " +
            "request_summary, result, error_message, client_ip, create_time) " +
            "VALUES (#{operatorId}, #{operatorName}, #{operation}, #{resourceType}, #{resourceId}, " +
            "#{requestSummary}, #{result}, #{errorMessage}, #{clientIp}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(AuditLog log);

    @Select("<script>" +
            "SELECT * FROM audit_log WHERE 1=1 " +
            "<if test='operatorId != null'>AND operator_id = #{operatorId}</if> " +
            "<if test='operation != null'>AND operation = #{operation}</if> " +
            "<if test='resourceType != null'>AND resource_type = #{resourceType}</if> " +
            "<if test='startTime != null'>AND create_time &gt;= #{startTime}</if> " +
            "<if test='endTime != null'>AND create_time &lt;= #{endTime}</if> " +
            "ORDER BY create_time DESC " +
            "LIMIT #{offset}, #{size}" +
            "</script>")
    List<AuditLog> selectPage(@Param("operatorId") Long operatorId,
                               @Param("operation") String operation,
                               @Param("resourceType") String resourceType,
                               @Param("startTime") java.time.LocalDateTime startTime,
                               @Param("endTime") java.time.LocalDateTime endTime,
                               @Param("offset") int offset,
                               @Param("size") int size);

    @Select("<script>" +
            "SELECT COUNT(*) FROM audit_log WHERE 1=1 " +
            "<if test='operatorId != null'>AND operator_id = #{operatorId}</if> " +
            "<if test='operation != null'>AND operation = #{operation}</if> " +
            "<if test='resourceType != null'>AND resource_type = #{resourceType}</if> " +
            "<if test='startTime != null'>AND create_time &gt;= #{startTime}</if> " +
            "<if test='endTime != null'>AND create_time &lt;= #{endTime}</if> " +
            "</script>")
    int countPage(@Param("operatorId") Long operatorId,
                  @Param("operation") String operation,
                  @Param("resourceType") String resourceType,
                  @Param("startTime") java.time.LocalDateTime startTime,
                  @Param("endTime") java.time.LocalDateTime endTime);

    @Select("<script>" +
            "SELECT * FROM audit_log WHERE 1=1 " +
            "<if test='operatorId != null'>AND operator_id = #{operatorId}</if> " +
            "<if test='operation != null'>AND operation = #{operation}</if> " +
            "<if test='resourceType != null'>AND resource_type = #{resourceType}</if> " +
            "<if test='startTime != null'>AND create_time &gt;= #{startTime}</if> " +
            "<if test='endTime != null'>AND create_time &lt;= #{endTime}</if> " +
            "ORDER BY create_time DESC" +
            "</script>")
    List<AuditLog> selectAll(@Param("operatorId") Long operatorId,
                              @Param("operation") String operation,
                              @Param("resourceType") String resourceType,
                              @Param("startTime") java.time.LocalDateTime startTime,
                              @Param("endTime") java.time.LocalDateTime endTime);
}
