package com.panghu.food.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("circle_plan_shopping_list")
public class CirclePlanShoppingList {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    private String planId;
    private String startedByUserId;
    private LocalDateTime startedAt;
    private Integer restartCount;
    private LocalDateTime updatedAt;
}
