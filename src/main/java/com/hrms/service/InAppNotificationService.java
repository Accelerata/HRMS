package com.hrms.service;

import com.hrms.entity.Notification;
import com.hrms.mapper.NotificationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 站内信通知实现
 *
 * 目前仅写入 notification 表 + 日志，预留 RocketMQ / 邮件扩展
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InAppNotificationService implements NotificationService {

    private final NotificationMapper notificationMapper;

    @Override
    public void send(Long recipientId, String title, String content, int type,
                     Integer businessType, Long businessId) {
        if (recipientId == null) {
            log.warn("通知接收人为空，跳过: title={}", title);
            return;
        }

        Notification notification = new Notification();
        notification.setRecipientId(recipientId);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setType(type);
        notification.setBusinessType(businessType);
        notification.setBusinessId(businessId);
        notification.setIsRead(0);

        notificationMapper.insert(notification);

        log.info("通知已发送: recipientId={}, type={}, title={}, businessType={}, businessId={}",
                recipientId, type, title, businessType, businessId);
    }
}
