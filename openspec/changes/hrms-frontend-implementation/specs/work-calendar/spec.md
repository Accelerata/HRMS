## ADDED Requirements

### Requirement: Calendar View
The system SHALL display an annual calendar view with holidays (dayType=1) and makeup workdays (dayType=2) marked by color. Holidays SHALL be in orange, makeup days in blue.

#### Scenario: View 2026 calendar
- **WHEN** user selects year=2026
- **THEN** system calls GET /calendar?year=2026 and renders calendar grid

### Requirement: Batch Save Calendar
The system SHALL provide a batch configuration interface: select dates in a date picker, assign dayType and name, then save all at once.

#### Scenario: Configure National Day holiday
- **WHEN** user selects Oct 1-7 as type=1 (holiday), enters "国庆节", and saves
- **THEN** system calls POST /calendar/batch with array of calendar entries

#### Scenario: Configure makeup workday
- **WHEN** user selects Oct 10 as type=2 (makeup workday), enters "国庆调班补班", and saves
- **THEN** system adds to batch and saves

### Requirement: Delete Calendar Entry
The system SHALL allow deleting a calendar entry by date. Deletion MUST require confirmation.

#### Scenario: Delete a holiday entry
- **WHEN** user deletes Oct 10 entry
- **THEN** system calls DELETE /calendar?date=2026-10-10 and refreshes calendar
