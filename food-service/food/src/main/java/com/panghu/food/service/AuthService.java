package com.panghu.food.service;

import com.panghu.food.dto.AuthLoginRequest;
import com.panghu.food.dto.AuthLoginResponse;
import com.panghu.food.dto.AuthRegisterRequest;
import com.panghu.food.dto.AuthUserResponse;
import com.panghu.food.dto.WechatLoginRequest;

public interface AuthService {
    AuthLoginResponse login(AuthLoginRequest request);

    AuthLoginResponse register(AuthRegisterRequest request);

    AuthLoginResponse wechatLogin(WechatLoginRequest request);

    AuthUserResponse getCurrentUser();
}
