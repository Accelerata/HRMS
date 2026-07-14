package com.hrms.crypto;

import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.sql.*;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

/**
 * EncryptedStringTypeHandler 单元测试
 * 使用 H2 内存数据库 + 真实 JDBC 验证 set/get 的加解密行为
 */
@DisplayName("EncryptedStringTypeHandler 字符串加密TypeHandler")
class EncryptedStringTypeHandlerTest {

    private static EncryptionUtil encryptionUtil;
    private static EncryptedStringTypeHandler handler;
    private static Connection conn;

    @BeforeAll
    static void setUpClass() throws Exception {
        // 生成固定测试密钥
        byte[] keyBytes = new byte[32];
        for (int i = 0; i < 32; i++) keyBytes[i] = (byte) (i + 1);
        String testKey = Base64.getEncoder().encodeToString(keyBytes);

        EncryptionConfig config = new EncryptionConfig(testKey);
        encryptionUtil = new EncryptionUtil(config);
        EncryptedStringTypeHandler.setEncryptionUtil(encryptionUtil);
        EncryptedBigDecimalTypeHandler.setEncryptionUtil(encryptionUtil);
        handler = new EncryptedStringTypeHandler();

        // H2 内存数据库
        conn = DriverManager.getConnection("jdbc:h2:mem:test_handler;MODE=MySQL");
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE test_str (" +
                    "id INT PRIMARY KEY, " +
                    "phone VARCHAR(512), " +
                    "email VARCHAR(512), " +
                    "address VARCHAR(1024)" +
                    ")");
        }
    }

    @AfterAll
    static void tearDownClass() throws Exception {
        EncryptedStringTypeHandler.setEncryptionUtil(null);
        EncryptedBigDecimalTypeHandler.setEncryptionUtil(null);
        if (conn != null) conn.close();
    }

    @AfterEach
    void tearDown() throws Exception {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM test_str");
        }
    }

    // ═══════════════ 写入加密 ═══════════════

    @Test
    @DisplayName("写入时应加密明文，数据库存的是密文")
    void shouldEncryptOnSet() throws Exception {
        String plaintext = "13800138000";

        try (PreparedStatement ps = conn.prepareStatement("INSERT INTO test_str (id, phone) VALUES (?, ?)")) {
            ps.setInt(1, 1);
            handler.setNonNullParameter(ps, 2, plaintext, null);
            ps.executeUpdate();
        }

        // 直接查原始值
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT phone FROM test_str WHERE id = 1")) {
            assertTrue(rs.next());
            String stored = rs.getString("phone");
            assertNotNull(stored);
            assertNotEquals(plaintext, stored, "数据库存的值不应等于明文");
            assertTrue(stored.length() > plaintext.length(), "密文长度应大于明文");
        }
    }

    @Test
    @DisplayName("写入中文地址应加密存储")
    void shouldEncryptChinese() throws Exception {
        String address = "北京市朝阳区建国路88号";

        try (PreparedStatement ps = conn.prepareStatement("INSERT INTO test_str (id, address) VALUES (?, ?)")) {
            ps.setInt(1, 2);
            handler.setNonNullParameter(ps, 2, address, null);
            ps.executeUpdate();
        }

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT address FROM test_str WHERE id = 2")) {
            assertTrue(rs.next());
            String stored = rs.getString("address");
            assertNotEquals(address, stored, "中文地址应为密文");
            assertEquals(address, encryptionUtil.decrypt(stored), "解密后应与原文一致");
        }
    }

    @Test
    @DisplayName("写入NULL应存NULL")
    void shouldStoreNull() throws Exception {
        try (PreparedStatement ps = conn.prepareStatement("INSERT INTO test_str (id, phone) VALUES (?, ?)")) {
            ps.setInt(1, 3);
            ps.setNull(2, Types.VARCHAR);
            ps.executeUpdate();
        }

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT phone FROM test_str WHERE id = 3")) {
            assertTrue(rs.next());
            assertNull(rs.getString("phone"));
        }
    }

    // ═══════════════ 读取解密 ═══════════════

    @Test
    @DisplayName("读取应自动解密为明文")
    void shouldDecryptOnGet() throws Exception {
        String plaintext = "13900139000";
        String ciphertext = encryptionUtil.encrypt(plaintext);

        // 直接插入密文
        try (PreparedStatement ps = conn.prepareStatement("INSERT INTO test_str (id, phone) VALUES (?, ?)")) {
            ps.setInt(1, 4);
            ps.setString(2, ciphertext);
            ps.executeUpdate();
        }

        // 通过 TypeHandler 读取（模拟 MyBatis 调用 getNullableResult）
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT phone FROM test_str WHERE id = 4")) {
            assertTrue(rs.next());
            String result = handler.getNullableResult(rs, "phone");
            assertEquals(plaintext, result, "TypeHandler 读取应返回明文");
        }
    }

    @Test
    @DisplayName("通过列索引读取也应正常解密")
    void shouldDecryptByColumnIndex() throws Exception {
        String plaintext = "110101198005152536";
        String ciphertext = encryptionUtil.encrypt(plaintext);

        try (PreparedStatement ps = conn.prepareStatement("INSERT INTO test_str (id, phone) VALUES (?, ?)")) {
            ps.setInt(1, 5);
            ps.setString(2, ciphertext);
            ps.executeUpdate();
        }

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id, phone FROM test_str WHERE id = 5")) {
            assertTrue(rs.next());
            String result = handler.getNullableResult(rs, 2); // phone 在第2列
            assertEquals(plaintext, result);
        }
    }

    @Test
    @DisplayName("读取NULL列应返回NULL")
    void shouldReturnNullOnGet() throws Exception {
        try (PreparedStatement ps = conn.prepareStatement("INSERT INTO test_str (id, phone) VALUES (?, ?)")) {
            ps.setInt(1, 6);
            ps.setNull(2, Types.VARCHAR);
            ps.executeUpdate();
        }

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT phone FROM test_str WHERE id = 6")) {
            assertTrue(rs.next());
            String result = handler.getNullableResult(rs, "phone");
            assertNull(result);
        }
    }

    // ═══════════════ 往返一致性 ═══════════════

    @Test
    @DisplayName("写入+读取往返一致")
    void shouldRoundTrip() throws Exception {
        String[] testData = {
                "13800138000",
                "test@hrms.com",
                "6222020200001234567",
                "北京市海淀区中关村大街1号",
                "hello world!@#$%",
        };

        for (int i = 0; i < testData.length; i++) {
            int id = 10 + i;
            String plaintext = testData[i];

            // 写入
            try (PreparedStatement ps = conn.prepareStatement("INSERT INTO test_str (id, phone) VALUES (?, ?)")) {
                ps.setInt(1, id);
                handler.setNonNullParameter(ps, 2, plaintext, null);
                ps.executeUpdate();
            }

            // 读取
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT phone FROM test_str WHERE id = " + id)) {
                assertTrue(rs.next());
                String result = handler.getNullableResult(rs, "phone");
                assertEquals(plaintext, result, "往返不一致: " + plaintext);
            }
        }
    }

    @Test
    @DisplayName("长文本往返一致")
    void shouldRoundTripLongText() throws Exception {
        String longText = "北京市海淀区中关村大街1号院SOHO大厦A座15层1506室".repeat(5);

        try (PreparedStatement ps = conn.prepareStatement("INSERT INTO test_str (id, address) VALUES (?, ?)")) {
            ps.setInt(1, 20);
            handler.setNonNullParameter(ps, 2, longText, null);
            ps.executeUpdate();
        }

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT address FROM test_str WHERE id = 20")) {
            assertTrue(rs.next());
            String result = handler.getNullableResult(rs, "address");
            assertEquals(longText, result);
        }
    }

    // ═══════════════ 随机IV验证 ═══════════════

    @Test
    @DisplayName("两次写入相同明文应产生不同密文（随机IV）")
    void shouldProduceDifferentCiphertext() throws Exception {
        String plaintext = "13800138000";

        // 第一次写入
        try (PreparedStatement ps = conn.prepareStatement("INSERT INTO test_str (id, phone) VALUES (?, ?)")) {
            ps.setInt(1, 30);
            handler.setNonNullParameter(ps, 2, plaintext, null);
            ps.executeUpdate();
        }
        // 第二次写入
        try (PreparedStatement ps = conn.prepareStatement("INSERT INTO test_str (id, phone) VALUES (?, ?)")) {
            ps.setInt(1, 31);
            handler.setNonNullParameter(ps, 2, plaintext, null);
            ps.executeUpdate();
        }

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT phone FROM test_str WHERE id IN (30, 31) ORDER BY id")) {
            assertTrue(rs.next());
            String ct1 = rs.getString("phone");
            assertTrue(rs.next());
            String ct2 = rs.getString("phone");

            assertNotEquals(ct1, ct2, "随机IV应产生不同密文");
            // 但都应解密为原文
            assertEquals(plaintext, encryptionUtil.decrypt(ct1));
            assertEquals(plaintext, encryptionUtil.decrypt(ct2));
        }
    }

    // ═══════════════ 无 EncryptionUtil 回退 ═══════════════

    @Test
    @DisplayName("无EncryptionUtil时明文存入、原样读取")
    void shouldPassPlaintextWhenNoUtil() throws Exception {
        EncryptedStringTypeHandler.setEncryptionUtil(null);
        try {
            // 写入
            try (PreparedStatement ps = conn.prepareStatement("INSERT INTO test_str (id, phone) VALUES (?, ?)")) {
                ps.setInt(1, 40);
                handler.setNonNullParameter(ps, 2, "13800138000", null);
                ps.executeUpdate();
            }

            // 直接查 → 存的是明文
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT phone FROM test_str WHERE id = 40")) {
                assertTrue(rs.next());
                assertEquals("13800138000", rs.getString("phone"));
            }

            // TypeHandler 读取 → 原样返回
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT phone FROM test_str WHERE id = 40")) {
                assertTrue(rs.next());
                String result = handler.getNullableResult(rs, "phone");
                assertEquals("13800138000", result);
            }
        } finally {
            EncryptedStringTypeHandler.setEncryptionUtil(encryptionUtil);
        }
    }
}
