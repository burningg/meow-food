package com.panghu.food.service;

import com.panghu.food.dto.NotificationBootstrapResponse;
import com.panghu.food.dto.NotificationListResponse;

public interface NotificationService {
    NotificationBootstrapResponse getBootstrap();

    NotificationListResponse getNotifications();

    void markRead(String notificationId);
}
