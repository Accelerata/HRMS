# Design: 审批中心补全

## 总体架构

沿用现有「模板驱动 + 顺序门控」审批引擎（`ApprovalStateMachineService`），新业务类型通过审批模板种子接入，不引入新的流程框架。改动集中在五处：引擎扩展（直接上级/委托改派/提交人通知/代审留痕）、三个新业务流（请假/补卡/薪资批次）、任务改派（转交/委托）、工作台增强。

## 关键决策

### D1: 请假分级审批用「模板 + 条件表达式」而非硬编码步骤生成

6.3.4 的审批层级由「假期类型 + 天数」动态决定。现有引擎已支持 `condition_expr`（needHr / hasSalaryAdjust 硬编码匹配）。沿用该模式：

```sql
-- business_type=6 请假
(6, 1, 'direct_supervisor', '直接上级审批', NULL),
(6, 2, 'dept_manager',      '部门负责人审批', 'leaveNeedDeptManager');
```

`LeaveService.submit` 计算 `leaveNeedDeptManager = (年假||调休 && days>3) || (事假||病假 && days>1)` 写入 `ApprovalContext`，引擎过滤条件步骤。婚/产/丧假不走第二级——「HR 备案」是通知而非审批节点，在审批全部通过时由 `LeaveService` 向 HR 发送备案通知（type=2 站内信）。

- **备选方案**：每类假期一组模板行（7 类 × 2 级）。否决：模板表爆炸且天数阈值（3天/1天）无法表达。

### D2: 余额「提交时占用、拒绝/取消回补」

现状 `apply` 即扣减余额，拒绝后余额丢失。改为：

- `apply`（草稿）：仅校验余额充足，不扣减。
- `submit`：重新校验并扣减（占用）。
- 审批拒绝 / 申请人取消：`restoreBalance`（usedDays -= days, remainingDays += days）。

并发安全：扣减与回补均在 `@Transactional` 内走 `LeaveBalanceMapper.update`，余额校验与更新同事务。

### D3: 委托改派在 `startApproval` 生成记录时生效

需求 8.3：「委托期间产生的审批任务自动转给被委托人」。在 `startApproval` 解析出审批人后、插入记录前，查询 `approval_delegation` 中该审批人生效中的委托（status=1 且当前时间在起止区间内）：

- 命中：记录 `original_approver_id/name` = 原审批人，`approver_id/name` 改写为被委托人，`assign_type=2`，待办通知发给被委托人。
- 未命中：`assign_type=0`，正常分配。

委托只对**新产生**的任务生效；委托生效前已存在的待办不转移（需求未要求，列入 Non-goals）。同一用户同一时刻只允许一条生效委托（部分唯一索引或应用层校验）。

### D4: 转交与委托共用「代审留痕」字段

`approval_record` 增加三列：

| 列 | 含义 |
|---|---|
| `original_approver_id` / `original_approver_name` | 改派前审批人（转交/委托时为 NULL 表示正常分配） |
| `assign_type` | 0-正常分配 1-转交 2-委托 |

`processApproval` 在 `assign_type != 0` 时，将审批意见存储为 `{现审批人} 代 {原审批人} 审批：{意见}`（若意见为空则只留代审前缀），满足 8.3「系统记录 XXX 代 YYY 审批」。审批历史展示天然兼容。

- **转交**（assign_type=1）：一次性动作，`POST /approvals/records/{id}/transfer` 校验当前用户是该记录审批人且待审，改派给目标用户并通知。不校验目标用户角色（转交是审批人的自主授权行为，与 8.2.1「转交」语义一致），但禁止转交给自己。

### D5: 「直接上级」解析：report_to → 部门负责人回退

6.3.4 与 8.1（补卡）均以「直接上级」为第一审批人。`employee.report_to`（phase4 已建列）指向直属汇报人。解析链：

1. `employee.report_to` 非空且该员工有关联 `user_id` → 用其 user_id；
2. 否则回退 `dept_manager`（部门负责人）；
3. 仍解析失败 → 回退 HR 专员兜底并记录 warn（避免流程因数据缺失而静默丢失审批步骤——现状 `approverId == null` 时 skip 步骤，可能导致零级审批直接通过）。

### D6: 薪资批次：新表承载，记录挂接

