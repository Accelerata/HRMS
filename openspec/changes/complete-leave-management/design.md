# Design: 请假管理补全

## 总体架构

沿用现有「模板驱动审批引擎 + Service/Mapper 分层」结构，不引入新流程框架。请假核心闭环（提交/审批/取消/余额占用回补）保持不动，本次在外围补四块能力：工作日历（天数与出勤计算依据）、附件（OSS 存储）、年假/调休余额的自动化（定时任务驱动）、请假统计。新增逻辑集中于 Service 层与两个 `@Scheduled` 任务，对 `LeaveService` 的改动收敛在 `apply`/`submit` 两个入口。

## 关键决策

### D1: 工作日历用「异常表」而非全量日历

6.1.2：标准工作日周一至周五、休息日周六日、法定节假日需提前配置自动排除。若存全量 365 天日历则维护成本高。改为只存**例外**：

```sql
CREATE TABLE `work_calendar` (
    `id`            BIGINT      NOT NULL AUTO_INCREMENT,
    `calendar_date` DATE        NOT NULL,
    `day_type`      TINYINT     NOT NULL  COMMENT '1-法定节假日/休息 2-调班工作日',
    `name`          VARCHAR(64) DEFAULT NULL COMMENT '节日/调班说明',
    `year`          INT         NOT NULL,
    `create_time`   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`   DATETIME    DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_calendar_date` (`calendar_date`),
    KEY `idx_year` (`year`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工作日历（法定节假日/调班）';
```

`WorkCalendarService.isWorkday(date)` 判定顺序：

1. 命中 `work_calendar` 记录 → 按 `day_type`（1=休息，2=工作日）返回；
2. 否则周六/周日 → 非工作日；
3. 其余 → 工作日。

`day_type=2`（调班工作日）用于支持「法定调休后周六上班」这类场景。`workdaysBetween(start, end)` 逐日调用 `isWorkday` 收集工作日，供请假天数与应出勤天数共用。HR 通过接口按年维护节假日（增/删/列表），可批量导入。

- **备选方案**：全量日历表。否决：每年需批量生成且 99% 行是默认规则，冗余。

### D2: 请假天数服务端权威计算，时段折半

6.3.3：开始/结束时间含「日期+时段（上午/下午）」，请假天数系统计算、支持 0.5 天。现状客户端直接传 `days` 且服务端信任，无时段。

- `leave_application` 增列：`start_period` TINYINT、`end_period` TINYINT（约定 `0`-上午 `1`-下午；开始默认上午、结束默认下午即全天）。
- 新增 `LeaveDayCalculator.calculate(startDate, startPeriod, endDate, endPeriod)`：
  - 取出 `[startDate, endDate]` 内所有工作日（复用 D1）；
  - 中间工作日各计 `1.0`；开始日计 `start_period=上午?1.0:0.5`，结束日计 `end_period=下午?1.0:0.5`；
  - 同一天：上午-下午=1.0，上午-上午 / 下午-下午=0.5。
- `LeaveApplyDTO` 增加 `startPeriod`/`endPeriod`；`days` 改为非必填，服务端计算后**覆盖**（不信任客户端值），杜绝少填天数绕过余额校验/超额请假。
- 新增试算接口 `GET /leave/days/calculate?startDate&startPeriod&endDate&endPeriod` 返回计算天数，供前端实时回显。
- 边界：区间无工作日 → 400；`endDate < startDate` → 400；时段仅允许 0/1。

请假扣款（7.x，`sumApprovedLeaveDays`）与考勤统计都以「工作日天数」为口径，系统计算后口径统一。

### D3: 附件复用 `AliOssUtil`，`leave_attachment` 存元数据，提交强制校验

项目已有 `AliOssUtil`（上传返回 URL）、`AliOssProperties`、`sky.alioss` 配置与 `aliyun-sdk-oss` 依赖，但从未装配。本次：

- 新增 `OssConfiguration`，以 `AliOssProperties` 为参构造 `AliOssUtil` 单例 Bean。
- 新增 `FileStorageService.upload(MultipartFile)`：校验扩展名（jpg/jpeg/png/pdf）与大小（≤10MB，可配置），生成 objectKey `leave/{yyyyMM}/{uuid}.{ext}`，调用 `AliOssUtil.upload` 返回 URL。
- 新建 `leave_attachment`：

```sql
CREATE TABLE `leave_attachment` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT,
    `application_id` BIGINT       DEFAULT NULL COMMENT '关联 leave_application.id（绑定前为 NULL）',
    `file_name`      VARCHAR(255) NOT NULL,
    `object_key`     VARCHAR(255) NOT NULL COMMENT 'OSS objectName',
    `file_url`       VARCHAR(512) NOT NULL,
    `file_size`      BIGINT       DEFAULT NULL,
    `content_type`   VARCHAR(64)  DEFAULT NULL,
    `upload_by`      BIGINT       NOT NULL,
    `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_application_id` (`application_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='请假附件表';
