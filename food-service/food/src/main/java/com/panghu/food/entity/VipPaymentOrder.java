package com.panghu.food.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("vip_payment_order")
public class VipPaymentOrder {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    private String userId;
    private String outTradeNo;
    private String transactionId;
    private String planCode;
    private Integer amountFen;
    private String status;
    private String prepayId;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
