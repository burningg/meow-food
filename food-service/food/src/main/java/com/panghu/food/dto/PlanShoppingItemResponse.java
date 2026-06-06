package com.panghu.food.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class PlanShoppingItemResponse {
    private String id;
    private String ingredientName;
    private boolean purchased;
    private String purchasedByUserId;
    private String purchasedByNickname;
    private LocalDateTime purchasedAt;
    private List<PlanShoppingItemSourceResponse> sources = new ArrayList<>();
}
