## ADDED Requirements

### Requirement: Salary Monthly Trend Report
The system SHALL render an AntV line chart showing monthly grossTotal and netTotal trends with employee count.

#### Scenario: View trend
- **WHEN** finance user navigates to salary reports
- **THEN** system calls GET /salary/reports/trend and renders dual-line chart (grossTotal + netTotal)

### Requirement: Department Cost Distribution Report
The system SHALL render an AntV pie/treemap chart showing per-department salary cost distribution with percentage labels.

#### Scenario: View dept cost
- **WHEN** user selects year=2026, month=7
- **THEN** system calls GET /salary/reports/dept-cost?year=2026&month=7 and renders pie chart

### Requirement: Salary Composition Report
The system SHALL render an AntV stacked bar or rose chart showing salary composition breakdown: baseSalary, bonus, overtime, allowance, socialInsurance, housingFund, incomeTax.

#### Scenario: View composition
- **WHEN** user selects year=2026, month=7
- **THEN** system calls GET /salary/reports/composition?year=2026&month=7 and renders composition chart

### Requirement: Alert Warnings
The system SHALL highlight warning indicators when: leave days > 15, overtime hours > 50, salary fluctuation > 30% month-over-month.

#### Scenario: Trigger warning
- **WHEN** salary report shows >30% salary change for an employee
- **THEN** system highlights the anomaly in orange/red
