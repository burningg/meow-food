package com.panghu.food.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PetResponse {
    private boolean claimed;
    private String id;
    private String petType;
    private String petTypeName;
    private String name;
    private Integer experience;
    private Integer companionDays;
    private String moodCode;
    private String moodLabel;
    private Integer fullnessPercent;
    private String todayStory;
    private LocalDateTime claimedAt;
}
