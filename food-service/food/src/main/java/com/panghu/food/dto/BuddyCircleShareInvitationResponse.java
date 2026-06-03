package com.panghu.food.dto;

import lombok.Data;

@Data
public class BuddyCircleShareInvitationResponse {
    private AuthUserResponse inviter;
    private BuddyCircleSummaryResponse circle;
    private boolean friend;
    private boolean member;
    private String status;
}
