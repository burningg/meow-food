package com.panghu.food.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class PlanShoppingListResponse {
    private String id;
    private String planId;
    private String planTitle;
    private LocalDate planDate;
    private String circleId;
    private String circleName;
    private boolean shoppingStarted;
    private String startedByUserId;
    private String startedByNickname;
    private LocalDateTime startedAt;
    private int restartCount;
    private int totalItemCount;
    private int purchasedItemCount;
    private List<PlanShoppingItemResponse> items = new ArrayList<>();
}
