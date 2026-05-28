package com.panghu.food.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FriendRequestItemResponse {
    private Long id;
    private Long requesterUserId;
    private Long targetUserId;
    private String requesterNickname;
    private String targetNickname;
    private String requesterAvatar;
    private String targetAvatar;
    private String message;
    private String status;
    private LocalDateTime createdAt;
}
