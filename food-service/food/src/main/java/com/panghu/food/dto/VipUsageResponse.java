package com.panghu.food.dto;

import lombok.Data;

@Data
public class VipUsageResponse {
    private Integer dailyLimit;
    private Integer usedToday;
    private Integer remainingToday;
}
