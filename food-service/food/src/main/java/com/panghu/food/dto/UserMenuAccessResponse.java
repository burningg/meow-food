package com.panghu.food.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class UserMenuAccessResponse {
    private AuthUserResponse user;
    private boolean friend;
    private boolean sameCircle;
    private String actionType;
    private String description;
    private long accessibleCount;
    private long privateCount;
    private List<AccessRuleResponse> accessRules = new ArrayList<>();
    private List<DishSummaryResponse> menus = new ArrayList<>();
}
