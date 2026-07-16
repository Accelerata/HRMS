# Tasks: 请假管理补全

## 1. 数据库脚本（phase11-leave-management.sql）

- [ ] 1.1 新建 `work_calendar` 表（uk_calendar_date 唯一，day_type 1-休息/2-调班工作日）
- [ ] 1.2 新建 `leave_attachment` 表（idx_application_id）
- [ ] 1.3 新建 `comp_leave_grant`（idx_employee_status_expire）与 `comp_leave_usage`（idx_application_id/grant_id）表
- [ ] 1.4 `leave_application` 增列 `start_period`、`end_period`（TINYINT，0-上午 1-下午）
- [ ] 1.5 权限码种子：新增 `att:calendar`(菜单)、`att:calendar:manage`，分配 ROLE_ADMIN/ROLE_HR；确认 `att:leave:apply/view`、`att:record:manage` 已覆盖附件与统计接口

## 2. 工作日历（work-calendar）

- [ ] 2.1 新增 `WorkCalendar` 实体 + Mapper + XML（按日期查、按年查、插入、删除、唯一冲突更新）
- [ ] 2.2 `WorkCalendarService.isWorkday(date)`：命中日历记录按 day_type，否则周六日休息、其余工作日
- [ ] 2.3 `WorkCalendarService.workdaysBetween(start,end)`：收集区间内工作日列表
- [ ] 2.4 节假日配置：按年保存（批量 upsert）/删除/列表；`WorkCalendarController`（HR，`att:calendar:manage`）

## 3. 请假天数系统计算（leave-days-calculation）

- [ ] 3.1 `LeaveApplication` 增加 `startPeriod`/`endPeriod`；`LeaveApplicationMapper.xml` 结果映射与 insert 同步
- [ ] 3.2 新增 `LeaveDayCalculator.calculate(startDate,startPeriod,endDate,endPeriod)`：工作日逐日累加 + 边界日按时段折半（0.5）
- [ ] 3.3 `LeaveApplyDTO` 增加 `startPeriod`/`endPeriod`/`attachmentIds`；`days` 改非必填；时段值校验（0/1）
- [ ] 3.4 `LeaveService.apply` 改用 `LeaveDayCalculator` 服务端计算天数并覆盖客户端值；区间无工作日/结束早于开始 → 400
- [ ] 3.5 `LeaveController` 新增 `GET /leave/days/calculate` 试算预览端点（`att:leave:apply`）

## 4. 请假附件（leave-attachment）

- [ ] 4.1 新增 `OssConfiguration` 装配 `AliOssUtil` Bean（注入 `AliOssProperties`）
- [ ] 4.2 新增 `FileStorageService.upload(MultipartFile)`：校验扩展名(jpg/jpeg/png/pdf)与大小(≤10MB 可配)，objectKey=`leave/{yyyyMM}/{uuid}.{ext}`，返回 URL
- [ ] 4.3 新增 `LeaveAttachment` 实体 + Mapper + XML（插入、按 applicationId 查、按 id 查、绑定 applicationId）
- [ ] 4.4 `LeaveAttachmentService.upload`：落 `leave_attachment`（application_id 暂 NULL）；`bindToApplication` 回写 application_id 且校验归属本人
- [ ] 4.5 `LeaveAttachmentController`：`POST /leave/attachments`（multipart 上传）、`GET /leave/{id}/attachments`（本人/审批人/HR 查看）
- [ ] 4.6 `LeaveService.apply` 绑定 `attachmentIds`；`submit` 增加条件必填校验：病假>1 天/婚假/产假无附件 → 400

## 5. 年假自动发放（annual-leave-accrual）

- [ ] 5.1 新增 `AnnualLeaveAccrualTask`（`@Scheduled` 每年 1 月 1 日 02:00）：遍历在职员工调用 `initAnnualLeaveBalance(empId, entryDate, currentYear)`（幂等）
- [ ] 5.2 `LeaveService.initNewEmployeeBalances(employeeId, entryDate, year)`：建年假（首年 0）+ 调休（0）余额行；入职生效处调用
- [ ] 5.3 保留并复用 `POST /leave/balance/annual/init` 作为 HR 手动补偿通道（`att:record:manage`）

## 6. 调休落地（comp-leave-management）

- [ ] 6.1 新增 `CompLeaveGrant`、`CompLeaveUsage` 实体 + Mapper + XML（入账、按员工+有效+expire 升序查、过期查询、used 更新、usage 插入/按申请查）
- [ ] 6.2 `CompLeaveService.convertOvertime(employeeId)`：累计未转换加班时长 floor(/8)=days，插 grant（expire=加班次月月末）、累加调休余额、markAsConverted（幂等）
- [ ] 6.3 定时任务每月 1 日批量对全体员工执行 convertOvertime；保留 HR 手动触发端点（`att:record:manage`）
- [ ] 6.4 `LeaveService.deductBalance` 调休分支改为按 expire_date 升序 FIFO 逐笔扣 grant.used_days + 写 comp_leave_usage + 扣 leave_balance
- [ ] 6.5 `restoreBalance` 调休分支按 comp_leave_usage 逆向回填 grant.used_days + 回补 leave_balance
- [ ] 6.6 新增 `CompLeaveExpiryTask`（`@Scheduled` 每日 03:00）：expire_date<今天且未用完的 grant 置 status=0，差额从 leave_balance.remaining_days 扣减（下限 0）

## 7. 请假统计（leave-statistics）

- [ ] 7.1 `LeaveApplicationMapper` 补聚合查询：按 status=2 且与当月相交，GROUP BY 类型 / JOIN employee GROUP BY 部门
- [ ] 7.2 新增统计 VO（分类型天数、请假率、类型分布）
- [ ] 7.3 `LeaveStatisticsService`：personal（分类型天数+年假余额）、dept（请假率=请假天数/(在职人数×当月工作日数)）、type-distribution
- [ ] 7.4 `LeaveStatisticsController`：3 个统计端点（`att:leave:view`）；Service 层数据范围控制（员工仅本人、主管仅本部门、HR/管理员全量）

## 8. 测试与验证

- [ ] 8.1 `LeaveDayCalculator` 单测：跨周末/节假日、时段折半、同日半天、无工作日
- [ ] 8.2 附件校验测试：病假>1 天无附件提交失败、婚/产假无附件失败、病假≤1 天免传
- [ ] 8.3 调休测试：折算入账（含余数滚动）、FIFO 消耗、过期清零、拒绝回补精确
- [ ] 8.4 年假发放任务幂等测试；入职初始化建行测试
- [ ] 8.5 统计接口测试：个人汇总、部门请假率、类型分布、数据范围越权防护
- [ ] 8.6 全量 `mvn test` 通过；核对权限种子与控制器权限码一致（CLAUDE.md 权限检查）
