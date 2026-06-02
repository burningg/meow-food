package com.panghu.food.entity;

import lombok.Data;

@Data
public class WechatAuthVO {
    private String openid;
    private String session_key;
    private String errmsg;
    private String errcode;
}
