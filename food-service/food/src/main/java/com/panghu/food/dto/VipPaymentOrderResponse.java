package com.panghu.food.dto;

import lombok.Data;

@Data
public class VipPaymentOrderResponse {
    private String outTradeNo;
    private Integer amountFen;
    private String planName;
    private String offerId;
    private String productId;
    private Integer buyQuantity;
    private Integer env;
    private String currencyType;
    private Integer goodsPrice;
    private String attach;
    private String signData;
    private String paySig;
    private String signature;
    private String mode;
    private String successTip;
}
