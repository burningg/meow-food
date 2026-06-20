package com.panghu.food.dto;

import lombok.Data;

@Data
public class PlanAiUsageResponse {
    private Integer monthlyLimit;
    private Integer usedThisMonth;
    private Integer remainingThisMonth;
}
