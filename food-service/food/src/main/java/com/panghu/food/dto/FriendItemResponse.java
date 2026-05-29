package com.panghu.food.dto;

import lombok.Data;

@Data
public class FriendItemResponse {
    private String id;
    private String account;
    private String nickname;
    private String avatar;
    private String bio;
    private boolean friend;
    private long visibleMenuCount;
    private long sharedMenuCount;
    private boolean memberInCircle;
}
