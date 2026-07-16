# comp-leave-management

## Purpose

落地加班→调休的全流程：加班时长按 8 小时=1 天折算入账，入账带有效期限（加班当月+次月），消耗时按过期日 FIFO 扣减，到期未用完自动清零。通过明细表（`comp_leave_grant`/`comp_leave_usage`）承载有效期与回补精确性。

## Requirements

### Requirement: 加班时长折算调休入账

系统 SHALL 将员工未转换的加班时长按 8 小时 = 1 天折算为调休天数，写入 `comp_leave_grant` 入账明细（记录加班所属月与过期日），并同步累加 `leave_balance` 调休行。不足 8 小时的余数 MUST 保持未转换状态滚动至下一折算周期（不丢弃）。折算 MUST 幂等：已标记转换（`converted_to_comp=1`）的加班记录不得重复折算。

#### Scenario: 加班 20 小时折算 2 天

- **WHEN** 某员工 3 月累计未转换加班 20 小时，执行折算
- **THEN** 入账 2 天调休（floor(20/8)），剩余 4 小时保持未转换滚动，调休余额 +2

#### Scenario: 折算幂等

- **WHEN** 对同一批已转换加班记录再次执行折算
- **THEN** 不重复入账、调休余额不变

### Requirement: 调休有效期

每笔调休入账 MUST 设定过期日为加班当月次月月末（加班当月及次月有效）。消耗调休时系统 MUST 按过期日近者优先（FIFO）从有效 `comp_leave_grant` 逐笔扣减，并记录 `comp_leave_usage` 占用明细。

#### Scenario: 按过期日近者优先消耗

- **WHEN** 员工有两笔有效调休入账（expire 分别 4 月 30 日、5 月 31 日），请 1 天调休
- **THEN** 优先从 4 月 30 日到期的入账扣减，并写入一条 comp_leave_usage

### Requirement: 调休过期清零

系统 SHALL 通过定时任务对已过过期日且未用完的调休入账执行清零：置入账状态为已过期，并将未用差额从 `leave_balance` 调休行剩余天数中扣减（扣减后不得低于 0）。

#### Scenario: 过期未用完清零

- **WHEN** 某笔调休入账 days=2、已用 0.5、过期日已过
- **THEN** 该入账置为已过期，调休余额 remaining 扣减 1.5（不低于 0）

### Requirement: 调休拒绝/取消精确回补

调休请假被拒绝或取消时，系统 MUST 依据 `comp_leave_usage` 按原占用路径逆向回填各入账的已用天数，并回补 `leave_balance` 调休行。

#### Scenario: 取消调休回补原入账

- **WHEN** 员工取消一笔占用两笔入账（各 0.5 天）的调休申请
- **THEN** 两笔入账 used_days 各回减 0.5，调休余额相应回补

### Requirement: 调休折算触发

系统 SHALL 每月 1 日定时对全体员工执行加班折算入账；HR 亦可通过接口手动触发指定员工折算作为补偿。

#### Scenario: 月度批量折算

- **WHEN** 每月 1 日折算任务运行
- **THEN** 全体存在未转换加班的员工完成当月折算入账
