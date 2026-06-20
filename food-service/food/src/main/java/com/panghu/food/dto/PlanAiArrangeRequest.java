package com.panghu.food.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class PlanAiArrangeRequest {
    private String circleId;
    private String mealType;
    private LocalDate planDate;
    private Integer dishCount;
    private String healthAdvice;
}
