package com.panghu.food.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class PlanRecipesUpdateRequest {
    private List<String> dishIds = new ArrayList<>();
}
