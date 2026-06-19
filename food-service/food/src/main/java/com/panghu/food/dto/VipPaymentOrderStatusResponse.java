package com.panghu.food.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class VipPaymentOrderStatusResponse {
    private String outTradeNo;
    private Integer amountFen;
    private String planName;
    private String productId;
    private String status;
    private LocalDateTime paidAt;
    private LocalDateTime providedAt;
    private VipInfoResponse vipInfo;
}