```

- 流程：`POST /leave/attachments`（multipart）上传 → 落 `leave_attachment`（`application_id` 暂为 NULL）返回 id+URL；`LeaveApplyDTO.attachmentIds` 在 `apply` 时绑定（回写 `application_id`，且校验附件归属本人）；`GET /leave/{id}/attachments` 供详情/审批查看（本人/审批人/HR）。
- **条件必填校验（submit 时）**：病假且 `days>1`、婚假、产假 → 该申请须已绑定 ≥1 附件，否则 400「请上传证明材料」。病假 ≤1 天、事假、年假、调休、丧假不强制。

- **备选方案**：前端直传 OSS 后回传 URL，后端只存 URL。否决（用户已选后端完整上传+存储）：后端可控文件类型/大小，安全与口径统一。

### D4: 年假「定时发放 + 入职初始化」，计算规则不变

经用户确认维持「满 1 年才开始享」，`calculateAnnualLeaveDays` 不动（首年 0 天）。补自动化：

- 新增 `AnnualLeaveAccrualTask`（`@Scheduled`，每年 1 月 1 日 02:00，另提供 HR 手动触发接口作补偿）：遍历全体**在职**员工，调用现有 `initAnnualLeaveBalance(employeeId, entryDate, currentYear)`（已是幂等 upsert：无则插、有则按 `total-used` 重算 remaining）。跨年档位（满 10/20 年）随年份自然升级。
- 入职生效时自动建行：`LeaveService.initNewEmployeeBalances(employeeId, entryDate, year)` 建年假（首年 0 天）与调休（0 天）余额行，由入职流程在员工激活时调用，保证新员工立即有余额行可查。
- 法定年假当年有效、不结转（需求未提结转，列 Non-goals），每年新行互不影响。

### D5: 调休用「入账明细表」承载有效期，FIFO 消耗，过期清零

6.3.2：加班时长累计 1:1 转调休、当月及次月有效、过期清零。单一 `leave_balance` 行无法承载「多笔不同过期日」，故新增明细表：

```sql
CREATE TABLE `comp_leave_grant` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT,
    `employee_id`    BIGINT       NOT NULL,
    `overtime_month` CHAR(7)      NOT NULL COMMENT '加班所属月 yyyy-MM',
    `days`           DECIMAL(4,1) NOT NULL COMMENT '本次折算入账天数',
    `used_days`      DECIMAL(4,1) NOT NULL DEFAULT 0.0,
    `expire_date`    DATE         NOT NULL COMMENT '过期日（加班次月月末）',
    `status`         TINYINT      NOT NULL DEFAULT 1 COMMENT '1-有效 0-已过期清零',
    `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`    DATETIME     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_employee_status_expire` (`employee_id`,`status`,`expire_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='调休入账明细表';

