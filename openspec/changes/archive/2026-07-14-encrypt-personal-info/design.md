## Context

HRMS 系统当前以明文形式在 MySQL 数据库中存储了 10 张表中的约 30+ 个敏感字段，包括身份证号、手机号、银行账号、家庭住址、薪资数据等。唯一的保护措施是 `SysUser.password` 采用了 BCrypt 哈希。

需要实现数据库层面的字段级加密，确保即使数据库被拖库，攻击者也无法读取敏感数据。加密方案需对业务代码透明，尽量减少侵入性。

### 约束

- 使用 JDK 自带 `javax.crypto`（无需引入第三方加密库）
- 基于 MyBatis TypeHandler 实现，与现有持久层无缝集成
- 密钥与代码分离，支持环境差异化配置
- 需要支持对加密字段的精确查询（如按手机号查找员工）
- 存量数据需平滑迁移，不能影响线上业务

## Goals / Non-Goals

**Goals:**
- 对 Employee、OnboardingApplication、SalaryAccount、SalaryRecord 等表中所有 PII 和财务敏感字段实现 AES-256 加密存储
- 通过 MyBatis TypeHandler 实现写入自动加密、读取自动解密，业务代码零侵入
- 对需要精确匹配查询的字段（phone、idCard、bankAccount）支持加密后等值查询
- 提供安全的密钥管理方案（环境变量/配置中心），支持密钥轮换
- 提供存量数据迁移脚本，支持分批处理和回滚

**Non-Goals:**
- 不加密 JWT token 或 session 数据（这些由各自的机制保护）
- 不实现传输层加密（已有 HTTPS 的场景，且不属于本 change 范围）
- 不改变前端展示逻辑（DTO/VO 中仍处理明文，加解密在持久层完成）
- 不处理日志中的敏感数据脱敏（separate concern）
- 不处理 `EmployeeTransfer.beforeData/afterData` JSON 快照的加密（该字段为完整序列化，需单独的序列化拦截方案）

## Decisions

### 1. 加密算法：AES-256-GCM

**选择**: AES-256/GCM/NoPadding（Galois/Counter Mode）

**理由**:
- GCM 模式是认证加密（AEAD），同时提供机密性和完整性校验，可防止密文被篡改
- 无 padding oracle 攻击面（CBC 模式有此类风险）
- JDK 9+ 原生支持，无需 Bouncy Castle 等第三方库
- NIST 推荐的对称加密标准，符合合规要求

**备选方案**:
- AES-CBC: 有 padding oracle 风险，需额外的 HMAC 校验，复杂度更高
- SM4: 国密算法，合规性更好但 JDK 不原生支持，需引入 Bouncy Castle，且 GCM 模式支持不成熟
- 应用层加密库（如 Jasypt）: 引入额外依赖，灵活性受限

### 2. 加解密实现层：MyBatis TypeHandler

**选择**: 自定义 `EncryptedStringTypeHandler` 和 `EncryptedBigDecimalTypeHandler`，在 MyBatis 映射层透明加解密

**理由**:
- TypeHandler 在 JDBC 参数设置和结果集读取时自动调用，业务 Service 层完全无感知
- 粒度精确到字段级别，不同字段可使用不同加密策略
- 与现有 MyBatis 架构完全兼容，只需在 Mapper XML 的 `#{field}` 占位符中声明 `typeHandler`

**备选方案**:
- JPA `@Convert` 注解: 项目使用 MyBatis 而非 JPA，不适用
- AOP 拦截 Entity setter/getter: 粒度粗糙，无法区分敏感/非敏感字段
- 数据库层面加密（MySQL TDE）: 需要 MySQL Enterprise Edition，成本高，且数据在传输和内存中仍为明文

### 3. 查询支持：确定性加密 + HMAC 哈希索引

**选择**: 对需要精确查询的字段（phone、idCard、bankAccount）采用确定性加密（固定 IV），对不需要查询的字段（address、salary）采用随机 IV 加密。同时在需要查询的字段旁增加 `*_hash` 辅助索引列

**方案细节**:
- **确定性加密字段** (phone, idCard, bankAccount): 使用 HMAC-SHA256 从字段值派生固定 IV，保证相同明文始终产生相同密文 → 支持 `SELECT ... WHERE phone_encrypted = ?`
- **随机 IV 字段** (address, salary, email): 每次加密使用 `SecureRandom` 生成随机 IV，IV 前缀存储在密文中 → 安全性更高但不支持直接查询
- **哈希辅助索引**: 对高频查询字段增加 `phone_hash`、`id_card_hash` 列，存储 `SHA-256(plaintext + pepper)`，查询时通过哈希匹配。哈希列无法还原明文

**备选方案**:
- 全字段随机 IV + 应用层解密后筛选: 性能极差，全表遍历不可接受
- 数据库加密函数（MySQL `AES_ENCRYPT`）: 密钥在 SQL 语句中传输，日志中可能泄露

### 4. 密钥管理：环境变量 + Spring 配置

