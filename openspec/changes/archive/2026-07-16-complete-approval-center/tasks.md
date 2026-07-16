# Tasks: 审批中心补全

## 1. 审批引擎扩展（approval-engine-enhancement）

- [x] 1.1 `BusinessTypeEnum` 增加 LEAVE(6,"请假")、CARD(7,"补卡")，SALARY(5) label 调整为「薪资批次」
- [x] 1.2 `ApproverTargetEnum` 增加 DIRECT_SUPERVISOR("direct_supervisor","直接上级")
- [x] 1.3 `ApprovalRecord` 实体增加 `originalApproverId`、`originalApproverName`、`assignType`；`approval_record` 增列（phase10 SQL）
- [x] 1.4 `ApprovalContext` 增加 `submitterUserId`、`leaveNeedDeptManager` 及对应工厂方法；`startApproval` 增加 `leaveNeedDeptManager` 条件步骤过滤
- [x] 1.5 `resolveApprover` 增加 `direct_supervisor` 解析：employee.report_to → 回退 dept_manager → 回退 hr_specialist 兜底（warn 日志，不静默丢步骤）
- [x] 1.6 `notifySubmitter` 修正：优先使用 context.submitterUserId（经 startApproval 暂存可查询），为空回退旧逻辑；四个已有业务 Service 调用处补传 submitter
- [x] 1.7 `startApproval` 接入委托自动改派：命中生效委托则改写 approver + 留存 original + assignType=2
- [x] 1.8 `processApproval` 对 assignType≠0 记录自动附加「XXX 代 YYY 审批」意见前缀

## 2. 请假审批流（leave-approval-flow）

- [x] 2.1 `LeaveApplication` 增加 `handoverTo` 字段与「4-已取消」状态；`leave_application` 增列 handover_to（phase10 SQL）
- [x] 2.2 `LeaveApplyDTO` 增加 handoverTo；`LeaveService.apply` 改为仅校验余额不扣减（占用移至提交）
- [x] 2.3 审批模板种子：(6,1,'direct_supervisor','直接上级审批') + (6,2,'dept_manager','部门负责人审批',condition_expr='leaveNeedDeptManager')
- [x] 2.4 `LeaveService.submit`：状态校验（草稿）、余额占用、计算 leaveNeedDeptManager（年假/调休>3天 或 事假/病假>1天）、startApproval(6)
- [x] 2.5 `LeaveService.approve`：走 stateMachine.processApproval，全部通过置已通过+通知员工；拒绝置已拒绝+`restoreBalance` 回补+通知；婚/产/丧假通过后向 HR 发送备案通知
- [x] 2.6 `LeaveService.cancel`：仅本人+审批中+无已处理审批；置已取消+回补余额+关闭待办审批记录（ApprovalRecordMapper 新增 closePendingByBusiness）
- [x] 2.7 `LeaveController` 新增 submit/approve/cancel 端点；权限码对齐种子 `att:leave:apply/view/approve`（balance:init 用 `att:record:manage`）
- [x] 2.8 `LeaveApplicationMapper` 补 selectById/updateStatus 方法（如缺）

## 3. 补卡审批（supplementary-card-approval）

- [x] 3.1 新建 `supplementary_card_application` 表（phase10 SQL）：employee_id、attendance_date、card_type(1上班/2下班)、supplement_time、reason、status(0草稿/1审批中/2已通过/3已拒绝)
- [x] 3.2 新增 `SupplementaryCardApplication` 实体 + Mapper + XML
- [x] 3.3 审批模板种子：(7,1,'direct_supervisor','直接上级审批')
- [x] 3.4 `SupplementaryCardService.apply`：校验该日期卡型为 MISSING_PUNCH、无重复申请（草稿/审批中/已通过）；创建申请（直接审批中）+ startApproval(7)
- [x] 3.5 `SupplementaryCardService.approve`：通过→回写 attendance_record（打卡时间=补卡时间、状态=NORMAL）+通知；拒绝→置已拒绝+通知
- [x] 3.6 新增 `SupplementaryCardController`（apply/approve/my-list）；权限码种子 `att:card:apply`（员工）、`att:card:approve`（主管/HR）

## 4. 薪资批次审批（salary-batch-approval）

