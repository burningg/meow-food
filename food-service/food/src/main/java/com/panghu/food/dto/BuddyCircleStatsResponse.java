package com.panghu.food.dto;

import lombok.Data;

@Data
public class BuddyCircleStatsResponse {
    private long memberCount;
    private long sharedMenuCount;
    private long weeklyUpdateCount;
}
