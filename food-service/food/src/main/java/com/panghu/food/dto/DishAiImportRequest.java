package com.panghu.food.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class DishAiImportRequest {
    private String text;
    private List<String> images = new ArrayList<>();
}
