package com.panghu.food.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class PlanMonthResponse {
    private String month;
    private List<PlanDayPlansResponse> days = new ArrayList<>();
}
