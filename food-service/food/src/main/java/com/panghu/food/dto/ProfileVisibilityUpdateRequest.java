package com.panghu.food.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ProfileVisibilityUpdateRequest {
    private String defaultMenuVisibility;
    private List<String> defaultMenuCircleIds = new ArrayList<>();
}
