package com.panghu.food.dto;

import lombok.Data;

@Data
public class FriendRequestActionRequest {
    private String targetUserId;
    private String targetAccount;
    private String message;
}
