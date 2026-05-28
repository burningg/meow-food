package com.panghu.food.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FeedItemResponse {
    private Long id;
    private Long actorUserId;
    private String actorNickname;
    private String actorAvatar;
    private String activityType;
    private String actionText;
    private String visibilityScope;
    private Long circleId;
    private String circleName;
    private String dishId;
    private String dishName;
    private String dishImage;
    private String dishDescription;
    private LocalDateTime createdAt;
}
