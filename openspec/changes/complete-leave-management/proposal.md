## Why

需求文档第 6.3 大点定义了「请假管理」：七类假期、假期余额计算（年假/调休公式）、请假申请（含时段、系统计天数、附件证明）与 6.3.4 分级审批流。经代码审计，**审批闭环与余额占用/回补已随 `complete-approval-center` 落地并测试通过**（提交/审批/取消、6.3.4 分级规则、婚产丧假 HR 备案），但与需求相比仍存在以下功能性缺口：

- **附件证明材料完全缺失（6.3.1 + 6.3.3）**：病假 >1 天需医院证明、婚假需结婚证复印件、产假需医院证明，且 6.3.3 规定附件「条件必填」。当前无任何附件字段、表或上传机制（项目已有 `AliOssUtil`/`AliOssProperties`/`sky.alioss` 配置，但从未装配为 Bean、无任何调用）。
- **请假天数非系统计算、无时段（6.3.3）**：`LeaveApplyDTO.days` 由客户端直接传入并信任，服务端不核算；`leave_application` 只有 `start_date`/`end_date`（DATE），无「上午/下午」时段字段，无法表达「周一下午至周三上午」；也未按 6.1.2 排除周末与法定节假日。
- **工作日/节假日未配置（6.1.2 依赖）**：全库无节假日/调班配置表，无法判定工作日，请假天数与 6.4 应出勤天数均无计算依据。
- **年假不会自动发放（6.3.2）**：`calculateAnnualLeaveDays` 计算正确（经用户确认维持「满 1 年才开始享」，首年 0 天），但余额只能靠 HR 手动调 `POST /leave/balance/annual/init`；无年度定时发放、入职生效时也不会自动建余额行。
- **调休完全未落地（6.3.2）**：`OvertimeRecordMapper.selectUnconvertedByEmployee`/`markAsConverted` 与 `LeaveService.calculateCompensatoryDays` 均闲置（仅测试引用），加班时长从未转换为可调休余额；「当月及次月有效、过期清零」的有效期机制整体缺失。
- **请假统计缺失（6.4 请假部分）**：无个人请假汇总、部门请假率、请假类型分布接口，前端请假看板（饼图/部门对比）无数据来源。

## What Changes

补齐请假管理后端缺失能力，使代码与需求第 6.3 大点（含 6.1.2、6.4 请假部分、9.3）吻合（**仅后端，前端后期再做**）：

- **工作日历（work-calendar）**：新增 `work_calendar` 表存储法定节假日与调班工作日（默认周一至周五为工作日、周六日休息）；提供 `isWorkday`/`workdaysBetween` 判定服务与 HR 配置接口，作为请假天数与应出勤天数计算的唯一依据。
- **请假天数系统计算（leave-days-calculation）**：`leave_application` 增加 `start_period`/`end_period`（上午/下午）字段；新增 `LeaveDayCalculator` 按工作日历逐日累加、边界日按时段折半（0.5 天）；`apply` 改为服务端权威计算天数（不再信任客户端 `days`），并提供天数试算预览接口。
- **请假附件（leave-attachment）**：装配 `AliOssUtil` 为 Spring Bean（复用 `sky.alioss` 配置），新增 `leave_attachment` 表与文件上传/下载接口；`LeaveApplyDTO` 携带 `attachmentIds` 绑定申请；**提交时强制校验**——病假 >1 天、婚假、产假必须至少 1 个附件。
- **年假自动发放（annual-leave-accrual）**：新增定时任务每年 1 月 1 日为全体在职员工按 `calculateAnnualLeaveDays` 初始化当年年假余额（幂等 upsert）；入职生效时自动建年假与调休余额行；保留手动初始化接口作为补偿通道。
- **调休落地（comp-leave-management）**：新增 `comp_leave_grant` 调休入账明细表（记录加班所属月、折算天数、过期日）；定时/手动将未转换加班时长按 8 小时=1 天折算入账（余数滚动累积），过期日为加班次月月末；请假消耗按过期日近者优先（FIFO）扣减；定时任务对过期未用完部分执行清零。
- **请假统计（leave-statistics）**：新增个人请假汇总（分类型天数 + 年假余额）、部门请假率（请假天数/应出勤天数）、请假类型分布接口；数据权限按 RBAC 控制（主管仅本部门、HR 全量）。
- **审批流模型扩展（leave-approval-flow 修改）**：申请/提交接入天数系统计算与附件条件必填校验，审批详情可查看附件列表。

