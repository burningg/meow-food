package com.panghu.food.dto;

import lombok.Data;

@Data
public class BuddyCircleMemberResponse {
    private String id;
    private String account;
    private String nickname;
    private String avatar;
    private String role;
    private boolean vip;
    private long sharedMenuCount;
}
