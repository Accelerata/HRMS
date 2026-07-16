package com.hrms.service;

import com.hrms.entity.SysUser;
import com.hrms.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 安全合规定时任务调度器
 *
 * 职责：
 * 1. 密码到期提醒（每天 09:00 扫描，到期前 7/3/1 天通知）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SecurityScheduler {

    private final SysUserMapper sysUserMapper;
    private final NotificationService notificationService;

    /**
     * 密码到期提醒 — 每天 09:00
     * 扫描密码将在 7/3/1 天后到期的用户，发送系统通知提醒
     */
    @Scheduled(cron = "0 0 9 * * ?")
    public void scanPasswordExpiryReminders() {
        log.info("定时任务: 密码到期提醒扫描开始");
        LocalDateTime now = LocalDateTime.now();
        int[] reminderDays = {7, 3, 1};

        for (int days : reminderDays) {
            LocalDateTime targetDate = now.minusDays(90 - days);
            LocalDateTime targetEnd = targetDate.plusDays(1);
            List<SysUser> users = sysUserMapper.findByPwdUpdateTimeBetween(
                    targetDate.minusHours(1), targetEnd);
            for (SysUser user : users) {
                try {
                    String title = "密码到期提醒";
                    String content = String.format("您的密码将于 %d 天后过期，请及时修改密码。", days);
                    notificationService.send(user.getId(), title, content, 2, null, null);
                    log.info("密码到期提醒已发送: userId={}, days={}", user.getId(), days);
                } catch (Exception e) {
                    log.error("密码到期提醒发送失败: userId={}", user.getId(), e);
                }
            }
        }
        log.info("定时任务: 密码到期提醒扫描完成");
    }
}