## Capabilities

### New Capabilities

- `work-calendar`: 工作日/法定节假日与调班配置，提供工作日判定服务（支撑请假天数与应出勤天数计算）。
- `leave-days-calculation`: 请假天数系统计算 —— 时段（上午/下午）字段、按工作日历排除非工作日、边界日折半、服务端权威计算与试算预览。
- `leave-attachment`: 请假附件证明 —— OSS 上传/下载、`leave_attachment` 元数据、提交时按类型条件必填校验。
- `annual-leave-accrual`: 年假年度自动发放（定时任务）与入职自动初始化余额。
- `comp-leave-management`: 调休余额落地 —— 加班时长折算入账、当月及次月有效期、FIFO 消耗、过期清零。
- `leave-statistics`: 请假统计 —— 个人汇总、部门请假率、请假类型分布，含数据权限。

### Modified Capabilities

- `leave-approval-flow`: 申请模型扩展时段与附件；天数改为系统计算；提交增加附件条件必填校验。

## Impact

- **数据库**：新增增量脚本 `sql/phase11-leave-management.sql` —— 新建 `work_calendar`、`leave_attachment`、`comp_leave_grant`、`comp_leave_usage` 四表；`leave_application` 增列 `start_period`/`end_period`；新增权限码种子并分配角色。
- **Entity 层**：新增 `WorkCalendar`、`LeaveAttachment`、`CompLeaveGrant`、`CompLeaveUsage`；`LeaveApplication` 增加 `startPeriod`/`endPeriod`。
- **DTO/VO**：`LeaveApplyDTO` 增加 `startPeriod`/`endPeriod`/`attachmentIds`（`days` 改为服务端计算，不再必填）；新增请假统计 VO、附件 VO。
- **Mapper 层**：新增三张表 Mapper + XML；`LeaveApplicationMapper` 补按部门/类型/日期范围聚合统计查询；`LeaveBalanceMapper`、`OvertimeRecordMapper` 补方法。
- **Service 层**：新增 `WorkCalendarService`、`LeaveDayCalculator`、`LeaveAttachmentService`（含 `FileStorageService`/OSS 装配）、`CompLeaveService`、`LeaveStatisticsService`、`AnnualLeaveAccrualTask`、`CompLeaveExpiryTask`；`LeaveService.apply/submit` 改造接入天数计算与附件校验。
- **Controller 层**：新增 `WorkCalendarController`（HR 配置）、`LeaveAttachmentController`（上传/下载）、`LeaveStatisticsController`；`LeaveController` 增加天数试算端点。
- **定时任务**：新增年假年度发放（1 月 1 日）、调休过期清零（每日/每月）两个 `@Scheduled` 任务，挂入现有调度体系（参考 `LifecycleScheduler`）。
- **配置/依赖**：装配 `AliOssUtil` Bean（复用现有 `sky.alioss` 配置与 `aliyun-sdk-oss` 依赖，无新增第三方库）；附件类型/大小限制、OSS objectKey 规则集中配置。
- **权限**：新增 `att:calendar:manage`（HR）等工作日历权限码并分配；附件上传/统计接口复用 `att:leave:apply/view` 并按角色做数据范围控制；手动发放接口复用 `att:record:manage`。遵循 CLAUDE.md「每完成一个模块检查权限管理是否完成」。
- **非目标（Non-goals）**：前端页面；加班申请/审批流（`att:overtime:approve` 权限种子已有，流程后续单独立项，本期调休以 `overtime_record` 已存在数据为转换来源）；考勤迟到/早退/旷工统计与打卡日历（属考勤模块 6.2/6.4 非请假部分）；年假跨年结转（需求未提及，法定年假当年有效）；调休按小时粒度存储（维持现有「8 小时=1 天、天数」模型，与既有测试一致）。
