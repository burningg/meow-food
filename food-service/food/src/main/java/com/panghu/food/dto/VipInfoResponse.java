package com.panghu.food.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class VipInfoResponse {
    private Boolean vip;
    private String vipLevel;
    private LocalDateTime openedAt;
    private LocalDateTime expiresAt;
    private BigDecimal openAmount;
    private Integer dailyRecipeAnalysisLimit;
    private Integer dailyRecipeAnalysisUsed;
    private Integer dailyRecipeAnalysisRemaining;
    private Integer monthlyPlanAiLimit;
    private Integer monthlyPlanAiUsed;
    private Integer monthlyPlanAiRemaining;
}
