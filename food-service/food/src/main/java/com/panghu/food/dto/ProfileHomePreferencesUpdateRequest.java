package com.panghu.food.dto;

import lombok.Data;

@Data
public class ProfileHomePreferencesUpdateRequest {
    private Boolean showKnowledgeOnHome;
    private Boolean showPetOnHome;
}
