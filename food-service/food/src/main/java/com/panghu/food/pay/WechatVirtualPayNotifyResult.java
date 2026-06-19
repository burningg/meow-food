package com.panghu.food.pay;

import lombok.Data;

@Data
public class WechatVirtualPayNotifyResult {
    private String event;
    private String openId;
    private String outTradeNo;
    private Integer env;
    private String payChannel;
    private String transactionId;
    private String wechatPayMchOrderNo;
    private Integer quantity;
    private Integer origPrice;
    private Integer actualPrice;
    private String attach;
    private String productId;
}
