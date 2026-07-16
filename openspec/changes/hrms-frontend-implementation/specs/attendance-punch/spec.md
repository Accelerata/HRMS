## ADDED Requirements

### Requirement: Punch In
The system SHALL provide a punch-in button on the attendance page. On click, it MUST call POST /attendance/punch-in with current employeeId, groupId, and current timestamp. The system SHALL display the punch result (status, lateMinutes).

#### Scenario: Normal punch-in
- **WHEN** user clicks punch-in at 08:55
- **THEN** system records punch with status "NORMAL" and lateMinutes=0

#### Scenario: Late punch-in
- **WHEN** user clicks punch-in at 09:20 and group threshold is 15 minutes
- **THEN** system records punch with status "LATE" and lateMinutes=20

#### Scenario: Duplicate punch-in
- **WHEN** user attempts to punch-in again on the same day
- **THEN** system displays error "今日已打卡"

### Requirement: Punch Out
The system SHALL provide a punch-out button. On click, it MUST call POST /attendance/punch-out.

#### Scenario: Normal punch-out
- **WHEN** user clicks punch-out at 18:05
- **THEN** system records punch-out and calculates daily status

#### Scenario: Early punch-out
- **WHEN** user clicks punch-out at 17:30 and group end time is 18:00
- **THEN** system marks status "EARLY" with earlyMinutes=30

### Requirement: Attendance Records
The system SHALL display the current month's attendance records in a calendar view or table, showing daily status (NORMAL/LATE/EARLY/ABSENT/MISSING).

#### Scenario: View attendance records
- **WHEN** page loads
- **THEN** system calls GET /attendance/records/{employeeId}?page=1&size=31 and renders records

### Requirement: Attendance Groups Management
The system SHALL display attendance groups list and allow CRUD operations. The form MUST validate groupType (1=固定班, 2=弹性班, 3=排班制) and require startTime/endTime.

#### Scenario: Create flexible attendance group
- **WHEN** user creates a group with type=2, startTime=09:30, endTime=18:30
- **THEN** system calls POST /attendance/groups and refreshes list
