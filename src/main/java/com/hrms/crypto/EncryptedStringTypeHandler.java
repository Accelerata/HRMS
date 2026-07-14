package com.hrms.crypto;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * String 类型加密 TypeHandler
 * 写入时自动加密，读取时自动解密，对业务代码透明
 */
@MappedTypes(String.class)
public class EncryptedStringTypeHandler extends BaseTypeHandler<String> {

    /**
     * MyBatis 通过无参构造创建，需从 Spring 容器获取 EncryptionUtil
     */
    private static volatile EncryptionUtil encryptionUtil;

    public EncryptedStringTypeHandler() {
        super();
    }

    /**
     * Spring 集成时通过此方法注入 EncryptionUtil
     */
    public static void setEncryptionUtil(EncryptionUtil util) {
        encryptionUtil = util;
    }

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, String parameter, JdbcType jdbcType)
            throws SQLException {
        if (encryptionUtil != null) {
            ps.setString(i, encryptionUtil.encrypt(parameter));
        } else {
            ps.setString(i, parameter);
        }
    }

    @Override
    public String getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String value = rs.getString(columnName);
        return decryptIfNeeded(value);
    }

    @Override
    public String getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String value = rs.getString(columnIndex);
        return decryptIfNeeded(value);
    }

    @Override
    public String getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String value = cs.getString(columnIndex);
        return decryptIfNeeded(value);
    }

    private String decryptIfNeeded(String value) {
        if (value == null || encryptionUtil == null) return value;
        return encryptionUtil.decrypt(value);
    }
}
