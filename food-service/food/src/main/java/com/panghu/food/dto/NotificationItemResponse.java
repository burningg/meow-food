package com.panghu.food.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NotificationItemResponse {
    private String id;
    private String title;
    private String summary;
    private String body;
    private String audienceType;
    private String priority;
    private Boolean read;
    private LocalDateTime publishedAt;
}
