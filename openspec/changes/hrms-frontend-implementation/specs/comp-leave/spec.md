## ADDED Requirements

### Requirement: Convert Overtime to Comp Leave
The system SHALL provide a button to convert overtime hours to compensatory leave for a selected employee. The result SHALL display the converted leave days (BigDecimal).

#### Scenario: Trigger conversion
- **WHEN** admin selects employee and clicks "加班折算调休"
- **THEN** system calls POST /comp-leave/convert/{employeeId} and displays converted days

#### Scenario: No overtime to convert
- **WHEN** employee has no overtime records
- **THEN** system returns 0 or displays "无可折算的加班记录"
