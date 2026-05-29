package com.panghu.food.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("dish_ingredient")
public class DishIngredient {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    private String dishId;

    private String name;

    private String amount;

    private Integer sort;
}
