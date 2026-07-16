## ADDED Requirements

### Requirement: Audit Log Query
The system SHALL display paginated audit logs with multi-condition filters: operatorId, operation (SELECT/CREATE/UPDATE/DELETE), resourceType (EMPLOYEE/DEPT/ATTENDANCE/LEAVE/SALARY...), startTime, endTime. Results SHALL show operatorName, operation, resourceType, resourceId, result, errorMessage, clientIp, createTime.

#### Scenario: Filter by operation and resource
- **WHEN** admin selects operation=CREATE, resourceType=EMPLOYEE, time range
- **THEN** system calls GET /audit-logs with all parameters and renders table

#### Scenario: Pagination
- **WHEN** admin navigates to page 2
- **THEN** system calls GET /audit-logs?page=2&size=20

### Requirement: Export Audit Logs
The system SHALL provide "导出CSV" button. On click, system downloads a CSV file with the current filter criteria (excluding pagination params).

#### Scenario: Export with filters
- **WHEN** admin applies filters and clicks "导出CSV"
- **THEN** system calls GET /audit-logs/export with filter params and triggers file download
