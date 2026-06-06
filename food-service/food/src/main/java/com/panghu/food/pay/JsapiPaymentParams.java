package com.panghu.food.pay;

import lombok.Data;

@Data
public class JsapiPaymentParams {
    private String timeStamp;
    private String nonceStr;
    private String payPackage;
    private String signType;
    private String paySign;
    private String prepayId;
}
