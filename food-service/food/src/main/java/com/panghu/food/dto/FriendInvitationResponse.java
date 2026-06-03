package com.panghu.food.dto;

import lombok.Data;

@Data
public class FriendInvitationResponse {
    private AuthUserResponse inviter;
    private String status;
    private FriendRequestItemResponse request;
}
