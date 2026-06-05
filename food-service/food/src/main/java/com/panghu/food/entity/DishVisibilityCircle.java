package com.panghu.food.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("dish_visibility_circle")
public class DishVisibilityCircle {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    private String dishId;
    private String circleId;
    private LocalDateTime createdAt;
}
