package com.panghu.food.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
public class PlanDayPlansResponse {
    private LocalDate date;
    private List<PlanSummaryResponse> plans = new ArrayList<>();
}
