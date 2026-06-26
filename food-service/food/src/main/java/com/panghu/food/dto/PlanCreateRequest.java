package com.panghu.food.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
public class PlanCreateRequest {
    private String circleId;
    private LocalDate planDate;
    private String title;
    private List<String> visibleUserIds = new ArrayList<>();
}