-- 请假对调休入账的占用明细（保证拒绝/取消时按原路径精确回补）
CREATE TABLE `comp_leave_usage` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT,
    `application_id` BIGINT       NOT NULL COMMENT '关联 leave_application.id',
    `grant_id`       BIGINT       NOT NULL COMMENT '关联 comp_leave_grant.id',
    `days`           DECIMAL(4,1) NOT NULL COMMENT '从该 grant 扣减的天数',
    `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_application_id` (`application_id`),
    KEY `idx_grant_id` (`grant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='调休占用明细表';
```

`leave_balance` 的调休行作为**聚合缓存**（total/used/remaining），随明细变动同步维护，供快速余额校验；`comp_leave_grant` 是有效期判定的权威来源，`comp_leave_usage` 是回补依据。三个动作：

1. **入账（CompLeaveService.convertOvertime，定时每月 1 日 + HR 手动）**：取员工 `converted_to_comp=0` 的加班记录累计时长，`days = floor(hours/8)`（维持现有 `calculateCompensatoryDays` 语义，余数小时记录保持未转换滚动到下一周期）；`days>0` 时插入 `comp_leave_grant`（`expire_date = 加班次月月末`）、累加 `leave_balance` 调休行，并将参与折算的加班记录 `markAsConverted`。
2. **消耗（提交调休请假时）**：`LeaveService.deductBalance` 对调休改为「按 `expire_date` 升序（先到期先用）逐笔扣减 `comp_leave_grant.used_days`」，同时扣 `leave_balance`；拒绝/取消回补时按原扣减记录逆向回填。需新增 `comp_leave_usage` 记录每笔请假占用到哪些 grant（保证回补精确）——见「数据流」。
3. **过期清零（CompLeaveExpiryTask，每日 03:00）**：`expire_date < 今天` 且 `used_days < days` 的 grant 置 `status=0`，并把未用差额 `days-used_days` 从 `leave_balance` 调休行 `remaining_days` 中扣减（不低于 0）。

- **加班来源说明**：加班申请/审批流为既有 Non-goal（`att:overtime:approve` 种子已有、流程后续立项），本期以 `overtime_record` 已存在数据为转换来源，待加班审批落地后可改为只转换「已审批」记录。
- **备选方案**：调休按小时粒度存储。否决：与 `leave_balance` 天数模型及既有「8 小时=1 天、不满 8 小时不计」测试冲突，改动面大。

### D6: 请假统计复用工作日历，数据权限按 RBAC

6.4 请假部分：个人请假天数汇总、年假余额、部门请假率 = 请假天数/应出勤天数、请假类型分布。

- `LeaveStatisticsService` + `LeaveStatisticsController`：
  - `GET /leave/stats/personal/{employeeId}?year&month`：该员工当月分类型「已通过」请假天数汇总 + 当年年假余额（本人/HR/主管）；
  - `GET /leave/stats/dept/{deptId}?year&month`：部门请假率 = 部门当月已通过请假天数合计 /（部门在职人数 × 当月工作日数），含分类型分布（HR/本部门主管）；
  - `GET /leave/stats/type-distribution?year&month[&deptId]`：分类型天数/人次分布（饼图数据，HR 全量、主管限本部门）。
- 应出勤天数 = D1 `workdaysBetween(月初, 月末)`；部门在职人数复用 `EmployeeMapper` 按部门统计。
- `LeaveApplicationMapper` 补聚合查询：按 `status=2`（已通过）且日期区间与当月相交，GROUP BY 类型 / 部门（JOIN employee）。
- 数据权限：普通员工仅本人；部门主管仅本部门（含下属）；HR/管理员全量。在 Service 层按当前用户角色与部门校验（对齐 RBAC 数据范围规则）。

### D7: 权限码新增与分配

遵循 CLAUDE.md「每完成一个模块检查权限管理」。`phase11` SQL 种子：

- 新增 `att:calendar`、`att:calendar:manage`（工作日历配置）→ 分配 ROLE_ADMIN、ROLE_HR；
- 附件上传 `att:leave:apply`（复用，员工及以上）、附件/统计查看 `att:leave:view`（复用）；
- 手动发放/调休转换触发 `att:record:manage`（复用，HR）；
- 统计的数据范围在 Service 层控制（主管限本部门），无需新增权限码。

## 数据流

**请假申请（病假 2 天，含附件）**：

1. 员工 `POST /leave/attachments` 上传医院证明 → 得 attachmentId；
2. `POST /leave/apply`（startDate+startPeriod/endDate+endPeriod + attachmentIds）→ `LeaveDayCalculator` 算出 days=2，绑定附件，建草稿；
3. `POST /leave/{id}/submit` → 校验病假>1 天已有附件 →（病假无余额占用）→ `startApproval(6)`，days>1 触发部门负责人二级审批；
4. 直接上级 → 部门负责人通过 → 置已通过、通知员工。

**调休（3 月加班 20 小时，5 月请 1 天调休）**：

1. 4 月 1 日入账任务：20 小时 → 2 天（余 4 小时滚动），插 grant（`overtime_month=2026-03`，`expire_date=2026-04-30`），调休余额 +2；
2. 5 月 1 日入账任务：若 4 月又加班补足 → 新 grant（`expire_date=2026-05-31`）；
3. 5 月请 1 天调休：先扣 `expire_date=2026-04-30`（已过期则跳过/已清零）→ 实际扣 `2026-05-31` 那笔；写 `comp_leave_usage` 留痕；
4. 每日过期任务：4 月 30 日后该 grant 未用完 → 置 0 并扣减余额。

## 异常与边界

- 请假区间无工作日 / `endDate<startDate` / 时段非法：400。
- 病假>1 天、婚假、产假提交时无附件：400「请上传证明材料」；绑定他人附件：403。
- 附件类型/大小超限：400；OSS 上传失败：500 并记录 error，不落元数据行。
- 年假发放任务幂等（重复跑不产生重复行——`initAnnualLeaveBalance` upsert + `uk_employee_leave_year` 唯一）。
- 调休入账重复跑：已 `markAsConverted` 的加班记录不重复折算（`converted_to_comp` 幂等屏障）。
- 过期清零并发：清零扣减 `remaining_days` 下限 0；与请假消耗并发靠 `@Transactional` + 行内余额校验兜底。
- 统计：部门无在职人数 / 当月无工作日时请假率按 0 处理，避免除零。
