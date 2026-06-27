package com.panghu.food.dto;

import lombok.Data;

@Data
public class CookingTimerCreateRequest {
    private String dishName;
    private String stepText;
    private Integer seconds;
    private String page;
}
