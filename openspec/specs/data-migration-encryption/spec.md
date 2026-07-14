# data-migration-encryption

## Purpose

对 HRMS 中已存在的明文敏感数据进行分批加密迁移，确保迁移过程可追踪、可回滚、可验证，并在验证通过后安全删除明文字段。

## Requirements

### Requirement: 存量数据加密迁移

系统 SHALL 提供数据库迁移脚本，将已存在的明文敏感数据分批加密后写入新的加密列。迁移过程 MUST 支持分批处理（每批 1000 条），避免长事务锁表。

#### Scenario: employee 表明文数据加密迁移

- **WHEN** 执行迁移脚本处理 `employee` 表
- **THEN** 脚本分批读取明文记录，使用 `EncryptionUtil` 加密每个敏感字段，将密文写入对应的 `*_encrypted` 列，同时计算并写入 `*_hash` 列，每批提交一次事务

#### Scenario: salary_record 表明文数据加密迁移

- **WHEN** 执行迁移脚本处理 `salary_record` 表
- **THEN** 所有金额字段的明文值被加密后写入 `*_encrypted` 列

#### Scenario: 迁移进度可追踪

- **WHEN** 迁移执行中
- **THEN** 脚本输出当前进度（已处理行数/总行数），每批处理后打印日志。迁移过程中若中断，再次运行可跳过已加密的行继续执行（幂等性）

### Requirement: 迁移后数据完整性校验

系统 MUST 在迁移完成后校验数据完整性：随机抽取至少 5% 的记录，解密后与原文对比，确保加密/解密循环无损。校验不通过 MUST 阻止后续的明文字段删除步骤。

#### Scenario: 校验通过 - 加密无损

- **WHEN** 迁移完成，执行校验脚本，随机抽取记录解密并与明文原文对比
- **THEN** 所有抽样记录解密结果与原文完全一致，校验通过，允许继续删除明文字段

#### Scenario: 校验失败 - 阻止删除

- **WHEN** 校验脚本发现解密结果与原文不一致
- **THEN** 脚本输出不匹配的记录 ID，阻止后续的 DROP COLUMN 操作，提示人工排查

### Requirement: 密文明文列共存期间兼容

系统 MUST 在加密列和明文列共存期间（迁移进行中），优先读取加密列。若加密列为 NULL 则回退到明文字段，确保迁移中业务不受影响。

#### Scenario: 已加密的行读取加密列

- **WHEN** 查询时某行的 `phone_encrypted` 列有值
- **THEN** TypeHandler 直接解密并返回密文列的值

#### Scenario: 未加密的行回退到明文列

- **WHEN** 查询时某行的 `phone_encrypted` 列为 NULL（尚未迁移）
- **THEN** TypeHandler 回退读取原明文 `phone` 列，保证数据不丢失

### Requirement: 迁移回滚方案

系统 SHALL 保留明文字段直至迁移验证通过。若需要回滚，MUST 提供回滚脚本：删除所有 `*_encrypted` 和 `*_hash` 列，恢复使用原文列。回滚后业务功能 MUST 不受影响。

#### Scenario: 执行回滚

- **WHEN** 迁移后发现加密相关问题需要回滚
- **THEN** 执行回滚脚本，代码中移除 TypeHandler 注册，恢复原有 MyBatis 映射，业务正常运行

### Requirement: 删除明文列

迁移验证通过后，系统 SHALL 通过独立的 DDL 脚本删除所有原明文字段，确保数据库中不再存储明文敏感数据。删除脚本 MUST 在验证通过、且在低峰期执行。

#### Scenario: 验证通过后删除明文列

- **WHEN** 数据完整性校验 100% 通过，且确认业务功能正常
- **THEN** 执行 `ALTER TABLE ... DROP COLUMN phone, DROP COLUMN id_card, ...` 删除明文字段，数据库仅存储密文
