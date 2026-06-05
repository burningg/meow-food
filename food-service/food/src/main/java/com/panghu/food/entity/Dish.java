package com.panghu.food.entity;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("dish")
public class Dish {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    private String ownerUserId;

    private String name;

    private String image;

    private BigDecimal price;

    private String description;

    private String categoryId;

    private Integer cookTimeMinutes;

    private String difficulty;

    private Integer servings;

    private String visibility = "public";

    private Boolean isFeatured = false;

    private Integer status = 1; // 0-下架，1-上架

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
