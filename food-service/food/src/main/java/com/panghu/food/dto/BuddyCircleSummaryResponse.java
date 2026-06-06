package com.panghu.food.dto;

import lombok.Data;

@Data
public class BuddyCircleSummaryResponse {
    private String id;
    private String name;
    private String description;
    private String ownerUserId;
    private String ownerNickname;
    private long memberCount;
    private long sharedMenuCount;
}