```sql
CREATE TABLE `salary_batch` (
    `id` BIGINT PK, `year` INT, `month` INT,
    `status` VARCHAR(16) DEFAULT 'DRAFT',  -- DRAFT/PENDING/APPROVED/REJECTED
    `employee_count` INT, `total_net_pay` DECIMAL(14,2),
    `submitter_id` BIGINT, create_time/update_time,
    UNIQUE KEY `uk_year_month` (`year`,`month`)
);
ALTER TABLE `salary_record` ADD COLUMN `batch_id` BIGINT NULL;
```

- `batchCalculate`：同学年月的未确认批次存在时复用，否则创建批次；记录写入 `batch_id`。
- 提交：`POST /salary/batches/{id}/submit` → 批次 PENDING + `startApproval(5, batchId, ctx(submitter=HR))`，模板 `(5,1,'finance_specialist','财务专员审批')`。
- 审批通过：批次 APPROVED + 批次内记录批量 `DRAFT→CONFIRMED`；拒绝：批次 REJECTED + 记录回退 DRAFT 并通知提交人。
- 「老板」二审（8.1 方括号=可选）：系统无老板角色，本期不实现，模板表结构可后续直接加行。

### D7: 工作台权限码改用种子已有的 `approval:*`

种子（phase2）已定义 `approval:workbench`(56)/`approval:view`(57)/`approval:approve`(58) 并分配给 HR、MANAGER，但 `ApprovalWorkbenchController` 误用入转调离四个 manage 权限码。改为：

- 列表/详情：`approval:workbench` 或 `approval:view`
- 审批操作仍由各业务 Controller 的权限码控制（不变）
- 转交/委托端点：`approval:workbench`（审批人自助操作，任务归属校验在 Service 层做）

ROLE_FINANCE 补授 `approval`(55)+`approval:workbench`(56)+`approval:view`(57)+`approval:approve`(58)，使其能审批薪资批次。

### D8: LeaveController 权限码对齐种子

控制器 `leave:*` 与种子 `att:leave:*` 断链，非管理员全 403。采用改控制器（5 处注解值）而非改种子的方案：种子 `att:leave:*` 已正确分配给 EMPLOYEE/MANAGER/HR，改控制器是一行级最小 diff，与 phase9「种子服务于控制器」的先例不冲突（phase9 时种子根本没有对应码，只能新增；本次种子已有现成码）。

映射：`leave:apply→att:leave:apply`、`leave:balance:view→att:leave:view`、`leave:record:view→att:leave:view`、`leave:balance:init→att:record:manage`（HR 管理操作）、新增审批端点 `att:leave:approve`。补卡新增 `att:card:apply`(员工)/`att:card:approve`(主管/HR) 种子。

### D9: 提交人通知修正

`ApprovalContext` 增加 `submitterUserId`；`notifySubmitter` 优先用它定位提交人，为空时回退旧逻辑（第一审批人，兼容历史调用）。四个已有业务 Service 调用处补传提交人（onboarding.submitterId 等），请假/补卡传员工 user_id，薪资批次传批次 submitter_id。

### D10: 业务类型编码

`BusinessTypeEnum`：SALARY(5) 保留并复用于薪资批次（label 调整为「薪资批次」）；新增 LEAVE(6,"请假")、CARD(7,"补卡")。`approval_template.business_type` 注释同步更新。

## 数据流（以请假>3天为例）

1. 员工 `POST /leave/apply`（草稿）→ `POST /leave/{id}/submit`
2. `LeaveService` 占用余额，计算 leaveNeedDeptManager=true，`startApproval(6, id, ctx)` → 生成 2 条待办（直接上级、部门负责人），通知直接上级
3. 上级通过 → 引擎顺序门控放行第二级，通知部门负责人
4. 负责人通过 → 引擎判定全部通过 → 回调 `LeaveService.approve` 置申请已通过，通知员工
5. 任一拒绝 → 申请已拒绝 + 余额回补 + 通知员工

## 异常与边界

- 委托给自己 / 转交给自己 / 重复转交已处理记录：400。
- 同一 delegator 时间区间重叠的生效委托：400。
- 补卡申请日期无缺卡记录（punch 状态非 MISSING_PUNCH）：400；同人同日同卡型重复申请（草稿/审批中）：400。
- 薪资批次重复提交（同年月已有 APPROVED 批次）：400；批次 PENDING 时重复提交：400。
- 请假取消仅允许「审批中」且本人；草稿可直接删除（复用现有 mapper delete）。
- 引擎顺序门控、48h 超时催办对新业务类型自动生效（dueTime 在 startApproval 统一计算）。