- [x] 4.1 新建 `salary_batch` 表（phase10 SQL，uk_year_month 唯一）；`salary_record` 增列 batch_id
- [x] 4.2 新增 `SalaryBatch` 实体 + Mapper + XML
- [x] 4.3 `SalaryBatchService.batchCalculate` 改造：创建/复用批次、记录挂 batch_id、刷新人数与实发合计
- [x] 4.4 审批模板种子：(5,1,'finance_specialist','财务专员审批')
- [x] 4.5 批次提交端点 `POST /salary/batches/{id}/submit`：状态校验（DRAFT/REJECTED）→ PENDING + startApproval(5, submitter=HR)
- [x] 4.6 批次审批端点：通过→批次 APPROVED + 批次内记录批量 CONFIRMED + 通知提交人；拒绝→REJECTED + 记录回退 DRAFT + 通知
- [x] 4.7 批次查询端点（列表/详情含批次内记录汇总）

## 5. 转交与委托（approval-task-assignment）

- [x] 5.1 新建 `approval_delegation` 表（phase10 SQL）：delegator_id/name、delegate_id/name、start_time、end_time、status(0已取消/1生效)
- [x] 5.2 新增 `ApprovalDelegation` 实体 + Mapper + XML（查询某审批人生效中委托、重叠校验查询）
- [x] 5.3 `ApprovalDelegationService`：设置委托（校验被委托人非本人/存在、时间合法、区间不重叠）、取消（仅本人）、查询我的委托
- [x] 5.4 转交端点 `POST /approvals/records/{id}/transfer`：校验本人待审记录、目标非本人；更新 approver + 留存 original + assignType=1 + 通知新审批人（ApprovalRecordMapper 新增 reassign）
- [x] 5.5 委托端点：`POST /approvals/delegations`、`DELETE /approvals/delegations/{id}`、`GET /approvals/delegations/my`

## 6. 工作台增强（approval-workbench）

- [x] 6.1 `ApprovalTodoVO` 增加 `dueTime`；`getTodoList` 填充 applicantName（按业务类型查申请人）、stepName（模板）、dueTime
- [x] 6.2 统一详情接口 `GET /approvals/detail/{businessType}/{businessId}`：返回业务数据（按类型组装 Map/VO）、审批历史列表（含代审留痕、dueTime）、当前用户可操作记录 ID
- [x] 6.3 `ApprovalWorkbenchController` 权限码改用 `approval:workbench`（列表/详情/转交/委托）；审批操作仍走各业务 Controller
- [x] 6.4 `ApprovalDoneVO` 增加 dueTime/stepName 展示字段（如有需要）并填充

## 7. SQL 与权限种子（phase10-approval-center.sql）

- [x] 7.1 DDL：approval_record 增列（original_approver_id/name、assign_type）；leave_application 增列 handover_to；salary_record 增列 batch_id；新建 supplementary_card_application、salary_batch、approval_delegation 三表
- [x] 7.2 审批模板种子：请假(6) 两行、补卡(7) 一行、薪资批次(5) 一行
- [x] 7.3 权限种子：`att:card:apply`、`att:card:approve` 插入并分配（员工/主管/HR）；ROLE_FINANCE 补 approval(55-58) 权限树
- [x] 7.4 核对 `sys_role_permission`：主管具备 att:leave:approve + att:card:approve + approval:*；HR 具备全部；员工具备 att:leave:apply + att:card:apply

## 8. 测试与验证

- [x] 8.1 请假审批规则矩阵单测（7 类假期 × 天数边界 → 审批级数）；余额占用/回补/取消回补
- [x] 8.2 委托单测：设置/重叠拒绝/自动改派/取消恢复；代审意见前缀
- [x] 8.3 转交单测：正常转交/给自己拒绝/非本人拒绝/已处理拒绝
- [x] 8.4 薪资批次单测：批次生成复用、提交状态机、通过批量 CONFIRMED、拒绝回退 DRAFT
- [x] 8.5 补卡单测：缺卡校验、重复申请拒绝、通过回写考勤
- [x] 8.6 `mvn test` 全量通过；`openspec validate complete-approval-center --strict` 通过
- [x] 8.7 权限自查：按 CLAUDE.md 核对本模块全部端点的权限码在种子中已定义且分配正确
