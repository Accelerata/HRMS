# advanced-employee-search

## Purpose

扩展员工列表查询的高级搜索功能，补齐需求 4.2.2 中定义但未实现的筛选条件：部门树多选、职位多选、职级多选、入职日期范围、手机号搜索。

## ADDED Requirements

### Requirement: 部门树多选筛选

系统 SHALL 支持在员工查询时传入多个部门 ID 进行筛选，返回指定部门及其下属部门的在职员工。

#### Scenario: 多部门筛选

- **WHEN** HR 传入 deptIds=[1, 5, 8] 查询员工列表
- **THEN** 系统返回所属部门 ID 在列表中的员工，按分页返回

#### Scenario: 部门筛选结合关键词

- **WHEN** HR 同时传入 keyword="张" 和 deptIds=[2, 3]
- **THEN** 系统返回姓名或工号包含"张"且部门为 2 或 3 的员工

### Requirement: 职位多选筛选

系统 SHALL 支持传入多个职位 ID 进行员工筛选。

#### Scenario: 按职位筛选

- **WHEN** HR 传入 positionIds=[10, 11, 12] 查询员工
- **THEN** 系统返回职位 ID 在指定列表中的员工

### Requirement: 在职状态多选筛选

系统 SHALL 支持在查询时传入多个在职状态进行筛选（如同时查询试用期和正式员工）。

#### Scenario: 多状态筛选

- **WHEN** HR 传入 statuses=["PROBATION", "REGULAR"] 查询
- **THEN** 系统返回在职状态为试用期或正式的员工

### Requirement: 职级多选筛选

系统 SHALL 支持传入多个职级进行员工筛选。

#### Scenario: 按职级筛选

- **WHEN** HR 传入 grades=["P5", "P6", "P7"] 查询
- **THEN** 系统返回职级在指定列表中的员工

### Requirement: 入职日期范围筛选

系统 SHALL 支持传入入职日期起止范围过滤员工。

#### Scenario: 按入职日期范围筛选

- **WHEN** HR 传入 startDate=2025-01-01 和 endDate=2025-12-31
- **THEN** 系统返回入职日期在该范围内的员工

#### Scenario: 仅传入开始日期

- **WHEN** HR 仅传入 startDate=2025-06-01
- **THEN** 系统返回入职日期在 2025-06-01 及之后的员工

### Requirement: 手机号搜索

系统 SHALL 支持通过手机号搜索员工。由于手机号加密存储，搜索 MUST 通过计算 `phone_hash = SHA-256(phonePlaintext + pepper)` 后与数据库 `phone_hash` 列精确匹配实现。

#### Scenario: 手机号精确搜索

- **WHEN** HR 在关键词中传入完整手机号"13812345678"
- **THEN** 系统计算哈希后在数据库中精确匹配 phone_hash 列，返回匹配的员工

#### Scenario: 手机号搜索无结果

- **WHEN** HR 传入不存在的手机号
- **THEN** 系统返回空列表

### Requirement: 高级搜索条件组合

系统 SHALL 支持所有筛选条件的任意组合，各条件之间为 AND 关系。

#### Scenario: 多条件组合搜索

- **WHEN** HR 同时传入 deptIds=[2]、positionIds=[10]、statuses=["REGULAR"]、startDate=2024-01-01
- **THEN** 系统返回同时满足所有条件的员工列表
