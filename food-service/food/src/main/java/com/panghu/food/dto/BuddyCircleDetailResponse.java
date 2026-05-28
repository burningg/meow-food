package com.panghu.food.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class BuddyCircleDetailResponse {
    private BuddyCircleSummaryResponse circle;
    private BuddyCircleStatsResponse stats;
    private List<BuddyCircleMemberResponse> members = new ArrayList<>();
    private List<DishSummaryResponse> sharedMenus = new ArrayList<>();
}
