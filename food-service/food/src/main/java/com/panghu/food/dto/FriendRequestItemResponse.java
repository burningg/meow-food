package com.panghu.food.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FriendRequestItemResponse {
    private String id;
    private String requesterUserId;
    private String targetUserId;
    private String requesterNickname;
    private String targetNickname;
    private String requesterAvatar;
    private String targetAvatar;
    private String message;
    private String status;
    private LocalDateTime createdAt;
}
