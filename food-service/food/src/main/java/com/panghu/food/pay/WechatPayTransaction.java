package com.panghu.food.pay;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class WechatPayTransaction {
    private String outTradeNo;
    private String transactionId;
    private String tradeState;
    private String mchid;
    private String openid;
    private Integer amountFen;
    private LocalDateTime successTime;
}
