package com.hrms.mapper;

import com.hrms.entity.Notification;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 通知 Mapper
 */
@Mapper
public interface NotificationMapper {

    @Insert("INSERT INTO notification (recipient_id, title, content, type, business_type, business_id, is_read, create_time) " +
            "VALUES (#{recipientId}, #{title}, #{content}, #{type}, #{businessType}, #{businessId}, 0, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(Notification notification);

    @Select("SELECT * FROM notification WHERE recipient_id = #{recipientId} ORDER BY create_time DESC")
    List<Notification> selectByRecipient(@Param("recipientId") Long recipientId);

    @Select("SELECT * FROM notification WHERE recipient_id = #{recipientId} AND is_read = 0 ORDER BY create_time DESC")
    List<Notification> selectUnreadByRecipient(@Param("recipientId") Long recipientId);

    @Update("UPDATE notification SET is_read = 1 WHERE id = #{id}")
    int markAsRead(@Param("id") Long id);

    @Select("SELECT COUNT(*) FROM notification WHERE recipient_id = #{recipientId} AND is_read = 0")
    int countUnread(@Param("recipientId") Long recipientId);
}
