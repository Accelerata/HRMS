package com.hrms.crypto;

import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.sql.*;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

/**
 * EncryptedBigDecimalTypeHandler 单元测试
 * 使用 H2 内存数据库 + 真实 JDBC 验证 BigDecimal 加解密链路
 */
@DisplayName("EncryptedBigDecimalTypeHandler 金额加密TypeHandler")
class EncryptedBigDecimalTypeHandlerTest {

    private static EncryptionUtil encryptionUtil;
    private static EncryptedBigDecimalTypeHandler handler;
    private static Connection conn;

    @BeforeAll
    static void setUpClass() throws Exception {
        byte[] keyBytes = new byte[32];
        for (int i = 0; i < 32; i++) keyBytes[i] = (byte) (i + 1);
        String testKey = Base64.getEncoder().encodeToString(keyBytes);

        EncryptionConfig config = new EncryptionConfig(testKey);
        encryptionUtil = new EncryptionUtil(config);
        EncryptedBigDecimalTypeHandler.setEncryptionUtil(encryptionUtil);
        handler = new EncryptedBigDecimalTypeHandler();

        conn = DriverManager.getConnection("jdbc:h2:mem:test_bd;MODE=MySQL");
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE test_money (" +
                    "id INT PRIMARY KEY, " +
                    "salary VARCHAR(256), " +
                    "bonus VARCHAR(256), " +
                    "deduction VARCHAR(256)" +
                    ")");
        }
    }

    @AfterAll
    static void tearDownClass() throws Exception {
        EncryptedBigDecimalTypeHandler.setEncryptionUtil(null);
        if (conn != null) conn.close();
    }

    @AfterEach
    void tearDown() throws Exception {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM test_money");
        }
    }

    // ═══════════════ 写入加密 ═══════════════

    @Test
    @DisplayName("写入BigDecimal应加密为字符串密文")
    void shouldEncryptOnSet() throws Exception {
        BigDecimal salary = new BigDecimal("12345.67");

        try (PreparedStatement ps = conn.prepareStatement("INSERT INTO test_money (id, salary) VALUES (?, ?)")) {
            ps.setInt(1, 1);
            handler.setNonNullParameter(ps, 2, salary, null);
            ps.executeUpdate();
        }

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT salary FROM test_money WHERE id = 1")) {
            assertTrue(rs.next());
            String stored = rs.getString("salary");
            assertNotNull(stored);
            assertNotEquals("12345.67", stored, "数据库存的应为密文");
            assertTrue(stored.length() > 8, "密文长度应大于明文");

            // 解密验证
            BigDecimal decrypted = encryptionUtil.decryptBigDecimal(stored);
            assertEquals(0, salary.compareTo(decrypted));
        }
    }

    @Test
    @DisplayName("写入整数金额应正常加密")
    void shouldEncryptInteger() throws Exception {
        BigDecimal salary = new BigDecimal("50000");

        try (PreparedStatement ps = conn.prepareStatement("INSERT INTO test_money (id, salary) VALUES (?, ?)")) {
            ps.setInt(1, 2);
            handler.setNonNullParameter(ps, 2, salary, null);
            ps.executeUpdate();
        }

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT salary FROM test_money WHERE id = 2")) {
            assertTrue(rs.next());
            String stored = rs.getString("salary");
            assertNotEquals("50000", stored);
            assertEquals(0, salary.compareTo(encryptionUtil.decryptBigDecimal(stored)));
        }
    }

    @Test
    @DisplayName("写入零值应正常加密")
    void shouldEncryptZero() throws Exception {
        try (PreparedStatement ps = conn.prepareStatement("INSERT INTO test_money (id, salary) VALUES (?, ?)")) {
            ps.setInt(1, 3);
            handler.setNonNullParameter(ps, 2, BigDecimal.ZERO, null);
            ps.executeUpdate();
        }

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT salary FROM test_money WHERE id = 3")) {
            assertTrue(rs.next());
            BigDecimal result = handler.getNullableResult(rs, "salary");
            assertEquals(0, BigDecimal.ZERO.compareTo(result));
        }
    }

    @Test
    @DisplayName("写入负数应正常加密（扣款场景）")
    void shouldEncryptNegative() throws Exception {
        BigDecimal deduction = new BigDecimal("-500.00");

        try (PreparedStatement ps = conn.prepareStatement("INSERT INTO test_money (id, deduction) VALUES (?, ?)")) {
            ps.setInt(1, 4);
            handler.setNonNullParameter(ps, 2, deduction, null);
            ps.executeUpdate();
        }

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT deduction FROM test_money WHERE id = 4")) {
            assertTrue(rs.next());
            BigDecimal result = handler.getNullableResult(rs, "deduction");
            assertEquals(0, deduction.compareTo(result));
        }
    }

    // ═══════════════ 读取解密 ═══════════════

    @Test
    @DisplayName("读取应解密为BigDecimal")
    void shouldDecryptOnGet() throws Exception {
        BigDecimal salary = new BigDecimal("28000.50");
        String ciphertext = encryptionUtil.encryptBigDecimal(salary);

        try (PreparedStatement ps = conn.prepareStatement("INSERT INTO test_money (id, salary) VALUES (?, ?)")) {
            ps.setInt(1, 5);
            ps.setString(2, ciphertext);
            ps.executeUpdate();
        }

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT salary FROM test_money WHERE id = 5")) {
            assertTrue(rs.next());
            BigDecimal result = handler.getNullableResult(rs, "salary");
            assertNotNull(result);
            assertEquals(0, salary.compareTo(result));
        }
    }

    @Test
    @DisplayName("通过列索引读取")
    void shouldDecryptByIndex() throws Exception {
        BigDecimal bonus = new BigDecimal("1500.00");
        String ciphertext = encryptionUtil.encryptBigDecimal(bonus);

        try (PreparedStatement ps = conn.prepareStatement("INSERT INTO test_money (id, bonus) VALUES (?, ?)")) {
            ps.setInt(1, 6);
            ps.setString(2, ciphertext);
            ps.executeUpdate();
        }

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id, bonus FROM test_money WHERE id = 6")) {
            assertTrue(rs.next());
            BigDecimal result = handler.getNullableResult(rs, 2);
            assertEquals(0, bonus.compareTo(result));
        }
    }

    @Test
    @DisplayName("读取NULL返回NULL")
    void shouldReturnNullOnGet() throws Exception {
        try (PreparedStatement ps = conn.prepareStatement("INSERT INTO test_money (id, salary) VALUES (?, ?)")) {
            ps.setInt(1, 7);
            ps.setNull(2, Types.VARCHAR);
            ps.executeUpdate();
        }

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT salary FROM test_money WHERE id = 7")) {
            assertTrue(rs.next());
            BigDecimal result = handler.getNullableResult(rs, "salary");
            assertNull(result);
        }
    }

    // ═══════════════ 往返一致性 ═══════════════

    @Test
    @DisplayName("多个典型金额往返一致")
    void shouldRoundTripMultiple() throws Exception {
        BigDecimal[] values = {
                new BigDecimal("0.00"),
                new BigDecimal("0.01"),
                new BigDecimal("1.00"),
                new BigDecimal("9999.99"),
                new BigDecimal("50000.00"),
                new BigDecimal("123456.78"),
                new BigDecimal("99999999.99"),
        };

        for (int i = 0; i < values.length; i++) {
            int id = 10 + i;
            BigDecimal original = values[i];

            // 写入
            try (PreparedStatement ps = conn.prepareStatement("INSERT INTO test_money (id, salary) VALUES (?, ?)")) {
                ps.setInt(1, id);
                handler.setNonNullParameter(ps, 2, original, null);
                ps.executeUpdate();
            }

            // 读取
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT salary FROM test_money WHERE id = " + id)) {
                assertTrue(rs.next());
                BigDecimal result = handler.getNullableResult(rs, "salary");
                assertEquals(0, original.compareTo(result),
                        "往返不一致: " + original.toPlainString() + " → " + result.toPlainString());
            }
        }
    }

    @Test
    @DisplayName("精度到分保持一致")
    void shouldPreservePrecision() throws Exception {
        BigDecimal precise = new BigDecimal("15000.67");

        try (PreparedStatement ps = conn.prepareStatement("INSERT INTO test_money (id, salary) VALUES (?, ?)")) {
            ps.setInt(1, 20);
            handler.setNonNullParameter(ps, 2, precise, null);
            ps.executeUpdate();
        }

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT salary FROM test_money WHERE id = 20")) {
            assertTrue(rs.next());
            BigDecimal result = handler.getNullableResult(rs, "salary");
            assertEquals("15000.67", result.toPlainString());
        }
    }

    // ═══════════════ 随机IV ═══════════════

    @Test
    @DisplayName("两次写入相同金额应产生不同密文")
    void shouldProduceDifferentCiphertexts() throws Exception {
        BigDecimal salary = new BigDecimal("10000.00");

        try (PreparedStatement ps = conn.prepareStatement("INSERT INTO test_money (id, salary) VALUES (?, ?)")) {
            ps.setInt(1, 30);
            handler.setNonNullParameter(ps, 2, salary, null);
            ps.executeUpdate();
        }
        try (PreparedStatement ps = conn.prepareStatement("INSERT INTO test_money (id, salary) VALUES (?, ?)")) {
            ps.setInt(1, 31);
            handler.setNonNullParameter(ps, 2, salary, null);
            ps.executeUpdate();
        }

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT salary FROM test_money WHERE id IN (30,31) ORDER BY id")) {
            assertTrue(rs.next());
            String ct1 = rs.getString("salary");
            assertTrue(rs.next());
            String ct2 = rs.getString("salary");

            assertNotEquals(ct1, ct2, "随机IV应产生不同密文");
            assertEquals(0, salary.compareTo(encryptionUtil.decryptBigDecimal(ct1)));
            assertEquals(0, salary.compareTo(encryptionUtil.decryptBigDecimal(ct2)));
        }
    }

    // ═══════════════ 无 EncryptionUtil 回退 ═══════════════

    @Test
    @DisplayName("无EncryptionUtil时写入BigDecimal")
    void shouldPassBigDecimalWhenNoUtil() throws Exception {
        EncryptedBigDecimalTypeHandler.setEncryptionUtil(null);
        try {
            BigDecimal salary = new BigDecimal("5000.00");

            try (PreparedStatement ps = conn.prepareStatement("INSERT INTO test_money (id, salary) VALUES (?, ?)")) {
                ps.setInt(1, 40);
                handler.setNonNullParameter(ps, 2, salary, null);
                ps.executeUpdate();
            }

            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT salary FROM test_money WHERE id = 40")) {
                assertTrue(rs.next());
                // 无加密时直接存 BigDecimal 的字符串形式
                assertEquals("5000.00", rs.getString("salary"));
            }
        } finally {
            EncryptedBigDecimalTypeHandler.setEncryptionUtil(encryptionUtil);
        }
    }
}
