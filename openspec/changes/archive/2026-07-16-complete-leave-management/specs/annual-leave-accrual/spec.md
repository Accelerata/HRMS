# annual-leave-accrual

## ADDED Requirements

### Requirement: 年假年度自动发放

系统 SHALL 通过定时任务在每年 1 月 1 日为全体在职员工初始化当年年假余额，年假天数按既有 `calculateAnnualLeaveDays`（入职满 1 年 5 天、满 10 年 10 天、满 20 年 15 天，首年 0 天）计算。发放 MUST 幂等：同一员工同一年重复执行不得产生重复余额行或重复累加（依赖 `uk_employee_leave_year` 唯一约束与 upsert 逻辑）。

#### Scenario: 年度发放创建新行

- **WHEN** 定时任务在 2027 年 1 月 1 日运行，某入职满 3 年的在职员工无 2027 年年假余额行
- **THEN** 系统为其创建 2027 年年假余额行，total=5、used=0、remaining=5

#### Scenario: 重复发放幂等

- **WHEN** 定时任务在同一年被重复触发
- **THEN** 同一员工同一年的年假余额行不重复创建、used/remaining 不被重复累加

#### Scenario: 离职员工不发放

- **WHEN** 年度发放任务遍历到已离职员工
- **THEN** 不为其创建当年年假余额

### Requirement: 入职自动初始化余额

新员工入职生效时，系统 SHALL 自动为其创建当年年假余额行（首年 0 天）与调休余额行（0 天），保证员工立即可查询余额。

#### Scenario: 新员工入职建行

- **WHEN** 某员工入职流程生效、员工档案激活
- **THEN** 系统创建其当年年假（0 天）与调休（0 天）余额行

### Requirement: 手动补偿通道

HR SHALL 能通过接口手动为指定员工初始化/重算某年年度年假余额，作为定时任务的补偿通道。

#### Scenario: HR 手动补发

- **WHEN** HR 对某员工手动触发当年年假初始化
- **THEN** 系统按入职年限重算并 upsert 该员工当年年假余额
