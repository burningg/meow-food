package com.panghu.food.dto;

import lombok.Data;

@Data
public class NotificationBootstrapResponse {
    private Boolean hasUnread;
    private NotificationItemResponse importantNotification;
}
