package com.panghu.food.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("activity_feed")
public class ActivityFeed {
    private Long id;
    private Long actorUserId;
    private String dishId;
    private Long circleId;
    private String activityType;
    private String visibilityScope;
    private LocalDateTime createdAt;
}
