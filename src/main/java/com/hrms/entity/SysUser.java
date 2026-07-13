package com.hrms.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统用户表
 */
@Data
public class SysUser {

    /** 主键ID */
    private Long id;

    /** 登录账号(默认手机号) */
    private String username;

    /** 加密后的密码(要求8位以上，大小写+数字) */
    private String password;

    /** 账号状态：0-禁用，1-正常 */
    private Integer status;

    /** 密码最后修改时间(用于90天强制更换校验) */
    private LocalDateTime pwdUpdateTime;

    /** 最后登录时间 */
    private LocalDateTime lastLoginTime;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
