package com.hrms.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PasswordUtil 单元测试
 * TDD RED 阶段 —— 先写测试，确保测试因功能缺失而失败
 */
@DisplayName("PasswordUtil 密码工具类")
class PasswordUtilTest {

    // ═══════════════ 密码强度校验 ═══════════════

    @Nested
    @DisplayName("密码强度校验 - 合法密码应通过")
    class ValidPasswords {

        @Test
        @DisplayName("8位含大小写+数字")
        void shouldAcceptValidPassword8Chars() {
            assertTrue(PasswordUtil.isStrong("Abcdefg1"));
        }

        @Test
        @DisplayName("超过8位含大小写+数字")
        void shouldAcceptLongerPassword() {
            assertTrue(PasswordUtil.isStrong("MyPass12345"));
        }

        @Test
        @DisplayName("含特殊字符的合法密码")
        void shouldAcceptPasswordWithSpecialChars() {
            assertTrue(PasswordUtil.isStrong("P@ssw0rd!"));
        }

        @Test
        @DisplayName("20位强密码")
        void shouldAcceptVeryLongStrongPassword() {
            assertTrue(PasswordUtil.isStrong("ThisIsAVeryLongP@ss1"));
        }
    }

    @Nested
    @DisplayName("密码强度校验 - 非法密码应拒绝")
    class InvalidPasswords {

        @ParameterizedTest
        @ValueSource(strings = {
                "Abcd1",         // 7位，太短
                "abc1",          // 4位，太短
                "A1b"            // 3位，太短
        })
        @DisplayName("长度不足8位")
        void shouldRejectTooShortPassword(String password) {
            assertFalse(PasswordUtil.isStrong(password),
                    "长度不足8位的密码应被拒绝: " + password);
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "abcdefg1",      // 没有大写字母
                "lowercase1",    // 没有大写字母
                "alllower123"    // 没有大写字母
        })
        @DisplayName("缺少大写字母")
        void shouldRejectPasswordWithoutUppercase(String password) {
            assertFalse(PasswordUtil.isStrong(password),
                    "缺少大写字母的密码应被拒绝: " + password);
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "ABCDEFG1",      // 没有小写字母
                "UPPERCASE1",    // 没有小写字母
                "ALLUPPER123"    // 没有小写字母
        })
        @DisplayName("缺少小写字母")
        void shouldRejectPasswordWithoutLowercase(String password) {
            assertFalse(PasswordUtil.isStrong(password),
                    "缺少小写字母的密码应被拒绝: " + password);
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "Abcdefgh",      // 没有数字
                "PasswordOnly",  // 没有数字
                "NoDigitsHere"   // 没有数字
        })
        @DisplayName("缺少数字")
        void shouldRejectPasswordWithoutDigit(String password) {
            assertFalse(PasswordUtil.isStrong(password),
                    "缺少数字的密码应被拒绝: " + password);
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "ABCDEFGH",      // 只有大写字母
                "abcdefgh",      // 只有小写字母
                "12345678"       // 只有数字
        })
        @DisplayName("单一字符类型")
        void shouldRejectSingleCharacterType(String password) {
            assertFalse(PasswordUtil.isStrong(password),
                    "只有单一字符类型的密码应被拒绝: " + password);
        }
    }

    @Nested
    @DisplayName("密码强度校验 - 边界情况")
    class EdgeCases {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" ", "  ", "\t", "\n"})
        @DisplayName("null/空/空白字符")
        void shouldRejectNullOrBlank(String password) {
            assertFalse(PasswordUtil.isStrong(password),
                    "null/空/空白密码应被拒绝");
        }

        @Test
        @DisplayName("恰好8位合法密码")
        void shouldAcceptExactly8Chars() {
            assertTrue(PasswordUtil.isStrong("Pass1234"));
        }
    }

    // ═══════════════ BCrypt 加密与验证 ═══════════════

    @Nested
    @DisplayName("BCrypt 加密与验证")
    class BCryptTests {

        @Test
        @DisplayName("加密后密码不等于原文")
        void encodeShouldNotReturnPlaintext() {
            String rawPassword = "MySecurePass1";
            String encoded = PasswordUtil.encode(rawPassword);

            assertNotNull(encoded);
            assertNotEquals(rawPassword, encoded);
        }

        @Test
        @DisplayName("加密后密码以 BCrypt $2a$ 前缀开头")
        void encodeShouldUseBCryptPrefix() {
            String encoded = PasswordUtil.encode("TestPass1");

            assertTrue(encoded.startsWith("$2a$") || encoded.startsWith("$2b$") || encoded.startsWith("$2y$"),
                    "BCrypt hash 应以 $2a$/$2b$/$2y$ 开头, 实际: " + encoded.substring(0, 4));
        }

        @Test
        @DisplayName("相同密码加密两次得到不同密文（盐值随机）")
        void encodeShouldGenerateDifferentHashesForSamePassword() {
            String raw = "SamePass1";
            String hash1 = PasswordUtil.encode(raw);
            String hash2 = PasswordUtil.encode(raw);

            assertNotEquals(hash1, hash2, "两次加密应产生不同密文（随机盐）");
        }

        @Test
        @DisplayName("正确密码匹配成功")
        void shouldMatchCorrectPassword() {
            String rawPassword = "CorrectPass1";
            String encoded = PasswordUtil.encode(rawPassword);

            assertTrue(PasswordUtil.matches(rawPassword, encoded),
                    "正确密码应匹配成功");
        }

        @Test
        @DisplayName("错误密码匹配失败")
        void shouldNotMatchWrongPassword() {
            String encoded = PasswordUtil.encode("RightPass1");

            assertFalse(PasswordUtil.matches("WrongPass1", encoded),
                    "错误密码应匹配失败");
        }

        @Test
        @DisplayName("大小写敏感 - 大小写不同应匹配失败")
        void shouldBeCaseSensitive() {
            String encoded = PasswordUtil.encode("CaseTest1");

            assertFalse(PasswordUtil.matches("casetest1", encoded),
                    "大小写不同应匹配失败");
        }
    }
}
