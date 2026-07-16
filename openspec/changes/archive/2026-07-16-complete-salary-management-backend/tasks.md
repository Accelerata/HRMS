# Tasks: 薪资管理后端补全

## 1. 薪资账套模板与工资项目

- [x] 1.1 新增 `salary_plan`、`salary_plan_item`、`salary_plan_scope` 表，包含账套名称、适用范围、生效日期、项目类型、计算规则、启用状态，并在金额规则字段使用加密 TypeHandler 或非敏感 JSON 配置。
- [x] 1.2 新增 `SalaryPlan`、`SalaryPlanItem`、`SalaryPlanScope` 实体、Mapper、XML。
- [x] 1.3 新增 `SalaryPlanService`：创建/更新/启停账套、维护工资项目、按员工部门/职位/职级匹配有效账套。
- [x] 1.4 新增 `SalaryPlanController`：账套列表、详情、创建、更新、启停；权限使用 `salary:plan:view/manage`。
- [x] 1.5 测试账套匹配优先级：员工指定 > 职位 > 职级 > 部门 > 默认账套。

## 2. 员工薪资档案与调薪历史

- [x] 2.1 扩展 `salary_account`：新增 `plan_id`、`effective_start_date`、`effective_end_date`、`change_reason`、`operator_id`。
- [x] 2.2 新增 `salary_change_history` 表，记录变更前后加密金额、变更原因、操作人、来源业务、变更时间。
- [x] 2.3 新增 `SalaryAccountService`：创建档案、调整档案、停用旧档案、查询员工当前档案与历史。
- [x] 2.4 新增/重构薪资档案接口：HR 管理全量，财务只读，员工不可管理；权限使用 `salary:account:view/manage`。
- [x] 2.5 测试调薪时旧档案失效、新档案生效、历史留痕完整。

## 3. 批次状态机与核算准备

- [x] 3.1 扩展 `salary_batch`：状态支持 `DRAFT/CALCULATING/PENDING_CONFIRM/PENDING_APPROVAL/APPROVED/PAID/ARCHIVED/REJECTED`，新增 `attendance_locked`、`lock_time`、`blocking_reason`。
- [x] 3.2 调整 `SalaryBatchService.batchCalculate`：创建批次后置 `CALCULATING`，计算完成置 `PENDING_CONFIRM`，失败或阻断回到可修正状态并记录原因。
- [x] 3.3 新增考勤锁定逻辑：核算时锁定当月考勤快照，已锁定月份禁止普通考勤修改，补卡/审批通过后触发批次需重算标记。
- [x] 3.4 将"在职/试用员工未配置薪资档案"从单员工异常升级为批次阻断，不允许提交审批。
- [x] 3.5 更新提交审批：仅 `PENDING_CONFIRM/REJECTED` 可提交，提交后状态为 `PENDING_APPROVAL`，审批通过后为 `APPROVED`。
- [x] 3.6 测试完整状态流转和非法状态操作拒绝。

## 4. 结构化预警与手动调整

- [x] 4.1 扩展 `salary_record.warnings` 为结构化 JSON 保存，包含 level/code/message/confirmed。
- [x] 4.2 新增 `salary_adjustment` 表，字段包含 batch_id、employee_id、record_id、type、amount、reason、operator_id，金额加密。
- [x] 4.3 `SalaryCalculationService` 纳入绩效工资、加班费、调整项，并保持所有金额 scale=2。
- [x] 4.4 `SalaryBatchService` 增加添加调整项、删除未提交调整项、重算员工、重算批次能力。
- [x] 4.5 红色预警需 HR 确认后才允许提交审批；阻断预警必须修复后重新核算。
- [x] 4.6 测试请假 >15、加班 >50、薪资波动 >30、无薪资档案阻断、调整项重算。

## 5. 工资条安全查看

- [x] 5.1 新增 `PayslipService`：工资条列表、详情、可见性校验、本人首次查看密码二次验证。
- [x] 5.2 新增 `payslip_view_log` 表，记录查看人、薪资记录、验证方式、查看时间。
- [x] 5.3 新增工资条 VO，按需求返回员工姓名、工号、部门、收入明细、扣除明细、实发金额、预警信息。
- [x] 5.4 重构 `SalaryController.detail`：返回真实工资条，审批通过前不可见；员工只能看本人；首次本人查看必须验证密码。
- [x] 5.5 新增员工历史工资条列表接口，按年月倒序返回本人可见记录。
- [x] 5.6 测试未审批不可见、非本人拒绝、首次无密码拒绝、密码正确后记录审计。

## 6. 薪资数据权限与 IDOR 防护

- [x] 6.1 新增 `SalaryAccessControlService`，集中判断当前用户对 employeeId、recordId、batchId、reportType 的访问权限。
- [x] 6.2 重构 `listRecords`、`yearlyRecords`、`batchRecords`、工资条接口，所有入口先做数据范围校验。
- [x] 6.3 部门主管只允许查看部门汇总或脱敏列表，不允许查看下属工资条详情和敏感薪资字段。
- [x] 6.4 财务可查看薪资全量与成本报表，但不可管理薪资档案。
- [x] 6.5 测试 HR、财务、主管、员工四类角色的薪资查询边界。

## 7. 发放确认、归档与旧接口收口

- [x] 7.1 新增批次级发放确认接口：仅 `APPROVED` 批次可标记 `PAID`，批次内记录批量置 `PAID`。
- [x] 7.2 新增批次归档接口：仅 `PAID` 批次可置 `ARCHIVED`，归档后禁止调整和重算。
- [x] 7.3 收紧 `records/{id}/confirm`、`records/{id}/paid`：改为废弃接口或仅允许内部兼容路径，不得绕过批次审批。
- [x] 7.4 测试审批通过后工资条可见、发放后状态一致、归档后不可修改。

## 8. 后端报表数据

- [x] 8.1 新增 `SalaryReportService`，在应用层解密后汇总近 6 个月应发/实发趋势。
- [x] 8.2 新增部门薪资成本分布、薪资构成占比、社保公积金对比、薪资变动分布查询。
- [x] 8.3 新增 `SalaryReportController`，权限使用 `salary:report:view`，主管只返回本部门汇总，HR/财务返回全量汇总。
- [x] 8.4 测试报表聚合结果、数据范围和加密金额解密汇总。

## 9. SQL、权限与回归验证

- [x] 9.1 新增 `src/main/resources/sql/phase12-salary-management.sql`，包含 DDL、索引、权限种子、角色权限分配。
- [x] 9.2 核对新增金额字段均注册 `EncryptedBigDecimalTypeHandler`。
- [x] 9.3 权限自查：按 AGENTS.md 要求核对每个新增/修改端点的权限码已定义且分配给正确角色。
- [x] 9.4 运行薪资相关单测：`mvn test -Dtest=SalaryCalculationServiceTest,SalaryBatchApprovalTest,*Salary*Test`。
- [x] 9.5 运行全量后端测试：`mvn test`。
- [x] 9.6 运行 OpenSpec 校验：`openspec validate complete-salary-management-backend --strict`。
