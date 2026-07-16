# Design: 薪资管理后端补全

## Scope

本 change 只处理后端。目标是把需求文档第 7 大点的薪资管理流程落到 Spring Boot + MyBatis 后端：账套模板、员工薪资档案、月度核算、预警/阻断、调整项、审批后工资条可见、工资条二次验证、薪资数据范围和报表数据接口。

## Current State

现有实现分散在 `SalaryController`、`SalaryBatchService`、`SalaryCalculationService`、`SalaryAccountMapper`、`SalaryRecordMapper`、`SalaryBatchMapper`。

- 核算公式已有基础能力：试用期比例、迟到/早退/旷工扣款、事假扣款、社保公积金、累计预扣个税、三类预警。
- 批次审批已有基础能力：批量核算生成批次，提交财务审批，审批通过后批量确认。
- 薪资金额字段已接入 `EncryptedBigDecimalTypeHandler`。
- 控制层仍存在未实现点：列表数据范围、工资条详情、二次验证。

## Proposed Architecture

### 1. Salary Plan Layer

新增薪资模板层：

- `SalaryPlan`：账套名称、适用范围类型（department/position/grade/manual）、适用对象 ID、生效日期、状态。
- `SalaryPlanItem`：工资项目编码、名称、类型、计算规则、排序、是否启用。
- `SalaryPlanService`：提供模板 CRUD、启停、匹配员工默认模板。

员工薪资档案 `SalaryAccount` 保留个人金额和缴费基数，但新增 `planId`，表示该员工使用哪个模板。个人金额仍加密存储。

### 2. Salary Account and Change History

新增 `SalaryAccountService` 管理员工薪资档案：

- 创建或调整薪资档案时，停用旧生效记录，插入新记录。
- 写入 `SalaryChangeHistory`，记录变更前后金额、原因、操作人、来源业务。
- 不对加密金额做 SQL LIKE 或范围查询。

### 3. Batch State Machine

批次状态扩展为：

- `DRAFT`：草稿
- `CALCULATING`：计算中
- `PENDING_CONFIRM`：待确认
- `PENDING_APPROVAL`：审批中
- `APPROVED`：已通过，工资条可见
- `PAID`：已发放
- `ARCHIVED`：已归档
- `REJECTED`：已驳回

`SalaryBatchService` 负责状态流转：

1. 创建批次并置 `CALCULATING`。
2. 锁定当月考勤快照。
3. 校验所有应核算员工都有薪资档案；缺失则记录阻断并终止为 `DRAFT` 或 `REJECTED`。
4. 计算成功后置 `PENDING_CONFIRM`。
5. HR 可添加调整项并重算。
6. HR 提交后置 `PENDING_APPROVAL`，走现有审批引擎。
7. 财务通过后置 `APPROVED`，工资条可见。
8. 发放确认后置 `PAID`，批次内记录置 `PAID`。
9. 归档后置 `ARCHIVED`，禁止继续调整。

### 4. Warnings and Adjustments

预警从逗号字符串升级为结构化 JSON：

- `level`：`YELLOW`、`RED`、`BLOCKING`
- `code`：如 `LEAVE_OVER_15_DAYS`
- `message`：展示消息
- `confirmed`：HR 是否确认过红色预警

新增 `SalaryAdjustment`：

- 仅允许在 `PENDING_CONFIRM` 或 `REJECTED` 批次中添加。
- 类型为收入或扣款，金额加密。
- 重算时进入 `grossPay` 或 `otherDeduction`，并保留调整原因和操作人。

### 5. Payslip Access

新增 `PayslipService`：

- 工资条仅在批次 `APPROVED/PAID/ARCHIVED` 后可见。
- 员工只能看本人；HR/财务可按权限看全量；主管不可查看下属工资条详情。
- 员工首次查看某条工资条必须传入密码二次验证，调用现有密码校验工具或 `AuthService` 能力。
- 每次详情查看写入 `PayslipViewLog`，包含查看人、记录 ID、是否二次验证、IP/UA（如当前请求上下文可取）。

### 6. Salary Access Control

新增 `SalaryAccessControlService` 集中处理薪资 IDOR 防护：

- 管理员/HR：全平台薪资明细。
- 财务：薪资相关数据全量。
- 部门主管：仅允许查看本部门及下属的汇总或脱敏列表，不返回员工工资条明细。
- 普通员工：仅本人薪资记录和工资条。

控制器不直接按 `employeeId` 查薪资，必须先调用访问控制服务生成可访问范围或校验目标记录。

### 7. Reporting APIs

新增 `SalaryReportService` 提供前端图表数据：

- 近 6 个月应发/实发趋势。
- 部门薪资成本分布。
- 当月薪资构成占比。
- 社保公积金扣除对比。
- 薪资波动分布。

由于薪资金额已加密，报表汇总在应用层解密后计算，SQL 只做月份、部门、员工范围过滤。

## Data Security

新增金额字段必须使用 `EncryptedBigDecimalTypeHandler`。涉及精确匹配的敏感文本字段如后续加入银行卡、身份证等，必须同步 hash 辅助索引；本 change 不新增此类字段。薪资金额不做数据库层范围查询。

## Testing Strategy

- `SalaryCalculationServiceTest` 扩展模板项目、调整项、阻断预警场景。
- 新增 `SalaryBatchManagementTest` 覆盖状态机、考勤锁定、提交/审批/发放/归档。
- 新增 `PayslipServiceTest` 覆盖工资条可见性、本人限制、首次二次验证、查看审计。
- 新增 `SalaryAccessControlServiceTest` 覆盖 HR/财务/主管/员工数据范围。
- 新增 Mapper XML 测试或服务层 mock 测试验证新增金额字段 TypeHandler 注册。

## Risks

- 加密金额导致报表无法在 SQL 层聚合，应用层汇总需要控制分页与月份范围；本期限定报表为 6 个月和单批次范围，避免全库扫描。
- 批次状态扩展会影响已有 `SalaryBatchApprovalTest`，需要同步更新测试预期。
- 旧接口 `confirm/paid` 如直接删除会影响前端；本期后端可先保留但改为校验批次状态并标记废弃，待前端迁移后移除。
