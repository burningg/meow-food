package com.panghu.food.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class FeedAccessibleMenusResponse {
    private List<DishSummaryResponse> menus = new ArrayList<>();
}
