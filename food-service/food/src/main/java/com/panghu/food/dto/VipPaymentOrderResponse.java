package com.panghu.food.dto;

import lombok.Data;

@Data
public class VipPaymentOrderResponse {
    private String outTradeNo;
    private Integer amountFen;
    private String planName;
    private String timeStamp;
    private String nonceStr;
    private String payPackage;
    private String signType;
    private String paySign;
}
