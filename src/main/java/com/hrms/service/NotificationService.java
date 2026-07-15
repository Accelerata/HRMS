package com.hrms.service;

/**
 * 通知服务接口
 *
 * 职责：发送各类业务通知（审批待办、结果通知、提醒、警告）
 * 预留 RocketMQ / 邮件扩展点
 */
public interface NotificationService {

    /**
     * 发送通知
     *
     * @param recipientId  接收人ID (sys_user.id)
     * @param title        通知标题
     * @param content      通知内容
     * @param type         通知类型: 1-系统 2-审批 3-提醒 4-警告
     * @param businessType 关联业务类型（可为null）
     * @param businessId   关联业务单据ID（可为null）
     */
    void send(Long recipientId, String title, String content, int type,
              Integer businessType, Long businessId);

    /**
     * 发送审批通知（type=2）
     */
    default void sendApprovalNotify(Long recipientId, String title, String content,
                                    Integer businessType, Long businessId) {
        send(recipientId, title, content, 2, businessType, businessId);
    }

    /**
     * 发送提醒（type=3）
     */
    default void sendReminder(Long recipientId, String title, String content,
                              Integer businessType, Long businessId) {
        send(recipientId, title, content, 3, businessType, businessId);
    }

    /**
     * 发送警告（type=4）
     */
    default void sendWarning(Long recipientId, String title, String content,
                             Integer businessType, Long businessId) {
        send(recipientId, title, content, 4, businessType, businessId);
    }
}