**选择**: 主密钥通过环境变量 `HRMS_ENCRYPTION_KEY` 注入，Spring Boot 在启动时读取并初始化 `EncryptionConfig`

**密钥层级**:
```
Master Key (环境变量 HRMS_ENCRYPTION_KEY, 256-bit)
  └── 通过 HKDF 派生 → Data Encryption Key (用于 AES-GCM 加密)
  └── 通过 HKDF 派生 → HMAC Key (用于 IV 派生和哈希索引)
```

**运行时**:
- 启动时一次性读取密钥，缓存在 `EncryptionConfig` Bean 中
- 密钥明文不出现在配置文件、日志或代码中
- 支持密钥轮换：新密钥加密新数据，旧密钥用于解密历史数据（通过 `key_version` 列标记）

### 5. 需要加密的字段清单

基于代码审计结果，确定以下加密范围：

| 表 | 字段 | 加密策略 | 查询支持 |
|---|---|---|---|
| employee | phone | 确定性加密 + hash 索引 | 是（按手机号查重/搜索） |
| employee | email | 随机 IV 加密 | 否 |
| employee | id_card | 确定性加密 + hash 索引 | 是（按身份证查重） |
| employee | registered_address | 随机 IV 加密 | 否 |
| employee | current_address | 随机 IV 加密 | 否 |
| employee | bank_account | 确定性加密 + hash 索引 | 是 |
| employee | bank_name | 随机 IV 加密 | 否 |
| employee | base_salary | 随机 IV 加密 | 否 |
| onboarding_application | phone, email, id_card, registered_address, current_address, bank_account, bank_name, offer_salary | 同上策略 | 同 employee |
| salary_account | basic_salary, position_salary, performance_salary, social_insurance_base, housing_fund_base | 随机 IV 加密 | 否 |
| salary_record | 所有金额字段 | 随机 IV 加密 | 否 |
| regularization_application | formal_salary | 随机 IV 加密 | 否 |

### 6. 数据迁移策略

- 新增 `V2__encrypt_sensitive_data.sql` Flyway 迁移脚本
- 先 ALTER TABLE 添加新加密列（如 `phone_encrypted`）和哈希辅助列
- 分批（每批 1000 条）读取明文 → 应用加密 → 写入加密列
- 验证加密完整后删除明文字段
- 迁移期间业务正常读写（TypeHandler 兼容新旧列并存期间）

### 7. 数据库 Schema 变更

```sql
-- 示例：employee 表
ALTER TABLE employee 
  ADD COLUMN phone_encrypted VARCHAR(512) COMMENT '手机号(加密)',
  ADD COLUMN phone_hash CHAR(64) COMMENT '手机号哈希索引',
  ADD COLUMN id_card_encrypted VARCHAR(512) COMMENT '身份证号(加密)',
  ADD COLUMN id_card_hash CHAR(64) COMMENT '身份证号哈希索引',
  -- ... 其他加密列
  ADD COLUMN encryption_version TINYINT DEFAULT 1 COMMENT '加密版本(支持密钥轮换)';
```

## Risks / Trade-offs

- **[性能] 加解密增加 CPU 开销** → 预估每次读写增加 < 1ms（AES-GCM 硬件加速），对 HRMS 这类 OLTP 低并发系统影响可忽略。批量查询时 TypeHandler 逐行解密，大数据量（>10000 条）场景需要基准测试验证。
- **[存储膨胀] 密文 + Base64 编码约为原文 1.5-4 倍** → 如 `idCard` 原文 18 字节，加密+IV+Base64 约 80 字节。需为加密列使用 `VARCHAR(512)` 预留空间。
- **[模糊搜索不可用] 加密后无法 LIKE 查询** → 如需姓名模糊搜索，保持 `name` 字段不加密。手机号/身份证精确匹配通过哈希索引支持。
- **[密钥泄露 = 全部数据泄露]** → 密钥必须严格管理：环境变量注入、禁止日志打印、定期轮换。建议生产环境使用 Vault/KMS 管理密钥。
- **[EmployeeTransfer JSON 快照暂不加密]** → `beforeData`/`afterData` 列存储完整 Employee JSON，加密需在序列化层拦截。本 change 先覆盖结构化字段，JSON 快照作为 follow-up。

## Open Questions

1. **密钥轮换具体频率和流程？** 建议 6-12 个月轮换一次，轮换时需要旧密钥解密 + 新密钥重新加密，是否接受维护窗口？
2. **是否需要引入 Vault/AWS KMS/阿里云 KMS？** 当前方案用环境变量管理密钥，生产环境建议接入专业 KMS。
3. **薪资字段是否需要可查询？** 当前方案对薪资字段使用随机 IV 加密，不支持 SQL 范围查询（如 `WHERE base_salary > 10000`）。如需支持，可在应用层做筛选或使用保序加密（OPE），但 OPE 安全性较弱。
4. **SM4 国密必要性和时间节点？** 如果客户要求国密合规，后续可增加 SM4-GCM 的支持层。
