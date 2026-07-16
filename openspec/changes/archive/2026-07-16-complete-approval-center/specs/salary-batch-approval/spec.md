# salary-batch-approval

## ADDED Requirements

### Requirement: 薪资批次生成

系统 SHALL 为每次批量核算创建（或复用同学年月未确认的）薪资批次，批次记录年份、月份、核算人数、实发合计、状态与提交人；批次内 `salary_record` MUST 挂接 `batch_id`。同一学年月只允许一个未拒绝的批次（唯一约束）。

#### Scenario: 批量核算生成批次

- **WHEN** HR 对 2026 年 6 月执行批量核算，共 50 名在职员工核算成功
- **THEN** 系统创建 2026-06 批次（状态 DRAFT、人数 50、实发合计汇总），批次内 50 条薪资记录的 batch_id 指向该批次

#### Scenario: 同学年月重复核算复用批次

- **WHEN** 2026-06 批次仍为 DRAFT 状态时 HR 再次批量核算
- **THEN** 复用原批次，批次内记录更新、人数与合计刷新，不新建批次

### Requirement: 批次提交审批

HR SHALL 能将 DRAFT/REJECTED 状态的批次提交审批。提交后批次置 PENDING，系统生成财务专员审批待办并通知。PENDING 或 APPROVED 状态的批次 MUST NOT 重复提交。

#### Scenario: 提交批次审批

- **WHEN** HR 提交 2026-06 DRAFT 批次
- **THEN** 批次置 PENDING，财务专员收到审批待办通知

#### Scenario: 重复提交拒绝

- **WHEN**  HR 对已 PENDING 的批次再次提交
- **THEN** 系统拒绝并提示当前状态不可提交

### Requirement: 批次审批通过与拒绝

财务专员 SHALL 能审批薪资批次（通过/拒绝附意见）。通过后批次置 APPROVED，批次内全部薪资记录 MUST 批量置为 CONFIRMED，并通知提交人；拒绝后批次置 REJECTED，批次内记录回退 DRAFT，并通知提交人（含审批意见）。

#### Scenario: 财务通过批量确认

- **WHEN** 财务专员通过 2026-06 批次审批
- **THEN** 批次置 APPROVED，批次内 50 条薪资记录全部置为 CONFIRMED，HR 收到通过通知

#### Scenario: 财务拒绝整批退回

- **WHEN** 财务专员拒绝 2026-06 批次并填写意见
- **THEN** 批次置 REJECTED，批次内记录回退 DRAFT，HR 收到含意见的拒绝通知
