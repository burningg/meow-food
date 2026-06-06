package com.panghu.food.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class NotificationListResponse {
    private Boolean hasUnread;
    private List<NotificationItemResponse> items = new ArrayList<>();
}
