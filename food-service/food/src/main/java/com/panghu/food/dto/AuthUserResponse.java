package com.panghu.food.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AuthUserResponse {
    private String id;
    private String account;
    private String nickname;
    private String avatar;
    private String bio;
    private String defaultMenuVisibility;
    private List<String> defaultMenuCircleIds = new ArrayList<>();
    private Boolean vip;
    private String vipLevel;
}
