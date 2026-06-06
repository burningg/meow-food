package com.panghu.food.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("circle_plan_shopping_item")
public class CirclePlanShoppingItem {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    private String shoppingListId;
    private String ingredientName;
    private Boolean purchased;
    private String purchasedByUserId;
    private LocalDateTime purchasedAt;
    private Integer sort;
}
