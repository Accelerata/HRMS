## ADDED Requirements

### Requirement: Position List with Sequence Filter
The system SHALL display a paginated table of positions with a sequence filter (M/P/S). Columns: positionName, positionCode, sequence, gradeRange, defaultProbationMonths, description, isStandard, status.

#### Scenario: Filter by sequence
- **WHEN** user selects sequence "P" from the filter tabs
- **THEN** system calls GET /position/list?sequence=P and displays only P-sequence positions

### Requirement: Create Position
The system SHALL provide a form with validation: positionCode must be unique and global, sequence must be M/P/S, isStandard defaults to 1.

#### Scenario: Create with duplicate code
- **WHEN** user enters an existing positionCode and submits
- **THEN** system displays error "职位编码已存在"

### Requirement: Edit Position
The system SHALL allow editing position details. Changing positionCode to an existing one SHALL be rejected.

#### Scenario: Edit position
- **WHEN** user modifies position details and submits
- **THEN** system calls PUT /position/{id} and refreshes list

### Requirement: Delete Position
The system SHALL require confirmation before deletion and MUST prevent deletion when employees are assigned to the position.

#### Scenario: Delete unused position
- **WHEN** user confirms deletion of an unassigned position
- **THEN** system calls DELETE /position/{id} and removes from list
