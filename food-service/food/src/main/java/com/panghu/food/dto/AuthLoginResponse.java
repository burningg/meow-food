package com.panghu.food.dto;

import lombok.Data;

@Data
public class AuthLoginResponse {
    private String token;
    private AuthUserResponse user;
}
