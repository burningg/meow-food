package com.panghu.food.dto;

import lombok.Data;

@Data
public class BuddyCircleInviteRequest {
    private Long inviteeUserId;
    private String inviteeAccount;
}
