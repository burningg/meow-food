package com.panghu.food.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class PlanSummaryResponse {
    private String id;
    private String title;
    private LocalDate planDate;
    private String circleId;
    private String circleName;
    private String creatorUserId;
    private String creatorNickname;
    private int recipeCount;
    private boolean shoppingStarted;
    private int shoppingTotalItemCount;
    private int shoppingPurchasedItemCount;
}
