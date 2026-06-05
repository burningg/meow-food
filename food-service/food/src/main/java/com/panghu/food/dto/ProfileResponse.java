package com.panghu.food.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ProfileResponse {
    private AuthUserResponse user;
    private ProfileStatsResponse stats;
    private List<FriendItemResponse> friendPreview = new ArrayList<>();
    private String defaultMenuVisibility;
    private List<String> defaultMenuCircleIds = new ArrayList<>();
    private String lastSelectedCircleId;
    private VipInfoResponse vipInfo;
}
