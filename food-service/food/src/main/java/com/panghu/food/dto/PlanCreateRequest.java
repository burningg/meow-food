package com.panghu.food.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class PlanCreateRequest {
    private String circleId;
    private LocalDate planDate;
    private String title;
}
