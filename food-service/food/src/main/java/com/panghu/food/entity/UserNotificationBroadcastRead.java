package com.panghu.food.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_notification_broadcast_read")
public class UserNotificationBroadcastRead {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    private String notificationId;
    private String userId;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;
}
