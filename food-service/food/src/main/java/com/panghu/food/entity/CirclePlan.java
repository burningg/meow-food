package com.panghu.food.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("circle_plan")
public class CirclePlan {
    public static final String SHOPPING_STATUS_NOT_STARTED = "NOT_STARTED";
    public static final String SHOPPING_STATUS_NOT_PURCHASED = "NOT_PURCHASED";
    public static final String SHOPPING_STATUS_PARTIALLY_PURCHASED = "PARTIALLY_PURCHASED";
    public static final String SHOPPING_STATUS_PURCHASED = "PURCHASED";

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    private String circleId;
    private LocalDate planDate;
    private String title;
    private String creatorUserId;
    private String shoppingStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
