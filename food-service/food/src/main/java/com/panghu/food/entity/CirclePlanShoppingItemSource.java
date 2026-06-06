package com.panghu.food.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("circle_plan_shopping_item_source")
public class CirclePlanShoppingItemSource {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    private String shoppingItemId;
    private String dishId;
    private String dishName;
    private String amount;
    private Integer sort;
}
