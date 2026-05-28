package com.panghu.food.dto;

import lombok.Data;

@Data
public class BuddyCircleSummaryResponse {
    private Long id;
    private String name;
    private String description;
    private Long ownerUserId;
    private String ownerNickname;
    private long memberCount;
    private long sharedMenuCount;
    private long weeklyUpdateCount;
}
