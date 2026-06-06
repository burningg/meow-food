package com.panghu.food.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("circle_plan_recipe")
public class CirclePlanRecipe {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    private String planId;
    private String dishId;
    private String addedByUserId;
    private LocalDateTime createdAt;
}
