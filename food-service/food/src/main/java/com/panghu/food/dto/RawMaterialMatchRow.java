package com.panghu.food.dto;

import lombok.Data;

@Data
public class RawMaterialMatchRow {
    private String queryName;
    private String name;
    private String commonNames;
    private String steamTime;
    private String boilTime;
    private String fryTime;
    private String bakeTime;
    private String stirFryTime;
    private String defaultHeatTemperature;
    private String allergenFlag;
    private String calorieEstimate;
    private String nutritionInfo;
    private String substituteIngredients;
    private String category;
}
