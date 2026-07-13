package com.hrms.utils;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 密码工具类
 * - 密码强度校验：8位以上，必须包含大写字母、小写字母、数字
 * - BCrypt 加密与验证
 */
public final class PasswordUtil {

    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    /** 密码强度正则：至少8位，至少1个大写字母、1个小写字母、1个数字 */
    private static final String STRONG_PASSWORD_REGEX =
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$";

    private PasswordUtil() {
        // 工具类禁止实例化
    }

    /**
     * 校验密码强度
     * @param password 明文密码
     * @return true=符合强度要求, false=不符合
     */
    public static boolean isStrong(String password) {
        if (password == null || password.isBlank()) {
            return false;
        }
        return password.matches(STRONG_PASSWORD_REGEX);
    }

    /**
     * BCrypt 加密
     * @param rawPassword 明文密码
     * @return 加密后的密文
     */
    public static String encode(String rawPassword) {
        return ENCODER.encode(rawPassword);
    }

    /**
     * BCrypt 验证
     * @param rawPassword     明文密码
     * @param encodedPassword 加密后的密文
     * @return true=匹配, false=不匹配
     */
    public static boolean matches(String rawPassword, String encodedPassword) {
        return ENCODER.matches(rawPassword, encodedPassword);
    }
}
