## Why

需求文档第 8 大点定义了「审批中心」：七类审批业务（入职/转正/调岗/离职/请假/补卡/薪资批次）、审批人工作台（待办列表、详情页、转交）与委托审批。经代码审计，入转调离四类审批流已随上一 change 落地（模板驱动、顺序门控、48h 超时催办），但审批中心整体完成度约 40%，存在以下差距：

- **请假审批完全缺失（8.1 + 6.3.4）**：`LeaveService.apply` 只创建草稿，无提交/审批/取消闭环，申请永远停留在草稿；余额在申请时即扣减且拒绝后无回补；「直接上级」审批人指向引擎不支持；分级审批规则（年假/调休>3天、病/事假>1天加审部门负责人；婚/产/丧假 HR 备案）未落地。
- **补卡审批完全缺失（8.1）**：无 `supplementary_card` 表、实体、接口，缺卡员工无补救通道。
- **薪资批次审批缺失（8.1）**：`SalaryBatchService.batchCalculate` 直接落库 DRAFT 记录，无批次实体、无「财务专员 → [老板]」审批流，`salary_record` 的 CONFIRMED 靠单条 confirm 接口，无审批记录留痕。
- **工作台功能不全（8.2）**：待办列表缺「申请人、截止时间」（VO 有 `applicantName`/`stepName` 字段但从未填充，`dueTime` 未返回）；无「转交」操作；无统一审批详情接口（8.2.2 要求按类型展示申请信息 + 审批历史 + 操作区）。
- **委托审批完全缺失（8.3）**：无委托表/接口，无法设置委托、自动改派、取消，也无「XXX 代 YYY 审批」留痕。
- **通知对象错误（引擎缺陷）**：`ApprovalStateMachineService.notifySubmitter` 名义上通知提交人，实际把结果通知发给了第一级审批人。
- **权限码断链（阻断非管理员使用）**：`LeaveController` 使用 `leave:*` 权限码，但 RBAC 种子是 `att:leave:*`，非管理员访问请假接口一律 403；补卡无权限码；`ApprovalWorkbenchController` 限定入转调离四个 manage 权限，财务专员（需审批薪资批次）与仅审批请假/补卡的主管无法进入工作台（种子中 `approval:workbench/view/approve` 权限码闲置未用，且 ROLE_FINANCE 未分配 approval 权限树）。

## What Changes

补齐审批中心后端缺失能力，使代码与需求第 8 大点吻合（**仅后端，前端后期再做**）：

- **请假审批流**：申请→提交→审批→取消全闭环；余额改为提交时占用、拒绝/取消自动回补；按 6.3.4 落地分级规则（直接上级 1 级；年假/调休>3天、病/事假>1天追加部门负责人；婚/产/丧假上级审批通过后 HR 备案通知，无需二审）；引擎新增 `direct_supervisor` 审批人指向（employee.report_to，缺省回退部门负责人）。
- **补卡审批**：新增补卡申请表与服务，员工针对缺卡日期发起 → 直接上级审批 → 通过后回写 `attendance_record` 打卡时间与状态。
- **薪资批次审批**：新增 `salary_batch` 批次实体，`salary_record` 挂接批次；HR 批量核算后提交 → 财务专员审批 → 通过则批次生效且批次内记录批量 CONFIRMED，拒绝则退回草稿（需求中「老板」为可选二审，系统无老板角色，本期仅财务一级，模板预留扩展）。
- **转交与委托审批**：审批记录支持转交（一次性改派）；新增 `approval_delegation` 委托设置/取消/查询，委托生效期间新产生的审批任务自动改派被委托人；转交/委托任务审批时留痕「XXX 代 YYY 审批」。
- **工作台增强**：待办列表填充申请人/步骤名称/截止时间；新增统一审批详情接口（按业务类型聚合申请信息 + 审批历史 + 当前用户可执行操作）。
- **引擎修正**：`BusinessTypeEnum` 扩展请假(6)/补卡(7)，薪资批次复用 5；`notifySubmitter` 改为通知真实提交人（`ApprovalContext` 携带 submitterUserId）。
- **权限修正**：`LeaveController` 权限码对齐种子 `att:leave:*`；新增补卡权限码 `att:card:apply/approve`；工作台改用种子已有的 `approval:workbench/view/approve`；ROLE_FINANCE 补 approval 权限树。

## Capabilities

### New Capabilities

- `leave-approval-flow`: 请假审批完整实现 —— 提交/审批/取消闭环、6.3.4 分级审批规则、余额提交占用与拒绝回补、婚产丧假 HR 备案通知。
- `supplementary-card-approval`: 补卡申请与审批 —— 缺卡校验、直接上级单级审批、通过后回写考勤记录。
- `salary-batch-approval`: 薪资批次审批 —— 批次实体、批量核算挂接、提交→财务审批→批量确认/退回。
- `approval-task-assignment`: 审批任务改派 —— 单次转交、委托审批（设置/取消/自动改派）、「XXX 代 YYY 审批」留痕。
- `approval-workbench`: 审批人工作台 —— 待办/已办列表字段补全、统一审批详情、转交入口。

### Modified Capabilities

- `approval-engine-enhancement`: 引擎扩展 —— 新业务类型（请假/补卡/薪资批次）、`direct_supervisor` 审批人指向、提交人通知修正、委托自动改派钩子、代审留痕字段。

## Impact

- **Entity 层**：新增 `SupplementaryCardApplication`、`SalaryBatch`、`ApprovalDelegation`；`ApprovalRecord` 增加 `originalApproverId/originalApproverName/assignType`；`LeaveApplication` 增加 `handoverTo` 与取消状态；`SalaryRecord` 增加 `batchId`。
- **枚举**：`BusinessTypeEnum` 增加 LEAVE(6)/CARD(7)；`ApproverTargetEnum` 增加 DIRECT_SUPERVISOR。
- **Mapper 层**：新增三张表 Mapper + XML；`ApprovalRecordMapper` 增加转交更新、按业务关闭待办；`LeaveApplicationMapper`/`AttendanceRecordMapper`/`SalaryRecordMapper` 补方法。
- **Service 层**：`LeaveService` 重构为完整审批闭环；新增 `SupplementaryCardService`、`SalaryBatchApprovalService`（或并入 SalaryBatchService）、`ApprovalDelegationService`；`ApprovalStateMachineService` 增加直接上级解析、委托改派、提交人通知修正、代审留痕；`ApprovalWorkbenchService` 增加统一详情与字段补全。
- **Controller 层**：新增 `SupplementaryCardController`；`LeaveController` 增加 submit/approve/cancel 端点并修正权限码；`ApprovalWorkbenchController` 增加详情/转交/委托端点并改用 `approval:*` 权限码；`SalaryController` 增加批次提交/审批端点。
- **数据库**：新增 `sql/phase10-approval-center.sql` 增量脚本（三张新表 + approval_record/salary_record/leave_application 增列 + 审批模板种子 + 权限种子）。
- **权限**：`LeaveController` 对齐 `att:leave:*`；新增 `att:card:apply/approve` 并分配给员工/主管/HR；工作台改用 `approval:workbench/view/approve`；ROLE_FINANCE 补 approval 权限树。遵循 CLAUDE.md「每完成一个模块检查权限管理是否完成」。
- **非目标（Non-goals）**：前端页面；薪资批次「老板」二审（系统无老板角色，模板预留）；加班审批流（8.1 未列出，种子权限已有 `att:overtime:approve`，后续单独立项）；委托生效前已存在的待办任务批量转移（需求仅要求委托期间新产生的任务自动改派）。
