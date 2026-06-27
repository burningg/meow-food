package com.panghu.food.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class PlanVisibleUsersUpdateRequest {
    private List<String> visibleUserIds = new ArrayList<>();
}
