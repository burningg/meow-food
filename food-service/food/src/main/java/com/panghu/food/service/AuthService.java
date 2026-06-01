package com.panghu.food.service;

import com.panghu.food.dto.AuthLoginRequest;
import com.panghu.food.dto.AuthLoginResponse;
import com.panghu.food.dto.AuthRegisterRequest;
import com.panghu.food.dto.AuthUserResponse;

public interface AuthService {
    AuthLoginResponse login(AuthLoginRequest request);

    AuthLoginResponse register(AuthRegisterRequest request);

    AuthUserResponse getCurrentUser();
}
