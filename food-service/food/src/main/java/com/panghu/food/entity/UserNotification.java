package com.panghu.food.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_notification")
public class UserNotification {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    private String userId;
    private String title;
    private String summary;
    private String body;
    private String audienceType;
    private String priority;
    private String recipientScope;
    private LocalDateTime recipientCutoffAt;
    private LocalDateTime publishedAt;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
