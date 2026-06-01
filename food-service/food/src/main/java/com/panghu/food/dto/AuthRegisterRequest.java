package com.panghu.food.dto;

import lombok.Data;

@Data
public class AuthRegisterRequest {
    private String account;
    private String password;
}
