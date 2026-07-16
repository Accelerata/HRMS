# work-calendar

## Purpose

提供统一的工作日判定服务，作为请假天数与应出勤天数计算的唯一依据。采用「异常表」模式——默认周一至周五为工作日、周六日休息，仅存储法定节假日与调班工作日例外。

## Requirements

### Requirement: 工作日判定

系统 SHALL 提供统一的工作日判定服务，作为请假天数与应出勤天数计算的唯一依据。默认周一至周五为工作日、周六周日为休息日；`work_calendar` 表中配置的法定节假日视为非工作日、调班工作日视为工作日。表内记录的优先级 MUST 高于星期默认规则。

#### Scenario: 默认工作日与休息日

- **WHEN** 判定一个未在 `work_calendar` 配置的周三与周六
- **THEN** 周三判定为工作日，周六判定为非工作日

#### Scenario: 法定节假日排除

- **WHEN** 某周五在 `work_calendar` 配置为 `day_type=1`（国庆节）
- **THEN** 该周五判定为非工作日

#### Scenario: 调班工作日

- **WHEN** 某周六在 `work_calendar` 配置为 `day_type=2`（调班上班）
- **THEN** 该周六判定为工作日

### Requirement: 工作日区间计算

系统 SHALL 提供 `workdaysBetween(start, end)` 返回闭区间内所有工作日，供请假天数与月度应出勤天数统计共用。

#### Scenario: 跨周末区间

- **WHEN** 计算周五至下周一（期间无节假日）的工作日
- **THEN** 返回周五与下周一共 2 个工作日

### Requirement: 节假日配置

HR SHALL 能按年维护节假日与调班（新增/批量保存/删除/列表）。同一日期重复配置 MUST 按唯一约束更新而非产生重复行。非 HR 角色 MUST NOT 调用配置接口。

#### Scenario: HR 批量导入年度节假日

- **WHEN** HR 提交 2026 年法定节假日列表
- **THEN** 系统批量写入 `work_calendar`，重复日期自动更新

#### Scenario: 非 HR 配置被拒绝

- **WHEN** 普通员工调用节假日配置接口
- **THEN** 系统拒绝（无 `att:calendar:manage` 权限）
