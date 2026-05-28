package com.panghu.food.dto;

import lombok.Data;

@Data
public class FriendRequestActionRequest {
    private Long targetUserId;
    private String targetAccount;
    private String message;
}
