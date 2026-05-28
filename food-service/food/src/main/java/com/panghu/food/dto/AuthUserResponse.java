package com.panghu.food.dto;

import lombok.Data;

@Data
public class AuthUserResponse {
    private Long id;
    private String account;
    private String nickname;
    private String avatar;
    private String bio;
    private String defaultMenuVisibility;
}
