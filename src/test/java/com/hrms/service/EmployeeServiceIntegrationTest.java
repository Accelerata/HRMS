package com.hrms.service;

import com.hrms.crypto.EncryptionUtil;
import com.hrms.dto.EmployeeSaveDTO;
import com.hrms.entity.Employee;
import com.hrms.mapper.EmployeeMapper;
import com.hrms.vo.EmployeeVO;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * EmployeeService 集成测试
 * 验证加密存储、解密读取、哈希索引查询的全链路正确性
 *
 * 要求: MySQL 数据库已运行，所有 DDL 脚本已执行
 */
@SpringBootTest
@DisplayName("EmployeeService 加密集成测试")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class EmployeeServiceIntegrationTest {

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private EmployeeMapper employeeMapper;

    @Autowired
    private EncryptionUtil encryptionUtil;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static Long createdEmployeeId;
    private static final String TEST_PHONE = "19900008888";
    private static final String TEST_ID_CARD = "440101199508080012";
    private static final String TEST_BANK_ACCOUNT = "6228480012345678912";

    // ═══════════════ 8.3: 加密存储与解密读取 ═══════════════

    @Test
    @Order(1)
    @DisplayName("8.3 创建员工→数据库存密文→读取返回明文")
    @Transactional
    void shouldStoreCiphertextAndReturnPlaintext() {
        // ── 1. 创建员工 ──
        EmployeeSaveDTO dto = buildTestDTO();
        EmployeeVO created = employeeService.create(dto);

        assertNotNull(created.getId(), "应回填主键ID");
        createdEmployeeId = created.getId();

        // ── 2. 直接查数据库原始值，验证敏感字段是密文 ──
        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT phone, email, id_card, bank_account, base_salary FROM employee WHERE id = ?",
                createdEmployeeId);

        // phone: 密文（Base64 编码，与明文不同）
        String dbPhone = (String) row.get("phone");
        assertNotNull(dbPhone, "phone 列应有值");
        assertNotEquals(TEST_PHONE, dbPhone, "数据库 phone 应为密文而非明文");

        // email: 密文
        String dbEmail = (String) row.get("email");
        assertNotNull(dbEmail);
        assertNotEquals("test8888@hrms.com", dbEmail, "数据库 email 应为密文");

        // id_card: 密文
        String dbIdCard = (String) row.get("id_card");
        assertNotNull(dbIdCard);
        assertNotEquals(TEST_ID_CARD, dbIdCard, "数据库 id_card 应为密文");

        // bank_account: 密文
        String dbBankAccount = (String) row.get("bank_account");
        assertNotNull(dbBankAccount);
        assertNotEquals(TEST_BANK_ACCOUNT, dbBankAccount, "数据库 bank_account 应为密文");

        // base_salary: 密文（VARCHAR 列存加密后的字符串）
        String dbBaseSalary = (String) row.get("base_salary");
        assertNotNull(dbBaseSalary);
        assertNotEquals("8000.00", dbBaseSalary, "数据库 base_salary 应为密文");

        // 验证密文可被 EncryptionUtil 解密
        assertEquals(TEST_PHONE, encryptionUtil.decrypt(dbPhone), "密文 phone 应可解密");
        assertEquals(TEST_ID_CARD, encryptionUtil.decrypt(dbIdCard), "密文 id_card 应可解密");

        // ── 3. 通过 Service 读取，验证返回的是明文 ──
        EmployeeVO read = employeeService.getById(createdEmployeeId);
        assertEquals(TEST_PHONE, read.getPhone(), "Service 返回的 phone 应为明文");
        assertEquals("test8888@hrms.com", read.getEmail(), "Service 返回的 email 应为明文");
        assertEquals(TEST_ID_CARD, read.getIdCard(), "Service 返回的 idCard 应为明文");
        assertEquals(TEST_BANK_ACCOUNT, read.getBankAccount(), "Service 返回的 bankAccount 应为明文");
        assertEquals(0, new BigDecimal("8000.00").compareTo(read.getBaseSalary()),
                "Service 返回的 baseSalary 应为明文");

        // ── 4. 验证非敏感字段正常 ──
        assertEquals("集成测试", read.getName());
        assertEquals("IT", read.getGrade());
    }

    @Test
    @Order(2)
    @DisplayName("8.3 更新员工→旧密文被覆盖→新值加密存储")
    @Transactional
    void shouldEncryptUpdatedFields() {
        // 先创建一个
        EmployeeSaveDTO dto = buildTestDTO();
        dto.setPhone("19900009999");
        EmployeeVO created = employeeService.create(dto);

        // 更新手机号和工资
        EmployeeSaveDTO update = new EmployeeSaveDTO();
        update.setId(created.getId());
        update.setName("集成测试");
        update.setPhone("19900007777");
        update.setEmail("updated@hrms.com");
        update.setIdCard(TEST_ID_CARD);
        update.setDeptId(1L);
        update.setPositionId(4L);
        update.setEntryType(1);
        update.setEntryDate(LocalDate.now());
        update.setBaseSalary(new BigDecimal("12000.00"));

        EmployeeVO updated = employeeService.update(update);
        assertEquals("19900007777", updated.getPhone());
        assertEquals(0, new BigDecimal("12000.00").compareTo(updated.getBaseSalary()));

        // 数据库中的 phone 密文应与旧密文不同（新值）
        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT phone FROM employee WHERE id = ?", created.getId());
        String dbPhone = (String) row.get("phone");
        assertNotEquals("19900007777", dbPhone, "更新后 phone 仍为密文");
        assertEquals("19900007777", encryptionUtil.decrypt(dbPhone), "解密后应为新值");
    }

    // ═══════════════ 8.4: 哈希索引查询 ═══════════════

    @Test
    @Order(3)
    @DisplayName("8.4 按手机号哈希精确查询")
    @Transactional
    void shouldFindByPhoneHash() {
        // 先创建一个确定手机号的员工
        EmployeeSaveDTO dto = buildTestDTO();
        dto.setPhone("18800001234");
        employeeService.create(dto);

        // 通过哈希查询
        String hash = encryptionUtil.computeHash("18800001234");
        assertNotNull(hash, "哈希值不应为空");
        assertEquals(64, hash.length(), "SHA-256 哈希应为 64 字符");

        Employee found = employeeMapper.selectByPhoneHash(hash);
        assertNotNull(found, "应通过 phone_hash 找到员工");
        assertEquals("18800001234", found.getPhone(), "返回的 phone 应为明文");
        assertEquals("集成测试", found.getName());
    }

    @Test
    @Order(4)
    @DisplayName("8.4 按身份证号哈希查重")
    @Transactional
    void shouldFindByIdCardHash() {
        EmployeeSaveDTO dto = buildTestDTO();
        dto.setIdCard("330102199508081234");
        employeeService.create(dto);

        String hash = encryptionUtil.computeHash("330102199508081234");
        Employee found = employeeMapper.selectByIdCardHash(hash);
        assertNotNull(found, "应通过 id_card_hash 找到员工");
        assertEquals("330102199508081234", found.getIdCard());
    }

    @Test
    @Order(5)
    @DisplayName("8.4 手机号查重-不存在的手机号应返回null")
    @Transactional
    void shouldReturnNullForNonExistentPhone() {
        String hash = encryptionUtil.computeHash("00000000000");
        Employee found = employeeMapper.selectByPhoneHash(hash);
        assertNull(found, "不存在的手机号应返回 null");
    }

    @Test
    @Order(6)
    @DisplayName("8.4 手机号唯一性校验-重复手机号应抛异常")
    @Transactional
    void shouldRejectDuplicatePhone() {
        EmployeeSaveDTO dto = buildTestDTO();
        dto.setPhone("16600005555");
        employeeService.create(dto);

        // 再用相同手机号创建 → 应抛异常
        EmployeeSaveDTO dup = buildTestDTO();
        dup.setPhone("16600005555");
        dup.setIdCard("510102199512121234"); // 不同身份证

        assertThrows(com.hrms.common.exception.BaseException.class,
                () -> employeeService.create(dup),
                "重复手机号应抛出异常");
    }

    @Test
    @Order(7)
    @DisplayName("8.4 身份证号唯一性校验-重复身份证应抛异常")
    @Transactional
    void shouldRejectDuplicateIdCard() {
        EmployeeSaveDTO dto = buildTestDTO();
        dto.setIdCard("610102199612121234");
        employeeService.create(dto);

        EmployeeSaveDTO dup = buildTestDTO();
        dup.setPhone("17700006666"); // 不同手机号
        dup.setIdCard("610102199612121234");

        assertThrows(com.hrms.common.exception.BaseException.class,
                () -> employeeService.create(dup),
                "重复身份证号应抛出异常");
    }

    @Test
    @Order(8)
    @DisplayName("8.4 银行账号哈希写入验证")
    @Transactional
    void shouldStoreBankAccountHash() {
        EmployeeSaveDTO dto = buildTestDTO();
        dto.setBankAccount("6222021234567890");
        EmployeeVO created = employeeService.create(dto);

        // 验证 hash 列有值
        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT bank_account, bank_account_hash FROM employee WHERE id = ?",
                created.getId());

        String dbBankAccount = (String) row.get("bank_account");
        String dbBankAccountHash = (String) row.get("bank_account_hash");

        assertNotNull(dbBankAccount, "bank_account 密文应有值");
        assertNotNull(dbBankAccountHash, "bank_account_hash 应有值");
        assertEquals(64, dbBankAccountHash.length(), "哈希应为 64 字符");
        assertNotEquals("6222021234567890", dbBankAccount, "bank_account 应为密文");
        assertEquals("6222021234567890", encryptionUtil.decrypt(dbBankAccount),
                "解密 bank_account 应为明文");
    }

    // ═══════════════ 辅助方法 ═══════════════

    private EmployeeSaveDTO buildTestDTO() {
        EmployeeSaveDTO dto = new EmployeeSaveDTO();
        dto.setName("集成测试");
        dto.setPhone(TEST_PHONE);
        dto.setEmail("test8888@hrms.com");
        dto.setIdCard(TEST_ID_CARD);
        dto.setGender(1);
        dto.setBirthday(LocalDate.of(1995, 8, 8));
        dto.setRegisteredAddress("广东省广州市天河区");
        dto.setCurrentAddress("北京市朝阳区望京");
        dto.setDeptId(1L);
        dto.setPositionId(4L);  // 高级工程师
        dto.setGrade("IT");
        dto.setWorkLocation("北京总部");
        dto.setEntryType(1);  // 社招
        dto.setEntryDate(LocalDate.now());
        dto.setBaseSalary(new BigDecimal("8000.00"));
        dto.setBankAccount(TEST_BANK_ACCOUNT);
        dto.setBankName("中国农业银行");
        dto.setStatus(1);  // 试用期
        return dto;
    }
}
