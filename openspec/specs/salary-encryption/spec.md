# salary-encryption

## Purpose

对 HRMS 中 SalaryAccount、SalaryRecord 和 RegularizationApplication 表的金额字段进行 AES-256-GCM 加密存储，通过 EncryptedBigDecimalTypeHandler 实现透明加解密，确保薪资数据在数据库中以密文形式存储。

## Requirements

### Requirement: 薪资字段加密存储

系统 SHALL 对 SalaryAccount 和 SalaryRecord 表中的所有金额字段进行 AES-256-GCM 加密存储，使用随机 IV 以确保最高安全性。加密后的值 MUST 以 Base64 编码存储在对应的 `*_encrypted` 列中。

#### Scenario: 薪资账套金额字段加密

- **WHEN** HR 专员创建或更新员工的薪资账套，设置 basicSalary、positionSalary、performanceSalary、socialInsuranceBase、housingFundBase
- **THEN** 系统通过 `EncryptedBigDecimalTypeHandler` 自动加密所有金额字段后写入数据库，数据库中所有金额列存储为密文

#### Scenario: 工资条金额字段加密

- **WHEN** 系统执行月度薪资计算，生成 SalaryRecord 记录
- **THEN** 所有金额字段（basicSalary、attendanceDeduction、leaveDeduction、overtimePay、grossPay、socialInsurancePersonal、housingFundPersonal、taxableIncome、tax、otherDeduction、netPay）均以密文形式存储

#### Scenario: 薪资数据解密展示

- **WHEN** 有薪资查看权限的用户（HR 专员、财务专员）查询工资条或薪资账套
- **THEN** 系统自动解密金额字段，返回明文数值。无薪资权限的用户（部门主管、普通员工）在 Service 层被拦截，无法获取薪资数据

### Requirement: 转正薪资加密

系统 SHALL 对 RegularizationApplication 实体中的 `formalSalary` 字段进行加密存储。

#### Scenario: 转正申请中的薪资加密

- **WHEN** 发起转正申请，设置转正后薪资 formalSalary
- **THEN** 系统自动加密 formalSalary 后存入数据库，审批通过后写入 Employee 表时同理加密

### Requirement: 薪资字段不支持 SQL 层范围查询

加密后的薪资字段 SHALL NOT 支持数据库层面的范围查询（如 `WHERE base_salary > 10000`）。如需此类查询，MUST 在应用层加载数据后解密并筛选。

#### Scenario: 查询高薪员工改用应用层筛选

- **WHEN** 需要查询基本工资大于 10000 的员工
- **THEN** 系统在应用层加载全量（或分页）员工数据，解密薪资字段后进行筛选，而非通过 SQL WHERE 条件过滤
