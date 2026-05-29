package com.panghu.food.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("activity_feed")
public class ActivityFeed {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    private String actorUserId;
    private String dishId;
    private String circleId;
    private String activityType;
    private String visibilityScope;
    private LocalDateTime createdAt;
}
