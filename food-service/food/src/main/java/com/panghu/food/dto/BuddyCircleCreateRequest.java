package com.panghu.food.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class BuddyCircleCreateRequest {
    private String name;
    private String description;
    private List<Long> initialMemberIds = new ArrayList<>();
}
