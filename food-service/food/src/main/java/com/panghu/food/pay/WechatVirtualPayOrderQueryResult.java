package com.panghu.food.pay;

import lombok.Data;

@Data
public class WechatVirtualPayOrderQueryResult {
    private String orderId;
    private String wechatOrderId;
    private String channelOrderId;
    private String wxpayOrderId;
    private Integer status;
    private Integer paidFee;
    private Long paidTime;
    private Long provideTime;
    private Integer orderType;
}
