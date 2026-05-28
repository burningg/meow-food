package com.panghu.food.dto;

import lombok.Data;

@Data
public class AuthLoginRequest {
    private String account;
    private String password;
}
