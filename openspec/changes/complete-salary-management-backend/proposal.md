## Why

需求文档第 7 大点定义了完整「薪资管理」流程：薪资账套模板、员工薪资档案、月度核算、异常预警、审批通过后工资条可见、工资条首次查看二次验证。经后端代码审计，目前已实现部分核心核算与批次审批能力，但与需求仍存在以下差距：

- **薪资账套仍是员工级固定字段，不是模板化账套（7.1）**：`salary_account` 只保存单员工 basic/position/performance/social/housing 等字段，没有账套名称、适用范围、生效日期、工资项目配置，也没有按部门/职位/职级匹配账套的后端能力。
- **员工薪资档案缺少历史与调薪留痕（7.2.1）**：`SalaryAccount` 有 `effectiveDate/status`，但无调薪历史表、变更原因、操作人、审批来源，也没有薪资档案管理接口。
- **试用期薪资只做基础折算，未完整遵循“仅影响基本工资和津贴”（7.2.2）**：`SalaryCalculationService` 当前把 `basicSalary + positionSalary` 一并按试用期比例折算，`performanceSalary` 未参与计算，账套项目也不可配置。
- **月度核算缺少数据锁定、阻断式异常、调整项与重新计算闭环（7.3.1/7.3.3）**：`SalaryBatchService.batchCalculate` 会吞掉单员工失败并继续，未把“新入职员工未设置薪资档案”作为红色阻断；无考勤锁定标记、无手动调整项、无调整后重算能力；`warnings` 只是逗号字符串，无法表达黄色预警/红色预警/阻断级别。
- **薪资批次状态未覆盖需求全状态（7.3.2）**：已有 `DRAFT/PENDING/APPROVED/REJECTED`，缺少“计算中、待确认、已发放、归档/已归档”语义；`SalaryController` 仍保留单条 confirm/paid 接口，可能绕过批次审批流程。
- **工资条后端未完成（7.4）**：`SalaryController.detail` 直接返回 `null` 且标注未实现注释；未限制“审批通过后方可查看”；员工本人查看无二次验证；工资条列表、详情 VO、首次查看留痕/审计均缺失。
- **薪资查询数据权限未完成（RBAC/IDOR 风险）**：`SalaryController.listRecords` 在未传 employeeId 时返回空列表，传 employeeId 时未做本人/部门/财务/HR 数据范围校验；`yearlyRecords` 也可传任意员工 ID 查询年度薪资。
- **成本报表/后端图表数据接口缺失（7.3.4 的后端支撑）**：前端 AntV 暂不做，但后端需要提供近 6 个月趋势、部门成本分布、薪资构成占比、社保公积金对比、薪资变动分布的数据接口。

已有吻合点：

- `SalaryCalculationService` 已覆盖金额精确到分、迟到/早退/旷工扣款、事假扣款、社保公积金基数上下限、累计预扣个税、请假 >15 天/加班 >50 小时/薪资波动 >30% 预警。
- `SalaryBatchService` 已生成批次、挂接 `salary_record.batch_id`、提交财务审批、审批通过批量确认。
- `SalaryAccount` 和 `SalaryRecord` 金额字段已通过 `EncryptedBigDecimalTypeHandler` 透明加密，符合薪资敏感字段密文存储要求。

## What Changes

补齐薪资管理后端流程，使代码与需求第 7 大点吻合（**仅后端，前端后期再做**）：

- **账套模板与工资项目**：新增薪资方案/工资项目配置表，支持账套名称、适用部门/职位/职级、生效日期、固定收入/变动收入/考勤扣款/社保/公积金/个税项目；员工薪资档案引用模板并保留个人基数。
- **薪资档案与调薪历史**：新增薪资档案管理 Service/Controller；每次创建、调整、停用薪资档案均写入调薪历史；敏感金额继续使用 `EncryptedBigDecimalTypeHandler`。
- **核算流程增强**：批次进入“计算中 → 待确认 → 审批中 → 已通过 → 已发放/已归档/已驳回”流程；核算前锁定当月考勤快照；未配置薪资档案的新入职/在职员工阻断整批提交；预警结构化保存级别、类型、消息与是否阻断。
- **调整项与重算**：新增薪资调整项表，HR 可在待确认/已驳回批次中对员工添加收入或扣款调整项并重算，所有调整项留痕。
- **工资条安全查看**：新增工资条列表/详情接口；只有批次已通过/已发放后可见；员工仅可查看本人；首次查看本人详情必须完成密码二次验证；记录查看审计。
- **薪资数据权限**：所有薪资记录、年度记录、工资条、报表接口统一按数据范围过滤：管理员/HR 全量，财务薪资全量，主管仅部门及下属汇总但不可看员工敏感薪资明细，普通员工仅本人。
- **发放确认与归档**：将发放动作提升到批次级，审批通过后由有权限角色标记已发放，批次内记录置 `PAID`，随后可归档；逐条 confirm/paid 接口移除或收紧为内部服务，避免绕过审批。
- **后端报表数据**：提供薪资趋势、部门成本、构成占比、社保公积金对比、薪资波动分布接口，供后续前端 AntV 使用。

## Capabilities

### New Capabilities

- `salary-management-backend`: 薪资管理后端完整流程 —— 账套模板、员工薪资档案、调薪历史、月度核算准备/预览/调整/审批/发放/归档、工资条安全查看、薪资报表数据接口。

### Modified Capabilities

- `salary-batch-approval`: 批次状态和审批边界增强 —— 扩展批次状态、移除单条确认绕过路径、审批通过后工资条可见、发放确认批次化。
- `salary-encryption`: 新增薪资模板、调整项、调薪历史等金额字段继续透明加密。

## Impact

- **Entity 层**：新增 `SalaryPlan`、`SalaryPlanItem`、`SalaryAdjustment`、`SalaryChangeHistory`、`PayslipViewLog`；扩展 `SalaryBatch` 状态/锁定字段；扩展 `SalaryRecord` 结构化预警与可见时间。
- **Mapper 层**：新增以上实体 Mapper/XML；`SalaryRecordMapper` 增加按数据范围查询、详情查询、年度本人查询、批量 paid/archived 更新；修正现有 `selectByDeptAndMonth` 字段映射。
- **Service 层**：新增 `SalaryPlanService`、`SalaryAccountService`、`PayslipService`、`SalaryReportService`、`SalaryAccessControlService`；增强 `SalaryBatchService` 和 `SalaryCalculationService`。
- **Controller 层**：重构 `SalaryController`，新增账套模板、薪资档案、批次预览/调整/提交/审批/发放/归档、工资条、报表数据接口；移除或限制单条 confirm/paid。
- **数据库**：新增 `sql/phase12-salary-management.sql` 增量脚本，包含模板、项目、调整项、调薪历史、工资条查看审计、批次/记录增列、权限种子。
- **权限**：补齐并分配 `salary:plan:view/manage`、`salary:account:view/manage`、`salary:batch:view/calc/adjust/submit/approve/pay/archive`、`salary:payslip:view/self`、`salary:report:view`；核对 HR/财务/主管/员工权限边界。
- **测试**：新增/扩展薪资账套匹配、调薪历史、批次状态机、阻断预警、调整重算、工资条二次验证、薪资数据权限、报表汇总单元测试。
- **非目标（Non-goals）**：前端页面与 AntV 图表实现；短信验证码通道接入（本期使用登录密码二次验证）；“老板”二审角色配置（现有审批模板仍预留扩展）。
