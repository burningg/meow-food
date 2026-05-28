package com.panghu.food.entity;


import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("dish")
public class Dish {
    private String id;

    private Long ownerUserId;

    private String name;

    private String image;

    private BigDecimal price;

    private String description;

    private String categoryId;

    private Integer cookTimeMinutes;

    private String difficulty;

    private Integer servings;

    private String visibility = "inherit";

    private Boolean isFeatured = false;

    private Integer status = 1; // 0-下架，1-上架

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
