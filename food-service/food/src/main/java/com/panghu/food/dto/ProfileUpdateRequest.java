package com.panghu.food.dto;

import lombok.Data;

@Data
public class ProfileUpdateRequest {
    private String nickname;
    private String bio;
    private String avatar;
}
