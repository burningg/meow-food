package com.panghu.food.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
public class PlanDetailResponse {
    private String id;
    private String title;
    private LocalDate planDate;
    private String circleId;
    private String circleName;
    private String creatorUserId;
    private String creatorNickname;
    private String shareToken;
    private String shoppingStatus;
    private boolean shoppingStarted;
    private int shoppingRestartCount;
    private int shoppingTotalItemCount;
    private int shoppingPurchasedItemCount;
    private boolean sharedView;
    private boolean viewerCanDelete;
    private boolean viewerCanAddRecipes;
    private boolean viewerCanManageRecipes;
    private boolean viewerCanUseShopping;
    private List<DishSummaryResponse> recipes = new ArrayList<>();
}
